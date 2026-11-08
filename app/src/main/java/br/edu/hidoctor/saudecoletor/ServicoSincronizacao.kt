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
        val canal = NotificationChannel(
            CANAL, "Sincronização de saúde", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarPrimeiroPlano()
        if (job == null) job = scope.launch { laco() }
        return START_NOT_STICKY
    }

    private fun iniciarPrimeiroPlano() {
        val notificacao = criarNotificacao(Estado.sincronizacao.value)
        if (Build.VERSION.SDK_INT >= 29) {
            var tipos = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            // O tipo "location" mantém a localização acessível com o app em segundo plano
            if (Localizacao.temPermissao(this)) {
                tipos = tipos or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            try {
                startForeground(ID_NOTIFICACAO, notificacao, tipos)
            } catch (e: Exception) {
                startForeground(
                    ID_NOTIFICACAO, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
        } else {
            startForeground(ID_NOTIFICACAO, notificacao)
        }
    }

    private suspend fun laco() {
        val client = HealthConnectClient.getOrCreate(this)
        val pedido = ChangesTokenRequest(TIPOS.toSet())
        var token: String? = null
        val ultimas = mutableMapOf<String, Leitura>()

        while (currentCoroutineContext().isActive) {
            var espera = INTERVALO_MS
            try {
                val usuario = Config.usuario(this)
                if (usuario == null) {
                    atualizar("Aguardando nome de usuário...")
                } else {
                    // 1) Atualiza o que se sabe sobre os dados
                    if (token == null) {
                        val novoToken = client.getChangesToken(pedido)
                        val lidas = lerUltimas(client)
                        token = novoToken
                        ultimas.clear()
                        ultimas.putAll(lidas)
                    } else {
                        var resposta = client.getChanges(token)
                        while (true) {
                            if (resposta.changesTokenExpired) {
                                token = null   // será recriado no próximo ciclo
                                break
                            }
                            resposta.changes
                                .filterIsInstance<UpsertionChange>()
                                .flatMap { converter(it.record) }
                                .forEach { guardarSeMaisRecente(ultimas, it) }
                            token = resposta.nextChangesToken
                            if (!resposta.hasMore) break
                            resposta = client.getChanges(token)
                        }
                    }
                    publicarLeituras(ultimas)
                    // 2) Envia: leitura nova, última conhecida ou "semDados"
                    val medicoes = montarMedicoes(usuario, ultimas)
                    if (enviarMedicoes(medicoes)) {
                        totalEnviado += medicoes.size
                        val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        atualizar("Último envio às $hora | total enviado: $totalEnviado")
                        Estado.ultimoEnvio.value = System.currentTimeMillis()
                        Estado.servidorOk.value = true
                    } else {
                        atualizar("Servidor indisponível. Nova tentativa em 10 s.")
                        Estado.servidorOk.value = false
                        espera = ESPERA_ERRO_MS
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                atualizar("Erro: ${e.message}. Nova tentativa em 10 s.")
                Estado.servidorOk.value = false
                espera = ESPERA_ERRO_MS
            }
            delay(espera)
        }
    }

    private fun criarNotificacao(texto: String): Notification =
        NotificationCompat.Builder(this, CANAL)
            .setContentTitle("hidoctor")
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
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "sync"
        private const val ID_NOTIFICACAO = 1
        private const val INTERVALO_MS = 5_000L     // consulta e envio a cada 5 s
        private const val ESPERA_ERRO_MS = 10_000L  // após falha, espera 10 s
    }
}