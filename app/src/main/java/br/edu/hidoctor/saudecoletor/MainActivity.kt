package br.edu.hidoctor.saudecoletor

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val detectorVolume = DetectorTriploVolume { Alertas.disparar(this, "app_aberto") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TelaPrincipal() } }
    }

    // Funciona só com o app aberto na tela. Em segundo plano, quem cuida é o ServicoAlertaVolume.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.repeatCount == 0) detectorVolume.registrar()
            return true // consome o toque (o volume não muda com o app aberto)
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun TelaPrincipal() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Pronto.") }

    if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
        Column(Modifier.padding(24.dp)) {
            Text("Health Connect indisponível. Instale ou atualize o app pela Play Store.")
        }
        return
    }

    val client = remember { HealthConnectClient.getOrCreate(context) }
    val intentServico = remember { Intent(context, ServicoSincronizacao::class.java) }

    fun iniciarServico() {
        ContextCompat.startForegroundService(context, intentServico)
        status = "Sincronização automática ligada."
    }

    val permissoesLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { concedidas ->
        status = "Permissões concedidas: ${concedidas.size} de ${TODAS_PERMISSOES.size}"
        if (concedidas.containsAll(PERMISSOES_LEITURA)) iniciarServico()
    }

    val segundoPlanoLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { concedidas ->
        status = if (concedidas.isNotEmpty()) "Leitura em segundo plano permitida."
        else "Leitura em segundo plano não concedida (ou sem suporte neste aparelho)."
    }

    val notificacaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val concedidas = client.permissionController.getGrantedPermissions()
        if (concedidas.containsAll(PERMISSOES_LEITURA)) iniciarServico()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Saúde Coletor", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = { permissoesLauncher.launch(TODAS_PERMISSOES) }) {
            Text("Pedir permissões")
        }

        Button(onClick = {
            segundoPlanoLauncher.launch(
                setOf("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND")
            )
        }) {
            Text("Permitir leitura em segundo plano")
        }

        Button(onClick = {
            scope.launch {
                status = try { inserirLeituraAgora(client) }
                catch (e: Exception) { "Erro: ${e.message}" }
            }
        }) {
            Text("Gerar leitura agora")
        }

        Button(onClick = {
            scope.launch {
                status = try { inserirDadosDeTeste(client) }
                catch (e: Exception) { "Erro: ${e.message}" }
            }
        }) {
            Text("Gerar dados de teste (24 h)")
        }

        Button(onClick = {
            scope.launch {
                status = try { limparDadosDeTeste(client) }
                catch (e: Exception) { "Erro: ${e.message}" }
            }
        }) {
            Text("Limpar dados de teste")
        }

        Button(onClick = {
            scope.launch {
                status = "Sincronizando..."
                status = try { enviarMedicoes(lerMedicoes(client)) }
                catch (e: Exception) { "Erro: ${e.message}" }
            }
        }) {
            Text("Sincronizar (manual)")
        }

        Button(onClick = { iniciarServico() }) {
            Text("Iniciar sincronização automática")
        }

        Button(onClick = {
            context.stopService(intentServico)
            status = "Sincronização automática parada."
        }) {
            Text("Parar sincronização automática")
        }

        Button(onClick = {
            Alertas.disparar(context, "botao_teste")
        }) {
            Text("Enviar alerta de teste")
        }

        Button(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }) {
            Text("Ativar alerta em segundo plano (acessibilidade)")
        }

        Text(status)
    }
}