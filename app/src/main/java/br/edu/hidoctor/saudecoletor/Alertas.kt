package br.edu.hidoctor.saudecoletor

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

object Alertas {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ultimoEnvio = 0L

    @Synchronized
    private fun podeEnviar(): Boolean {
        val agora = SystemClock.elapsedRealtime()
        if (agora - ultimoEnvio < 5000) return false // evita alerta duplicado
        ultimoEnvio = agora
        return true
    }

    /**
     * Envia o alerta na hora, com a posição simulada. Devolve false se foi ignorado
     * (outro alerta há menos de 5 s). O resultado do envio chega em [aoTerminar], na thread principal.
     */
    fun disparar(context: Context, origem: String, aoTerminar: (Boolean) -> Unit = {}): Boolean {
        if (!podeEnviar()) return false
        val app = context.applicationContext
        scope.launch {
            val local = Simulador.posicao()
            val ok = try {
                Api.service.enviarAlerta(
                    Alerta(
                        id = UUID.randomUUID().toString(),
                        usuario = Config.usuario(app) ?: "semUsuario",
                        tipo = "volume_x3",
                        dataHora = Instant.now().toString(),
                        origem = origem,
                        latitude = local.latitude,
                        longitude = local.longitude,
                        localizacaoEm = local.momento.toString()
                    )
                ).isSuccessful
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) { aoTerminar(ok) }
        }
        return true
    }
}
