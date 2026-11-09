package br.edu.hidoctor.saudecoletor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gera os dados simulados e os envia ao servidor a cada 5 s (10 s se falhar).
 * Começa quando a tela pede (startForegroundService) e termina com stopService.
 */
class ServicoSincronizacao : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var totalEnviado = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val canal = NotificationChannel(
            CANAL, "Sincronização", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarPrimeiroPlano()
        if (job == null) {
            Estado.rodando.value = true
            Estado.ultimoEnvio.value = null
            Estado.servidorOk.value = true
            Estado.sincronizacao.value = "Sincronizando..."
            job = scope.launch { laco() }
        }
        return START_NOT_STICKY
    }

    private fun iniciarPrimeiroPlano() {
        val notificacao = criarNotificacao(Estado.sincronizacao.value)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                ID_NOTIFICACAO, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(ID_NOTIFICACAO, notificacao)
        }
    }

    private suspend fun laco() {
        while (currentCoroutineContext().isActive) {
            var espera = INTERVALO_MS
            try {
                val usuario = Config.usuario(this)
                if (usuario == null) {
                    atualizar("Aguardando nome de usuário...")
                } else {
                    Simulador.posicao()   // mantém a "caminhada" andando
                    val leituras = Simulador.proximo(Estado.emergencias.value)
                    publicarLeituras(leituras)

                    val medicoes = montarMedicoes(usuario, leituras)
                    if (enviarMedicoes(medicoes)) {
                        totalEnviado += medicoes.size
                        Estado.ultimoEnvio.value = System.currentTimeMillis()
                        Estado.servidorOk.value = true
                        val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        atualizar("Último envio às $hora | total enviado: $totalEnviado")
                    } else {
                        Estado.servidorOk.value = false
                        atualizar("Servidor indisponível. Nova tentativa em 10 s.")
                        espera = ESPERA_ERRO_MS
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Estado.servidorOk.value = false
                atualizar("Erro: ${e.message}. Nova tentativa em 10 s.")
                espera = ESPERA_ERRO_MS
            }
            delay(espera)
        }
    }

    private fun criarNotificacao(texto: String): Notification =
        NotificationCompat.Builder(this, CANAL)
            .setContentTitle("HiDoctorTester")
            .setContentText(texto)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setOngoing(true)
            .build()

    private fun atualizar(texto: String) {
        Estado.sincronizacao.value = texto
        getSystemService(NotificationManager::class.java)
            .notify(ID_NOTIFICACAO, criarNotificacao(texto))
    }

    override fun onDestroy() {
        scope.cancel()
        job = null
        Estado.rodando.value = false
        Estado.sincronizacao.value = "Sincronização parada"
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "sync"
        private const val ID_NOTIFICACAO = 1
        private const val INTERVALO_MS = 5_000L     // gera e envia a cada 5 s
        private const val ESPERA_ERRO_MS = 10_000L  // após falha, espera 10 s
    }
}
