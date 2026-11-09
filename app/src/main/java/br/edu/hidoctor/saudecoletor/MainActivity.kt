package br.edu.hidoctor.saudecoletor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as CorAndroid
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras do sistema transparentes, com ícones escuros (o app é sempre claro)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(CorAndroid.TRANSPARENT, CorAndroid.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(CorAndroid.TRANSPARENT, CorAndroid.TRANSPARENT)
        )
        setContent { TemaHidoctor { TelaPrincipal() } }
    }
}

@Composable
fun TelaPrincipal() {
    val context = LocalContext.current
    var usuario by remember { mutableStateOf(Config.usuario(context)) }

    // Pede a permissão de notificações (Android 13+) assim que o nome de usuário existir
    val notificacaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val nome = usuario
    LaunchedEffect(nome) {
        if (nome != null && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (nome == null) {
        TelaNome { novo ->
            Config.salvarUsuario(context, novo)
            usuario = novo
        }
    } else {
        PainelPrincipal(nome)
    }
}