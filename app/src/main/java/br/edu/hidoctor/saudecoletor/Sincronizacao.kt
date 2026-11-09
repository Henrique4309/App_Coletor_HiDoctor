package br.edu.hidoctor.saudecoletor

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.roundToLong

const val SEM_DADOS = "semDados"
const val JANELA_DIAS = 30L   // quão para trás procurar a última leitura existente

/** Estado compartilhado entre o serviço, os alertas e a tela. */
object Estado {
    val sincronizacao = MutableStateFlow("Aguardando...")   // texto da notificação
    val ultimoEnvio = MutableStateFlow<Long?>(null)         // último envio confirmado pelo servidor
    val servidorOk = MutableStateFlow(true)
    val filaTamanho = MutableStateFlow(0)                   // medições aguardando envio
    val statusAlerta = MutableStateFlow<String?>(null)      // andamento do alerta, para a tela
}

fun guardarSeMaisRecente(mapa: MutableMap<String, Leitura>, nova: Leitura) {
    val atual = mapa[nova.tipo]
    if (atual == null || nova.momento.isAfter(atual.momento)) mapa[nova.tipo] = nova
}

/** Lê, para cada tipo ATIVO, a leitura mais recente que já existe no Health Connect. */
suspend fun lerUltimas(client: HealthConnectClient): Map<String, Leitura> {
    val fim = Instant.now()
    val filtro = TimeRangeFilter.between(fim.minus(JANELA_DIAS, ChronoUnit.DAYS), fim)
    val saida = mutableMapOf<String, Leitura>()
    for (tipo in TIPOS) {
        val registros = client.readRecords(
            ReadRecordsRequest(tipo, filtro, ascendingOrder = false, pageSize = 20)
        ).records
        registros.flatMap { converter(it) }.forEach { guardarSeMaisRecente(saida, it) }
    }
    return saida
}

/** Número inteiro quando for inteiro; senão, no máximo duas casas decimais. */
fun arredondar(v: Double): Number =
    if (v % 1.0 == 0.0) v.toLong() else (v * 100.0).roundToLong() / 100.0

/**
 * Compara cada tipo com o último valor registrado. Se mudou, cria a medição AGORA
 * (com o id definido na coleta) e a coloca na fila. Sem mudança, não cria nada.
 * Sem leitura nenhuma, o valor é "semDados" (também só quando muda).
 */
fun registrarAlteracoes(
    usuario: String,
    ultimas: Map<String, Leitura>,
    chaves: MutableMap<String, String>
) {
    for (tipo in TIPOS) {
        val info = INFO_TIPOS.getValue(tipo)
        val l = ultimas[info.nome]
        val valor: Any = if (l != null) arredondar(l.valor) else SEM_DADOS
        val valor2: Any? = l?.valor2?.let { arredondar(it) }
        val chave = "$valor|$valor2"
        if (chaves[info.nome] == chave) continue
        chaves[info.nome] = chave
        Fila.adicionarMedicao(
            Medicao(
                id = UUID.randomUUID().toString(),
                usuario = usuario,
                tipo = info.nome,
                valor = valor,
                valor2 = valor2,
                unidade = info.unidade,
                dataHora = l?.momento?.toString()
            )
        )
    }
}

/**
 * Envia a fila, da medição mais antiga para a mais nova. Cada uma sai da fila
 * somente depois que o servidor confirma. Devolve false se o servidor não respondeu.
 */
suspend fun esvaziarFila(): Boolean {
    while (true) {
        val m = Fila.primeiraMedicao() ?: return true
        val pronta = m.copy(enviadoEm = Instant.now().toString())
        val ok = try {
            // Se o id já existe (envio anterior sem resposta), atualiza em vez de duplicar
            Api.service.enviarMedicao(pronta).isSuccessful ||
                Api.service.atualizarMedicao(pronta.id, pronta).isSuccessful
        } catch (e: Exception) {
            false
        }
        if (!ok) return false
        Fila.removerMedicao(m.id)
    }
}

/** Dado simples que confirma que o app está funcionando. Não entra na fila. */
/** Dado simples que confirma que o app está funcionando. Não entra na fila. */
suspend fun enviarAtividade(usuario: String): Boolean {
    val m = Medicao(
        id = UUID.randomUUID().toString(),
        usuario = usuario,
        tipo = "app_ativo",
        valor = 1,
        enviadoEm = Instant.now().toString()
    )
    return try {
        Api.service.enviarMedicao(m).isSuccessful
    } catch (e: Exception) {
        false
    }
}
