package br.edu.hidoctor.saudecoletor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.request.ChangesTokenRequest
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServicoSincronizacao : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var totalEnviado = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val canal = NotificationChannel(CANAL, "Sincronização de saúde",
            NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacao = criarNotificacao("Iniciando...")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID_NOTIFICACAO, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ID_NOTIFICACAO, notificacao)
        }
        if (job == null) job = scope.launch { laco() }
        return START_NOT_STICKY
    }

    private suspend fun laco() {
        val client = HealthConnectClient.getOrCreate(this)
        val prefs = getSharedPreferences("sync", Context.MODE_PRIVATE)
        val pedido = ChangesTokenRequest(TIPOS.toSet())
        var token = prefs.getString("token", null) ?: client.getChangesToken(pedido)
        atualizarNotificacao("Aguardando novos dados...")

        while (currentCoroutineContext().isActive) {
            var espera = INTERVALO_MS
            try {
                val resposta = client.getChanges(token)
                if (resposta.changesTokenExpired) {
                    token = client.getChangesToken(pedido)
                } else {
                    val medicoes = resposta.changes
                        .filterIsInstance<UpsertionChange>()
                        .flatMap { converter(it.record) }

                    if (medicoes.isEmpty() || enviarSemPerder(medicoes)) {
                        token = resposta.nextChangesToken
                        prefs.edit().putString("token", token).apply()
                        if (medicoes.isNotEmpty()) {
                            totalEnviado += medicoes.size
                            val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date())
                            atualizarNotificacao("Enviadas: $totalEnviado (último envio $hora)")
                        }
                        if (resposta.hasMore) espera = 0L
                    } else {
                        atualizarNotificacao("Servidor indisponível, tentando de novo...")
                        espera = ESPERA_ERRO_MS
                    }
                }
            } catch (e: Exception) {
                atualizarNotificacao("Erro: ${e.message}")
                espera = ESPERA_ERRO_MS
            }
            delay(espera)
        }
    }

    private fun criarNotificacao(texto: String): Notification =
        NotificationCompat.Builder(this, CANAL)
            .setContentTitle("Saúde Coletor")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

    private fun atualizarNotificacao(texto: String) {
        getSystemService(NotificationManager::class.java)
            .notify(ID_NOTIFICACAO, criarNotificacao(texto))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "sync"
        private const val ID_NOTIFICACAO = 1
        private const val INTERVALO_MS = 1000L   // consulta a cada 1 segundo
        private const val ESPERA_ERRO_MS = 5000L // espera maior após falha
    }
}