package br.edu.hidoctor.saudecoletor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Locale

// ───────────────────────── Fundo comum ─────────────────────────

/** Fundo cinza claro com formas arredondadas e cruzinhas, como na tela de referência. */
fun Modifier.fundoHidoctor(): Modifier = this
    .background(Cores.Fundo)
    .drawBehind {
        val d = density
        drawOval(
            Cores.Claro,
            topLeft = Offset(-87 * d, -100 * d),
            size = Size(360 * d, 313 * d)
        )
        drawOval(
            Cores.Claro,
            topLeft = Offset(size.width - 147 * d, size.height - 153 * d),
            size = Size(253 * d, 253 * d)
        )
        fun cruz(cx: Float, cy: Float, r: Float) {
            drawLine(Cores.Borda, Offset(cx - r, cy), Offset(cx + r, cy), 1.7f * d, StrokeCap.Round)
            drawLine(Cores.Borda, Offset(cx, cy - r), Offset(cx, cy + r), 1.7f * d, StrokeCap.Round)
        }
        cruz(size.width * 0.86f, 83 * d, 5.5f * d)
        cruz(size.width * 0.08f, 253 * d, 4f * d)
        cruz(size.width * 0.90f, 383 * d, 4f * d)
        cruz(size.width * 0.12f, size.height - 67 * d, 4.7f * d)
    }

// ───────────────────────── Tela do nome (primeiro início) ─────────────────────────

@Composable
fun TelaNome(aoConfirmar: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    val valido = Config.valido(texto)
    val invalido = texto.isNotEmpty() && !valido

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fundoHidoctor()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.height(32.dp))

        // Logo grande dentro de círculos, com o balão de bpm e um coração flutuante
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(210.dp).background(Cores.Claro, CircleShape))
            Box(Modifier.size(164.dp).background(Cores.Borda.copy(alpha = 0.7f), CircleShape))
            Box(
                Modifier.shadow(
                    18.dp, CircleShape,
                    ambientColor = Cores.Sombra, spotColor = Cores.Sombra
                )
            ) { LogoHidoctor(116.dp) }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = 10.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Cores.SombraLeve, spotColor = Cores.SombraLeve)
                    .background(Color.White, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoracaoDecorativo(14.dp, Cores.Escuro)
                Box(Modifier.width(5.dp))
                Txt("72 bpm", 11, FontWeight.SemiBold, Cores.Tinta)
            }
            Box(Modifier.align(Alignment.BottomStart).offset(x = 8.dp, y = (-14).dp)) {
                CoracaoDecorativo(22.dp, Cores.Escuro.copy(alpha = 0.55f))
            }
        }

        Box(Modifier.height(24.dp))
        Txt(
            buildAnnotatedString {
                append("Bem-vindo ao ")
                withStyle(SpanStyle(color = Cores.Escuro)) { append("hidoctor") }
            },
            26, FontWeight.Bold, Cores.Tinta, alinhar = TextAlign.Center
        )
        Box(Modifier.height(6.dp))
        Txt(
            "Versão para simulações. O nome de usuário identifica os dados enviados ao servidor.",
            13, FontWeight.Normal, Cores.Suave, alinhar = TextAlign.Center
        )
        Box(Modifier.height(24.dp))

        // Cartão com o campo de nome
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Cores.SombraLeve, spotColor = Cores.SombraLeve)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Txt("Como podemos te chamar?", 16, FontWeight.SemiBold, Cores.Tinta)
            Box(Modifier.height(12.dp))
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Txt("Nome de usuário", 13, FontWeight.Normal, Cores.Suave) },
                singleLine = true,
                isError = invalido,
                textStyle = TextStyle(fontFamily = Poppins, fontSize = 17.sp, color = Cores.Tinta),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = { if (valido) IconeCheck(24.dp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valido) aoConfirmar(texto) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cores.Escuro,
                    unfocusedBorderColor = Cores.Borda,
                    focusedLabelColor = Cores.Escuro,
                    cursorColor = Cores.Escuro,
                    errorBorderColor = Cores.MaisEscuro,
                    errorCursorColor = Cores.MaisEscuro,
                    errorLabelColor = Cores.MaisEscuro,
                    focusedTextColor = Cores.Tinta,
                    unfocusedTextColor = Cores.Tinta
                )
            )
            Box(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeInfo(16.dp, if (invalido) Cores.MaisEscuro else Cores.Suave)
                Box(Modifier.width(8.dp))
                Txt(
                    if (invalido) "Use apenas letras (A–Z) e números, sem espaços ou acentos."
                    else "Apenas letras sem acento e números.",
                    12, if (invalido) FontWeight.SemiBold else FontWeight.Normal,
                    if (invalido) Cores.MaisEscuro else Cores.Suave
                )
            }
        }

        Box(Modifier.height(20.dp))
        Button(
            onClick = { aoConfirmar(texto) },
            enabled = valido,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Cores.Escuro,
                contentColor = Color.White,
                disabledContainerColor = Cores.Borda,
                disabledContentColor = Cores.Suave
            )
        ) { Txt("Continuar  →", 16, FontWeight.SemiBold, Color.Unspecified) }

        Box(Modifier.height(24.dp))
    }
}

// ───────────────────────── Tela principal ─────────────────────────

private fun formatarValor(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString()
    else String.format(Locale("pt", "BR"), "%.2f", v).trimEnd('0').trimEnd(',')

private class DefMetrica(
    val tipo: String,
    val titulo: AnnotatedString,
    val unidade: String,
    val icone: IconeMetrica,
    val tamValor: Int = 30,
    val tamUnidade: Int = 13
)

private val TITULO_O2: AnnotatedString = buildAnnotatedString {
    append("Saturação de O")
    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 8.sp)) { append("2") }
}

private val METRICAS = listOf(
    DefMetrica(Tipos.FC, AnnotatedString("Frequência cardíaca"), "bpm", IconeMetrica.CORACAO),
    DefMetrica(Tipos.FC_REPOUSO, AnnotatedString("FC em repouso"), "bpm", IconeMetrica.LUA),
    DefMetrica(Tipos.HRV, AnnotatedString("Variabilidade da FC"), "ms", IconeMetrica.ONDA),
    DefMetrica(Tipos.SPO2, TITULO_O2, "%", IconeMetrica.GOTA),
    DefMetrica(Tipos.RESP, AnnotatedString("Freq. respiratória"), "rpm", IconeMetrica.RESPIRACAO),
    DefMetrica(Tipos.TEMP, AnnotatedString("Temp. corporal"), "°C", IconeMetrica.TEMPERATURA),
    DefMetrica(Tipos.GLICEMIA, AnnotatedString("Glicemia"), "mg/dL", IconeMetrica.GLICEMIA, 30, 11),
    DefMetrica(Tipos.PRESSAO, AnnotatedString("Pressão arterial"), "mmHg", IconeMetrica.PRESSAO, 22, 11)
)

@Composable
fun PainelPrincipal(usuario: String) {
    val context = LocalContext.current
    val leituras by Estado.leituras.collectAsState()
    val historico by Estado.historico.collectAsState()
    val ultimoEnvio by Estado.ultimoEnvio.collectAsState()
    val servidorOk by Estado.servidorOk.collectAsState()
    val rodando by Estado.rodando.collectAsState()
    val emergencias by Estado.emergencias.collectAsState()

    // Relógio para o "Sincronizado há X s"
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            agora = System.currentTimeMillis()
        }
    }

    var statusAlerta by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(statusAlerta) {
        if (statusAlerta != null && statusAlerta != "Enviando…") {
            delay(4000)
            statusAlerta = null
        }
    }

    val textoSync = when {
        !rodando -> "Sincronização parada"
        !servidorOk -> "Servidor indisponível"
        ultimoEnvio == null -> "Conectando…"
        else -> "Sincronizado há ${((agora - (ultimoEnvio ?: agora)) / 1000).coerceAtLeast(0)} s"
    }
    val corSync = if (rodando && servidorOk && ultimoEnvio != null) Cores.Escuro else Cores.Suave

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fundoHidoctor()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Box(Modifier.height(14.dp))

        // Cabeçalho
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoHidoctor(44.dp)
            Box(Modifier.width(12.dp))
            Column {
                Txt("hidoctor", 24, FontWeight.Bold, Cores.Escuro)
                Txt("versão para simulações", 11, FontWeight.Medium, Cores.Suave)
            }
        }

        Box(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Cores.Borda, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconeInfo(16.dp, Cores.Suave)
            Box(Modifier.width(8.dp))
            Txt(
                "Modo simulação: os dados desta tela são gerados pelo app.",
                11, FontWeight.Normal, Cores.Suave
            )
        }

        Box(Modifier.height(18.dp))
        Txt("Olá,", 16, FontWeight.Normal, Cores.Suave)
        Txt(usuario, 32, FontWeight.Bold, Cores.Tinta, maxLinhas = 1)
        Box(Modifier.height(10.dp))

        // Chip de sincronização
        Row(
            modifier = Modifier
                .shadow(4.dp, CircleShape, ambientColor = Cores.SombraLeve, spotColor = Cores.SombraLeve)
                .background(Color.White, CircleShape)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(corSync, CircleShape))
            Box(Modifier.width(8.dp))
            Txt(textoSync, 12, FontWeight.Medium, corSync)
        }

        Box(Modifier.height(16.dp))
        BotaoSincronizacao(rodando) {
            val intent = Intent(context, ServicoSincronizacao::class.java)
            if (rodando) context.stopService(intent)
            else ContextCompat.startForegroundService(context, intent)
        }
        if (!rodando && emergencias.isNotEmpty()) {
            Box(Modifier.height(6.dp))
            Txt(
                "As simulações escolhidas entram em ação quando a sincronização iniciar.",
                11, FontWeight.Normal, Cores.Suave
            )
        }

        // Cards de dados
        Box(Modifier.height(18.dp))
        METRICAS.chunked(2).forEach { par ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                par.forEach { m ->
                    val l = leituras[m.tipo]
                    val v2 = l?.valor2
                    val texto = when {
                        l == null -> null
                        v2 != null -> "${formatarValor(l.valor)}/${formatarValor(v2)}"
                        else -> formatarValor(l.valor)
                    }
                    CardMetrica(
                        titulo = m.titulo,
                        valor = texto,
                        unidade = m.unidade,
                        icone = m.icone,
                        historico = historico[m.tipo].orEmpty(),
                        modifier = Modifier.weight(1f),
                        tamValor = m.tamValor,
                        tamUnidade = m.tamUnidade
                    )
                }
                if (par.size == 1) Box(Modifier.weight(1f))
            }
            Box(Modifier.height(10.dp))
        }

        // Simulações
        Box(Modifier.height(10.dp))
        Txt("Simulações", 16, FontWeight.SemiBold, Cores.Tinta)
        Txt("Toque para iniciar ou parar. O app só envia os números ao servidor.", 11, FontWeight.Normal, Cores.Suave)
        Box(Modifier.height(10.dp))
        BotaoSimulacao(
            "Parada cardiorrespiratória", "batimentos, respiração e pressão caem a zero",
            Emergencia.PARADA in emergencias
        ) { alternarEmergencia(Emergencia.PARADA) }
        Box(Modifier.height(8.dp))
        BotaoSimulacao(
            "Arritmia cardíaca", "batimentos rápidos e irregulares",
            Emergencia.ARRITMIA in emergencias
        ) { alternarEmergencia(Emergencia.ARRITMIA) }
        Box(Modifier.height(8.dp))
        BotaoSimulacao(
            "Hipoglicemia", "glicemia em queda até nível crítico",
            Emergencia.HIPOGLICEMIA in emergencias
        ) { alternarEmergencia(Emergencia.HIPOGLICEMIA) }
        Box(Modifier.height(8.dp))
        BotaoSimulacao(
            "Hiperglicemia", "glicemia em alta até nível crítico",
            Emergencia.HIPERGLICEMIA in emergencias
        ) { alternarEmergencia(Emergencia.HIPERGLICEMIA) }

        // Pedido de socorro (discreto)
        Box(Modifier.height(18.dp))
        BotaoDiscreto("Simular pedido de socorro") {
            statusAlerta = "Enviando…"
            val iniciou = Alertas.disparar(context, "botao") { ok ->
                statusAlerta = if (ok) "Pedido de socorro enviado" else "Falha ao enviar o pedido"
            }
            if (!iniciou) statusAlerta = null
        }
        statusAlerta?.let {
            Box(Modifier.height(6.dp))
            Txt(it, 12, FontWeight.Medium, Cores.Suave, Modifier.fillMaxWidth(), TextAlign.Center)
        }

        // Rodapé: localização
        Box(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Cores.SombraLeve, spotColor = Cores.SombraLeve)
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgeLocalizacao(38.dp)
            Box(Modifier.width(12.dp))
            Column {
                Txt("Localização gerada pelo app", 14, FontWeight.SemiBold, Cores.Tinta)
                Txt("enviada junto com o pedido de socorro", 12, FontWeight.Normal, Cores.Suave)
            }
        }
        Box(Modifier.height(20.dp))
    }
}
