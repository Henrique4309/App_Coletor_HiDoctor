package br.edu.hidoctor.saudecoletor

import androidx.compose.foundation.background
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
import kotlinx.coroutines.delay
import java.util.Locale

// ───────────────────────── Fundo comum ─────────────────────────

/** Fundo menta com formas arredondadas e cruzinhas, como na tela de referência. */
fun Modifier.fundoHidoctor(): Modifier = this
    .background(Cores.Fundo)
    .drawBehind {
        val d = density
        drawOval(
            Cores.Menta,
            topLeft = Offset(-87 * d, -100 * d),
            size = Size(360 * d, 313 * d)
        )
        drawOval(
            Cores.Menta,
            topLeft = Offset(size.width - 147 * d, size.height - 153 * d),
            size = Size(253 * d, 253 * d)
        )
        fun cruz(cx: Float, cy: Float, r: Float) {
            drawLine(Cores.Menta2, Offset(cx - r, cy), Offset(cx + r, cy), 1.7f * d, StrokeCap.Round)
            drawLine(Cores.Menta2, Offset(cx, cy - r), Offset(cx, cy + r), 1.7f * d, StrokeCap.Round)
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

        // Logo grande dentro de círculos menta, com o balão de bpm e um coração flutuante
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(210.dp).background(Cores.Menta, CircleShape))
            Box(Modifier.size(164.dp).background(Cores.Menta2.copy(alpha = 0.7f), CircleShape))
            Box(
                Modifier.shadow(
                    18.dp, CircleShape,
                    ambientColor = Color(0x660F9C89), spotColor = Color(0x660F9C89)
                )
            ) { LogoHidoctor(116.dp) }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = 10.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color(0x330F5046), spotColor = Color(0x330F5046))
                    .background(Color.White, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoracaoDecorativo(14.dp, Cores.Rosa)
                Box(Modifier.width(5.dp))
                Txt("72 bpm", 11, FontWeight.SemiBold, Cores.Tinta)
            }
            Box(Modifier.align(Alignment.BottomStart).offset(x = 8.dp, y = (-14).dp)) {
                CoracaoDecorativo(22.dp, Cores.Rosa.copy(alpha = 0.55f))
            }
        }

        Box(Modifier.height(24.dp))
        Txt(
            buildAnnotatedString {
                append("Bem-vindo ao ")
                withStyle(SpanStyle(color = Cores.Teal)) { append("hidoctor") }
            },
            26, FontWeight.Bold, Cores.Tinta, alinhar = TextAlign.Center
        )
        Box(Modifier.height(6.dp))
        Txt(
            "Seus dados de saúde sempre à mão, e ajuda a um toque de distância.",
            13, FontWeight.Normal, Cores.Suave, alinhar = TextAlign.Center
        )
        Box(Modifier.height(24.dp))

        // Cartão com o campo de nome
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x330F5046), spotColor = Color(0x330F5046))
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
                    focusedBorderColor = Cores.Teal,
                    unfocusedBorderColor = Cores.Menta2,
                    focusedLabelColor = Cores.Teal,
                    cursorColor = Cores.Teal,
                    errorBorderColor = Cores.Vermelho,
                    focusedTextColor = Cores.Tinta,
                    unfocusedTextColor = Cores.Tinta
                )
            )
            Box(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeInfo(16.dp, if (invalido) Cores.Vermelho else Cores.Suave)
                Box(Modifier.width(8.dp))
                Txt(
                    if (invalido) "Use apenas letras (A–Z) e números, sem espaços ou acentos."
                    else "Apenas letras sem acento e números.",
                    12, FontWeight.Normal, if (invalido) Cores.Vermelho else Cores.Suave
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
                containerColor = Cores.Teal,
                contentColor = Color.White,
                disabledContainerColor = Cores.Menta2,
                disabledContentColor = Cores.Suave
            )
        ) { Txt("Continuar  →", 16, FontWeight.SemiBold, Color.Unspecified) }

        Box(Modifier.height(24.dp))
        IndicadorEtapas(total = 3, atual = 0)
        Box(Modifier.height(14.dp))
        Txt(
            "Em seguida vamos pedir as permissões de saúde, notificações e localização.",
            12, FontWeight.Normal, Cores.Suave, alinhar = TextAlign.Center
        )
        Box(Modifier.height(24.dp))
    }
}

// ───────────────────────── Tela principal ─────────────────────────

private fun formatarValor(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString()
    else String.format(Locale("pt", "BR"), "%.2f", v).trimEnd('0').trimEnd(',')

@Composable
fun PainelPrincipal(usuario: String, pendencias: List<String>) {
    val context = LocalContext.current
    val leituras by Estado.leituras.collectAsState()
    val historico by Estado.historico.collectAsState()
    val ultimoEnvio by Estado.ultimoEnvio.collectAsState()
    val servidorOk by Estado.servidorOk.collectAsState()

    // Relógio para o "Sincronizado há X s"
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            agora = System.currentTimeMillis()
        }
    }

    var alertaEnviado by remember { mutableStateOf(false) }
    LaunchedEffect(alertaEnviado) {
        if (alertaEnviado) {
            delay(4000)
            alertaEnviado = false
        }
    }

    val textoSync = when {
        !servidorOk -> "Servidor indisponível"
        ultimoEnvio == null -> "Conectando…"
        else -> "Sincronizado há ${((agora - (ultimoEnvio ?: agora)) / 1000).coerceAtLeast(0)} s"
    }
    val corSync = when {
        !servidorOk -> Cores.Vermelho
        ultimoEnvio == null -> Cores.Amarelo
        else -> Cores.Teal
    }

    val localizacaoAtiva = Localizacao.temPermissao(context) && Localizacao.ligada(context)

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
                Txt("hidoctor", 24, FontWeight.Bold, Cores.Teal)
                Txt("monitoramento de saúde", 11, FontWeight.Normal, Cores.Suave)
            }
        }

        Box(Modifier.height(26.dp))
        Txt("Olá,", 16, FontWeight.Normal, Cores.Suave)
        Txt(usuario, 32, FontWeight.Bold, Cores.Tinta, maxLinhas = 1)
        Box(Modifier.height(10.dp))

        // Chip de sincronização
        Row(
            modifier = Modifier
                .shadow(4.dp, CircleShape, ambientColor = Color(0x220F5046), spotColor = Color(0x220F5046))
                .background(Color.White, CircleShape)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(corSync, CircleShape))
            Box(Modifier.width(8.dp))
            Txt(textoSync, 12, FontWeight.Medium, if (servidorOk) Cores.TealEscuro else corSync)
        }

        Box(Modifier.height(20.dp))

        // Cards de dados
        val tituloO2: AnnotatedString = buildAnnotatedString {
            append("Saturação de O")
            withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 8.sp)) { append("2") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardMetrica(
                titulo = AnnotatedString("Frequência cardíaca"),
                valor = leituras["frequencia_cardiaca"]?.let { formatarValor(it.valor) },
                unidade = "bpm", icone = IconeMetrica.CORACAO,
                cor = Cores.Rosa, fundoIcone = Cores.RosaFundo,
                historico = historico["frequencia_cardiaca"].orEmpty(),
                modifier = Modifier.weight(1f)
            )
            CardMetrica(
                titulo = AnnotatedString("FC em repouso"),
                valor = leituras["frequencia_repouso"]?.let { formatarValor(it.valor) },
                unidade = "bpm", icone = IconeMetrica.LUA,
                cor = Cores.Amarelo, fundoIcone = Cores.AmareloFundo,
                historico = historico["frequencia_repouso"].orEmpty(),
                modifier = Modifier.weight(1f)
            )
        }
        Box(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardMetrica(
                titulo = AnnotatedString("Variabilidade da FC"),
                valor = leituras["variabilidade_fc"]?.let { formatarValor(it.valor) },
                unidade = "ms", icone = IconeMetrica.ONDA,
                cor = Cores.Teal, fundoIcone = Cores.Menta,
                historico = historico["variabilidade_fc"].orEmpty(),
                modifier = Modifier.weight(1f)
            )
            CardMetrica(
                titulo = tituloO2,
                valor = leituras["saturacao_oxigenio"]?.let { formatarValor(it.valor) },
                unidade = "%", icone = IconeMetrica.GOTA,
                cor = Cores.Azul, fundoIcone = Cores.AzulFundo,
                historico = historico["saturacao_oxigenio"].orEmpty(),
                modifier = Modifier.weight(1f)
            )
        }

        // Botão de alerta
        Box(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BotaoSocorro(aoClicar = {
                Alertas.disparar(context, "botao")
                alertaEnviado = true
            })
            Txt(
                if (alertaEnviado) "Alerta enviado com a sua localização" else "ou aperte volume + três vezes",
                12, if (alertaEnviado) FontWeight.SemiBold else FontWeight.Normal,
                if (alertaEnviado) Cores.Vermelho else Cores.Suave,
                alinhar = TextAlign.Center
            )
        }
        Box(Modifier.height(18.dp))

        // Pendências (só aparece se faltar alguma permissão)
        if (pendencias.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Cores.AmareloFundo, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Txt("Pendências", 13, FontWeight.SemiBold, Cores.Tinta)
                pendencias.forEach { Txt("• $it", 12, FontWeight.Normal, Cores.Tinta) }
            }
            Box(Modifier.height(12.dp))
        }

        // Rodapé: localização
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x220F5046), spotColor = Color(0x220F5046))
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgeLocalizacao(localizacaoAtiva, 38.dp)
            Box(Modifier.width(12.dp))
            Column {
                Txt(
                    if (localizacaoAtiva) "Localização ativa" else "Localização desligada",
                    14, FontWeight.SemiBold, Cores.Tinta
                )
                Txt(
                    if (localizacaoAtiva) "enviada junto com o alerta" else "o alerta irá sem localização",
                    12, FontWeight.Normal, Cores.Suave
                )
            }
        }
        Box(Modifier.height(20.dp))
    }
}
