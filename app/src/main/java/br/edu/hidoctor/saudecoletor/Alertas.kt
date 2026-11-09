package br.edu.hidoctor.saudecoletor

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

/** Detecta 3 toques seguidos dentro de uma janela de tempo. */
class DetectorTriploVolume(
    private val janelaMs: Long = 1500,
    private val aoDetectar: () -> Unit
) {
    private val toques = ArrayDeque<Long>()

    fun registrar() {
        val agora = SystemClock.elapsedRealtime()
        toques.addLast(agora)
        while (toques.isNotEmpty() && agora - toques.first() > janelaMs) toques.removeFirst()
        if (toques.size >= 3) {
            toques.clear()
            aoDetectar()
        }
    }
}

/**
 * Alerta em dois envios com o MESMO id:
 *  1) na hora, com a última localização conhecida;
 *  2) assim que a localização precisa estiver disponível (ou após o prazo), atualizando o mesmo registro.
 * O alerta fica salvo em arquivo e é reenviado até o servidor confirmar.
 * O horário (dataHora) só é gravado depois que o 1º envio é confirmado e vai no 2º envio.
 */
object Alertas {
    private const val PRAZO_PRECISA_MS = 60_000L   // espera máxima pela posição precisa
    private const val ESPERA_FALHA_MS = 5_000L     // nova tentativa após falha
    private const val ESPERA_AGUARDANDO_MS = 500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ultimoToque = 0L
    private var bombaAtiva = false

    private enum class Resultado { OK, AGUARDANDO, FALHOU }

    @Synchronized
    private fun podeEnviar(): Boolean {
        val agora = SystemClock.elapsedRealtime()
        if (agora - ultimoToque < 5000) return false // evita alerta duplicado
        ultimoToque = agora
        return true
    }

    /** Devolve false se foi ignorado (outro alerta há menos de 5 s). */
    fun disparar(context: Context, origem: String): Boolean {
        if (!podeEnviar()) return false
        val app = context.applicationContext
        Fila.iniciar(app)

        // Localização imediata: a última conhecida, sem esperar
        val ultima = Localizacao.ultimaConhecida(app)
        val alerta = AlertaPendente(
            id = UUID.randomUUID().toString(),
            usuario = Config.usuario(app) ?: "semUsuario",
            tipo = "volume_x3",
            origem = origem,
            latitude = ultima?.latitude,
            longitude = ultima?.longitude,
            localizacaoEm = ultima?.let { Instant.ofEpochMilli(it.time).toString() },
            prazoPrecisaMs = System.currentTimeMillis() + PRAZO_PRECISA_MS
        )
        Fila.adicionarAlerta(alerta)
        Estado.statusAlerta.value = "Enviando alerta…"
        iniciarBomba(app)

        // Em paralelo, busca a localização precisa para o 2º envio
        scope.launch {
            val precisa = Localizacao.precisa(app)
            Fila.atualizarAlerta(alerta.id) { a ->
                if (precisa != null) {
                    a.latitude = precisa.latitude
                    a.longitude = precisa.longitude
                    a.localizacaoEm = Instant.ofEpochMilli(precisa.time).toString()
                    a.precisaObtida = true
                }
                a.precisaPronta = true
            }
        }
        return true
    }

    /** Retoma alertas salvos (por exemplo, depois de reabrir o app). */
    fun retomar(context: Context) {
        val app = context.applicationContext
        Fila.iniciar(app)
        if (Fila.alertas().isNotEmpty()) iniciarBomba(app)
    }

    private fun iniciarBomba(app: Context) {
        synchronized(this) {
            if (bombaAtiva) return
            bombaAtiva = true
        }
        scope.launch {
            try {
                while (true) {
                    val pendentes = Fila.alertas()
                    if (pendentes.isEmpty()) break
                    var falhou = false
                    for (a in pendentes) {
                        if (processar(app, a) == Resultado.FALHOU) falhou = true
                    }
                    delay(if (falhou) ESPERA_FALHA_MS else ESPERA_AGUARDANDO_MS)
                }
            } finally {
                synchronized(this@Alertas) { bombaAtiva = false }
            }
            // Um alerta pode ter chegado enquanto o laço terminava
            if (Fila.alertas().isNotEmpty()) iniciarBomba(app)
        }
    }

    private suspend fun processar(app: Context, a: AlertaPendente): Resultado {
        // 1º envio: última localização conhecida, ainda sem horário
        if (!a.inicialOk) {
            if (!enviar(a.paraEnvio(null), criar = true)) {
                if (!a.avisouFalha) {
                    Fila.atualizarAlerta(a.id) { it.avisouFalha = true }
                    Estado.statusAlerta.value =
                        "Sem conexão. O alerta será reenviado até o servidor confirmar."
                    avisar(app, "Sem conexão: o alerta será reenviado até o servidor confirmar")
                }
                return Resultado.FALHOU
            }
            // Horário registrado só agora, depois do envio bem sucedido
            val confirmadoEm = Instant.now().toString()
            Fila.atualizarAlerta(a.id) {
                it.inicialOk = true
                it.confirmadoEm = confirmadoEm
            }
            Estado.statusAlerta.value = "Alerta recebido pelo servidor. Atualizando a localização…"
            avisar(app, "Alerta enviado ao servidor")
        }

        // 2º envio: localização precisa (ou a melhor disponível, passado o prazo)
        val atual = Fila.alerta(a.id) ?: return Resultado.OK
        val prazoVencido = System.currentTimeMillis() >= atual.prazoPrecisaMs
        if (!atual.precisaPronta && !prazoVencido) return Resultado.AGUARDANDO
        if (!enviar(atual.paraEnvio(atual.confirmadoEm), criar = false)) return Resultado.FALHOU

        Fila.removerAlerta(a.id)
        mostrarFinal(
            if (atual.precisaObtida) "Alerta enviado com a localização precisa"
            else "Alerta enviado (sem localização precisa)"
        )
        return Resultado.OK
    }

    /**
     * criar = true  (1º envio): POST; se o id já existir, atualiza com PUT.
     * criar = false (2º envio): PUT no mesmo id; se o registro não existir (404), cria com POST.
     */
    private suspend fun enviar(alerta: Alerta, criar: Boolean): Boolean = try {
        if (criar) {
            Api.service.enviarAlerta(alerta).isSuccessful ||
                Api.service.atualizarAlerta(alerta.id, alerta).isSuccessful
        } else {
            val r = Api.service.atualizarAlerta(alerta.id, alerta)
            r.isSuccessful || (r.code() == 404 && Api.service.enviarAlerta(alerta).isSuccessful)
        }
    } catch (e: Exception) {
        false
    }

    private fun AlertaPendente.paraEnvio(dataHora: String?) = Alerta(
        id = id,
        usuario = usuario,
        tipo = tipo,
        dataHora = dataHora,
        origem = origem,
        latitude = latitude,
        longitude = longitude,
        localizacaoEm = localizacaoEm
    )

    private suspend fun avisar(app: Context, texto: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(app, texto, Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarFinal(texto: String) {
        Estado.statusAlerta.value = texto
        scope.launch {
            delay(6000)
            if (Estado.statusAlerta.value == texto) Estado.statusAlerta.value = null
        }
    }
}
