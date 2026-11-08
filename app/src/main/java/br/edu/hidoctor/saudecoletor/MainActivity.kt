package br.edu.hidoctor.saudecoletor

import android.Manifest
import android.content.Intent
import android.graphics.Color as CorAndroid
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController

class MainActivity : ComponentActivity() {

    private val detectorVolume = DetectorTriploVolume { Alertas.disparar(this, "app_aberto") }
    private var retomadas by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras do sistema transparentes, com ícones escuros (o app é sempre claro)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(CorAndroid.TRANSPARENT, CorAndroid.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(CorAndroid.TRANSPARENT, CorAndroid.TRANSPARENT)
        )
        setContent { TemaHidoctor { TelaPrincipal(retomadas) } }
    }

    override fun onResume() {
        super.onResume()
        retomadas++   // faz a tela rechecar as pendências ao voltar das configurações
    }

    // Funciona só com o app aberto na tela. Em segundo plano, quem cuida é o ServicoAlertaVolume.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.repeatCount == 0) detectorVolume.registrar()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

enum class Etapa { NOTIFICACAO, LOCALIZACAO, SAUDE, SEGUNDO_PLANO, ACESSIBILIDADE, CONCLUIDA }

@Composable
fun TelaPrincipal(retomadas: Int) {
    val context = LocalContext.current

    if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
        Column(Modifier.fillMaxSize().background(Cores.Fundo).padding(24.dp)) {
            Txt("hidoctor", 24, androidx.compose.ui.text.font.FontWeight.Bold, Cores.Teal)
            Txt(
                "Health Connect indisponível. Instale ou atualize o app pela Play Store.",
                14, cor = Cores.Tinta
            )
        }
        return
    }

    var usuario by remember { mutableStateOf(Config.usuario(context)) }
    val nome = usuario
    if (nome == null) {
        TelaNome { novo ->
            Config.salvarUsuario(context, novo)
            usuario = novo
        }
    } else {
        TelaConfigurada(nome, retomadas)
    }
}

@Composable
fun TelaConfigurada(usuario: String, retomadas: Int) {
    val context = LocalContext.current
    val client = remember { HealthConnectClient.getOrCreate(context) }
    var etapa by rememberSaveable { mutableStateOf(Etapa.NOTIFICACAO) }
    var pendencias by remember { mutableStateOf<List<String>>(emptyList()) }

    val notificacaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { etapa = Etapa.LOCALIZACAO }

    val localizacaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { etapa = Etapa.SAUDE }

    val saudeLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { etapa = Etapa.SEGUNDO_PLANO }

    val segundoPlanoLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { etapa = Etapa.ACESSIBILIDADE }

    // Pede, uma etapa por vez, tudo o que ainda falta
    LaunchedEffect(etapa) {
        when (etapa) {
            Etapa.NOTIFICACAO -> {
                if (Build.VERSION.SDK_INT >= 33 &&
                    !concedida(context, Manifest.permission.POST_NOTIFICATIONS)
                ) notificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else etapa = Etapa.LOCALIZACAO
            }
            Etapa.LOCALIZACAO -> {
                if (!Localizacao.temPermissao(context)) {
                    localizacaoLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else etapa = Etapa.SAUDE
            }
            Etapa.SAUDE -> {
                val concedidas = client.permissionController.getGrantedPermissions()
                if (!concedidas.containsAll(PERMISSOES_LEITURA)) {
                    saudeLauncher.launch(PERMISSOES_LEITURA)
                } else etapa = Etapa.SEGUNDO_PLANO
            }
            Etapa.SEGUNDO_PLANO -> {
                // Pedida uma vez só: pode não existir neste aparelho
                val concedidas = client.permissionController.getGrantedPermissions()
                if (PERMISSAO_SEGUNDO_PLANO !in concedidas &&
                    !Config.jaFez(context, "segundoPlano")
                ) {
                    Config.marcarFeito(context, "segundoPlano")
                    try {
                        segundoPlanoLauncher.launch(setOf(PERMISSAO_SEGUNDO_PLANO))
                    } catch (e: Exception) {
                        etapa = Etapa.ACESSIBILIDADE
                    }
                } else etapa = Etapa.ACESSIBILIDADE
            }
            Etapa.ACESSIBILIDADE -> {
                // Não existe pedido por diálogo: só abrir a tela de configurações (uma vez)
                if (!acessibilidadeAtiva(context) && !Config.jaFez(context, "acessibilidade")) {
                    Config.marcarFeito(context, "acessibilidade")
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                etapa = Etapa.CONCLUIDA
            }
            Etapa.CONCLUIDA -> {}
        }
    }

    // Recalcula as pendências e liga a sincronização automática assim que possível
    LaunchedEffect(retomadas, etapa) {
        val concedidas = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
        val leituraOk = concedidas.containsAll(PERMISSOES_LEITURA)

        val lista = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 &&
            !concedida(context, Manifest.permission.POST_NOTIFICATIONS)
        ) lista += "Notificações: libere em Configurações > Apps > hidoctor > Notificações"
        if (!Localizacao.temPermissao(context)) {
            lista += "Localização: libere em Configurações > Apps > hidoctor > Permissões"
        } else if (!Localizacao.ligada(context)) {
            lista += "Localização do aparelho desligada: ligue nas configurações rápidas"
        }
        if (!leituraOk) {
            lista += "Health Connect: permita a leitura em Health Connect > Acesso do app > hidoctor"
        }
        if (!acessibilidadeAtiva(context)) {
            lista += "Alerta em segundo plano: ative o alerta de volume do hidoctor em Configurações > Acessibilidade"
        }
        pendencias = lista

        if (leituraOk && etapa.ordinal >= Etapa.SEGUNDO_PLANO.ordinal) {
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, ServicoSincronizacao::class.java)
                )
            } catch (e: Exception) {
                // tenta de novo na próxima retomada do app
            }
        }
    }

    PainelPrincipal(usuario, pendencias)
}
