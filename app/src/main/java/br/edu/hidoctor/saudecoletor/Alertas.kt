package br.edu.hidoctor.saudecoletor

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.*
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

    fun disparar(context: Context, origem: String) {
        if (!podeEnviar()) return
        val app = context.applicationContext
        scope.launch {
            val ok = try {
                Api.service.enviarAlerta(
                    Alerta(
                        id = UUID.randomUUID().toString(),
                        tipo = "volume_x3",
                        dataHora = Instant.now().toString(),
                        origem = origem
                    )
                ).isSuccessful
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    app,
                    if (ok) "Alerta enviado ao servidor" else "Falha ao enviar o alerta",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}