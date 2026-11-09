package br.edu.hidoctor.saudecoletor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ───────────────────────── Logo ─────────────────────────

/** Símbolo do hidoctor em tons de cinza. Desenhado em código, sem imagem. */
@Composable
fun LogoHidoctor(tamanho: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tamanho)) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(Cores.Escuro, radius = r, center = c)
        drawCircle(
            Color(0xFFD5D8DB), radius = r * 0.842f, center = c,
            style = Stroke(width = maxOf(r * 0.025f, 1f))
        )
        val pontos = listOf(
            -0.752f to 0.055f, -0.305f to 0.055f, -0.129f to -0.389f,
            0.113f to 0.505f, 0.305f to 0.055f, 0.752f to 0.055f
        )
        val pulso = Path()
        pontos.forEachIndexed { i, (x, y) ->
            val px = c.x + x * r
            val py = c.y + y * r
            if (i == 0) pulso.moveTo(px, py) else pulso.lineTo(px, py)
        }
        drawPath(
            pulso, Color.White,
            style = Stroke(width = r * 0.084f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val cx = c.x + 0.531f * r
        val cy = c.y - 0.695f * r
        val meio = 0.09f * r
        val cruz = Color(0xFFC4C8CC)
        val larg = r * 0.096f
        drawLine(cruz, Offset(cx - meio, cy), Offset(cx + meio, cy), strokeWidth = larg, cap = StrokeCap.Round)
        drawLine(cruz, Offset(cx, cy - meio), Offset(cx, cy + meio), strokeWidth = larg, cap = StrokeCap.Round)
    }
}

// ───────────────────────── Ícones das métricas ─────────────────────────

enum class IconeMetrica { CORACAO, LUA, ONDA, GOTA, RESPIRACAO, TEMPERATURA, PRESSAO, GLICEMIA }

private fun caixa(cx: Float, cy: Float, w: Float, h: Float) =
    Rect(Offset(cx - w / 2f, cy - h / 2f), Size(w, h))

/** Ajusta uma lista de pontos para caber dentro da caixa, mantendo a proporção. */
private fun caminhoNaCaixa(pontos: List<Offset>, caixa: Rect): Path {
    val minX = pontos.minOf { it.x }
    val maxX = pontos.maxOf { it.x }
    val minY = pontos.minOf { it.y }
    val maxY = pontos.maxOf { it.y }
    val esc = minOf(caixa.width / (maxX - minX), caixa.height / (maxY - minY))
    val mx = (minX + maxX) / 2f
    val my = (minY + maxY) / 2f
    val path = Path()
    pontos.forEachIndexed { i, p ->
        val x = caixa.center.x + (p.x - mx) * esc
        val y = caixa.center.y + (p.y - my) * esc
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private val PONTOS_CORACAO: List<Offset> = (0 until 360 step 4).map {
    val a = it * PI / 180.0
    val x = 16.0 * sin(a) * sin(a) * sin(a)
    val y = -(13.0 * cos(a) - 5.0 * cos(2 * a) - 2.0 * cos(3 * a) - cos(4 * a))
    Offset(x.toFloat(), y.toFloat())
}

private val PONTOS_GOTA: List<Offset> = (0 until 360 step 4).map {
    val a = it * PI / 180.0
    val x = sin(a) * (1.0 - cos(a)) * 0.55
    val y = -cos(a)
    Offset(x.toFloat(), y.toFloat())
}

/** Círculo com o ícone da métrica dentro. */
@Composable
fun BadgeMetrica(tipo: IconeMetrica, cor: Color, fundo: Color, tamanho: Dp = 26.dp) {
    Canvas(Modifier.size(tamanho)) {
        val u = size.width / 76f
        drawCircle(fundo)
        when (tipo) {
            IconeMetrica.CORACAO ->
                drawPath(caminhoNaCaixa(PONTOS_CORACAO, caixa(38 * u, 40 * u, 38 * u, 34 * u)), cor)
            IconeMetrica.LUA -> {
                drawCircle(cor, radius = 19 * u, center = Offset(38 * u, 38 * u))
                drawCircle(fundo, radius = 17 * u, center = Offset(46 * u, 30 * u))
            }
            IconeMetrica.ONDA -> {
                val pts = listOf(
                    16f to 40f, 29f to 40f, 35f to 24f, 43f to 54f, 48f to 40f, 60f to 40f
                )
                val p = Path()
                pts.forEachIndexed { i, (x, y) ->
                    if (i == 0) p.moveTo(x * u, y * u) else p.lineTo(x * u, y * u)
                }
                drawPath(
                    p, cor,
                    style = Stroke(width = 5 * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            IconeMetrica.GOTA ->
                drawPath(caminhoNaCaixa(PONTOS_GOTA, caixa(38 * u, 40 * u, 32 * u, 42 * u)), cor)
            IconeMetrica.RESPIRACAO -> {
                drawOval(cor, topLeft = Offset(19 * u, 30 * u), size = Size(16 * u, 28 * u))
                drawOval(cor, topLeft = Offset(41 * u, 30 * u), size = Size(16 * u, 28 * u))
                drawLine(cor, Offset(38 * u, 18 * u), Offset(38 * u, 38 * u), 4 * u, StrokeCap.Round)
                drawLine(cor, Offset(38 * u, 38 * u), Offset(29 * u, 33 * u), 4 * u, StrokeCap.Round)
                drawLine(cor, Offset(38 * u, 38 * u), Offset(47 * u, 33 * u), 4 * u, StrokeCap.Round)
            }
            IconeMetrica.TEMPERATURA -> {
                drawRoundRect(
                    cor, topLeft = Offset(33 * u, 14 * u), size = Size(10 * u, 38 * u),
                    cornerRadius = CornerRadius(5 * u)
                )
                drawCircle(cor, radius = 9 * u, center = Offset(38 * u, 54 * u))
                drawRoundRect(
                    fundo, topLeft = Offset(36.2f * u, 20 * u), size = Size(3.6f * u, 30 * u),
                    cornerRadius = CornerRadius(1.8f * u)
                )
                drawRoundRect(
                    cor, topLeft = Offset(36.2f * u, 34 * u), size = Size(3.6f * u, 20 * u),
                    cornerRadius = CornerRadius(1.8f * u)
                )
                drawLine(cor, Offset(48 * u, 22 * u), Offset(54 * u, 22 * u), 3 * u, StrokeCap.Round)
                drawLine(cor, Offset(48 * u, 30 * u), Offset(54 * u, 30 * u), 3 * u, StrokeCap.Round)
                drawLine(cor, Offset(48 * u, 38 * u), Offset(54 * u, 38 * u), 3 * u, StrokeCap.Round)
            }
            IconeMetrica.PRESSAO -> {
                drawArc(
                    cor, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(16 * u, 26 * u), size = Size(44 * u, 44 * u),
                    style = Stroke(width = 5 * u, cap = StrokeCap.Round)
                )
                drawLine(cor, Offset(38 * u, 48 * u), Offset(48 * u, 35 * u), 4 * u, StrokeCap.Round)
                drawCircle(cor, radius = 4.5f * u, center = Offset(38 * u, 48 * u))
            }
            IconeMetrica.GLICEMIA -> {
                drawPath(
                    caminhoNaCaixa(PONTOS_GOTA, caixa(38 * u, 40 * u, 30 * u, 40 * u)), cor,
                    style = Stroke(width = 4 * u, join = StrokeJoin.Round)
                )
                drawCircle(cor, radius = 4.5f * u, center = Offset(38 * u, 46 * u))
            }
        }
    }
}

/** Minigráfico com as últimas leituras. Sem variação, mostra uma linha reta. */
@Composable
fun MiniGrafico(valores: List<Double>, cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (size.width <= 0f) return@Canvas
        val lista = if (valores.size >= 2) valores else listOf(0.0, 0.0)
        val min = lista.minOrNull() ?: 0.0
        val max = lista.maxOrNull() ?: 0.0
        val faixa = max - min
        val folga = 3.dp.toPx()
        val alto = size.height - 2 * folga
        val path = Path()
        lista.forEachIndexed { i, v ->
            val x = size.width * i / (lista.size - 1)
            val y = if (faixa == 0.0) size.height / 2f
            else (folga + alto * (1.0 - (v - min) / faixa)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path, cor.copy(alpha = 0.45f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ───────────────────────── Card de métrica ─────────────────────────

@Composable
fun CardMetrica(
    titulo: AnnotatedString,
    valor: String?,
    unidade: String,
    icone: IconeMetrica,
    historico: List<Double>,
    modifier: Modifier = Modifier,
    tamValor: Int = 30,
    tamUnidade: Int = 13
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .shadow(6.dp, forma, ambientColor = Cores.SombraLeve, spotColor = Cores.SombraLeve)
            .background(Color.White, forma)
            .padding(12.dp)
    ) {
        BadgeMetrica(icone, Cores.Escuro, Cores.Claro, 28.dp)
        Box(Modifier.height(6.dp))
        Txt(titulo, 11, FontWeight.Medium, Cores.Suave, maxLinhas = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(valor ?: "--", tamValor, FontWeight.Bold, if (valor == null) Cores.Borda else Cores.Tinta, maxLinhas = 1)
            Box(Modifier.width(5.dp))
            Txt(unidade, tamUnidade, FontWeight.SemiBold, Cores.Escuro, Modifier.padding(bottom = 5.dp), maxLinhas = 1)
        }
        Box(Modifier.height(4.dp))
        MiniGrafico(historico, Cores.Escuro, Modifier.fillMaxWidth().height(18.dp))
    }
}

// ───────────────────────── Botões ─────────────────────────

/** Botão grande de iniciar/parar a sincronização. */
@Composable
fun BotaoSincronizacao(rodando: Boolean, aoClicar: () -> Unit) {
    val forma = RoundedCornerShape(27.dp)
    val modificador = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .clip(forma)
        .let {
            if (rodando) it.background(Color.White).border(1.5.dp, Cores.Escuro, forma)
            else it.background(Cores.Escuro)
        }
        .clickable(onClick = aoClicar)
    Box(modificador, contentAlignment = Alignment.Center) {
        Txt(
            if (rodando) "Parar sincronização" else "Iniciar sincronização",
            16, FontWeight.SemiBold, if (rodando) Cores.Escuro else Color.White
        )
    }
}

/** Linha-botão de uma simulação: inicia ou para ao tocar. */
@Composable
fun BotaoSimulacao(titulo: String, descricao: String, ativo: Boolean, aoClicar: () -> Unit) {
    val forma = RoundedCornerShape(14.dp)
    val textoPrincipal = if (ativo) Color.White else Cores.Tinta
    val textoSecundario = if (ativo) Color(0xFFD5D8DB) else Cores.Suave
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, forma, ambientColor = Cores.SombraLeve, spotColor = Cores.SombraLeve)
            .clip(forma)
            .background(if (ativo) Cores.Escuro else Color.White)
            .clickable(onClick = aoClicar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(titulo, 14, FontWeight.SemiBold, textoPrincipal)
            Txt(descricao, 11, FontWeight.Normal, textoSecundario)
        }
        Box(Modifier.width(10.dp))
        Box(
            Modifier
                .background(if (ativo) Color.White else Cores.Claro, CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Txt(if (ativo) "Parar" else "Iniciar", 12, FontWeight.SemiBold, Cores.MaisEscuro)
        }
    }
}

/** Botão discreto, de contorno fino. */
@Composable
fun BotaoDiscreto(texto: String, aoClicar: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(forma)
            .border(1.dp, Cores.Borda, forma)
            .clickable(onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        Txt(texto, 13, FontWeight.Medium, Cores.Suave)
    }
}

// ───────────────────────── Ícones pequenos ─────────────────────────

/** Pino de localização dentro de um círculo claro. */
@Composable
fun BadgeLocalizacao(tamanho: Dp = 38.dp) {
    Canvas(Modifier.size(tamanho)) {
        val u = size.width / 56f
        drawCircle(Cores.Claro)
        val ponta = Path().apply {
            moveTo(28 * u, 51 * u)
            lineTo(15 * u, 27 * u)
            lineTo(41 * u, 27 * u)
            close()
        }
        drawPath(ponta, Cores.Escuro)
        drawCircle(Cores.Escuro, radius = 14 * u, center = Offset(28 * u, 21 * u))
        drawCircle(Cores.Claro, radius = 5 * u, center = Offset(28 * u, 21 * u))
    }
}

/** Círculo cinza-escuro com um "check" branco. */
@Composable
fun IconeCheck(tamanho: Dp = 24.dp) {
    Canvas(Modifier.size(tamanho)) {
        val s = size.width
        drawCircle(Cores.Escuro)
        val p = Path().apply {
            moveTo(0.28f * s, 0.52f * s)
            lineTo(0.44f * s, 0.67f * s)
            lineTo(0.73f * s, 0.35f * s)
        }
        drawPath(
            p, Color.White,
            style = Stroke(width = 0.1f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Ícone "i" de informação. */
@Composable
fun IconeInfo(tamanho: Dp = 18.dp, cor: Color = Cores.Suave) {
    Canvas(Modifier.size(tamanho)) {
        val s = size.width
        drawCircle(cor, radius = s * 0.46f, style = Stroke(width = s * 0.08f))
        drawCircle(cor, radius = s * 0.055f, center = Offset(s / 2f, s * 0.32f))
        drawRoundRect(
            cor, topLeft = Offset(s * 0.455f, s * 0.44f),
            size = Size(s * 0.09f, s * 0.30f), cornerRadius = CornerRadius(s * 0.03f)
        )
    }
}

/** Coração simples, usado como detalhe decorativo. */
@Composable
fun CoracaoDecorativo(tamanho: Dp, cor: Color) {
    Canvas(Modifier.size(tamanho)) {
        drawPath(
            caminhoNaCaixa(PONTOS_CORACAO, caixa(size.width / 2f, size.height / 2f, size.width, size.height * 0.9f)),
            cor
        )
    }
}
