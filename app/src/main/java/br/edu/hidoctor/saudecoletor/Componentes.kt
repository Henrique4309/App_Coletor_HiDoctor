package br.edu.hidoctor.saudecoletor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

/** Símbolo do hidoctor: círculo verde, anel fino, batimento e cruz. Desenhado em código, sem imagem. */
@Composable
fun LogoHidoctor(tamanho: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tamanho)) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(Cores.Teal, radius = r, center = c)
        drawCircle(
            Color(0xFFC8F0E8), radius = r * 0.842f, center = c,
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
        val cruz = Color(0xFFBEECE2)
        val larg = r * 0.096f
        drawLine(cruz, Offset(cx - meio, cy), Offset(cx + meio, cy), strokeWidth = larg, cap = StrokeCap.Round)
        drawLine(cruz, Offset(cx, cy - meio), Offset(cx, cy + meio), strokeWidth = larg, cap = StrokeCap.Round)
    }
}

// ───────────────────────── Ícones das métricas ─────────────────────────

enum class IconeMetrica { CORACAO, LUA, ONDA, GOTA }

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

/** Círculo colorido com o ícone da métrica dentro (coração, lua, onda ou gota). */
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
    cor: Color,
    fundoIcone: Color,
    historico: List<Double>,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .shadow(8.dp, forma, ambientColor = Color(0x330F5046), spotColor = Color(0x330F5046))
            .background(Color.White, forma)
            .padding(14.dp)
    ) {
        BadgeMetrica(icone, cor, fundoIcone, 28.dp)
        Box(Modifier.height(8.dp))
        Txt(titulo, 11, FontWeight.Medium, Cores.Suave, maxLinhas = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(valor ?: "--", 32, FontWeight.Bold, if (valor == null) Cores.Menta2 else Cores.Tinta)
            Box(Modifier.width(5.dp))
            Txt(unidade, 13, FontWeight.SemiBold, cor, Modifier.padding(bottom = 6.dp))
        }
        Box(Modifier.height(4.dp))
        MiniGrafico(historico, cor, Modifier.fillMaxWidth().height(20.dp))
    }
}

// ───────────────────────── Botão Pedir Socorro ─────────────────────────

@Composable
fun BotaoSocorro(aoClicar: () -> Unit) {
    val transicao = rememberInfiniteTransition(label = "pulso")
    val escala by transicao.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )
    Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(167.dp).scale(escala)
                .background(Cores.Vermelho.copy(alpha = 0.10f), CircleShape)
        )
        Box(Modifier.size(143.dp).background(Cores.Vermelho.copy(alpha = 0.18f), CircleShape))
        Box(Modifier.size(120.dp).offset(y = 3.dp).background(Cores.VermelhoEscuro, CircleShape))
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(14.dp, CircleShape, ambientColor = Color(0x66E23636), spotColor = Color(0x66E23636))
                .clip(CircleShape)
                .background(Cores.Vermelho)
                .clickable(onClick = aoClicar),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconeAlerta(Modifier.size(30.dp))
                Box(Modifier.height(4.dp))
                Txt("PEDIR", 18, FontWeight.Bold, Color.White)
                Txt("SOCORRO", 18, FontWeight.Bold, Color.White)
            }
        }
    }
}

/** Triângulo de alerta branco com exclamação vermelha. */
@Composable
fun IconeAlerta(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.width / 100f
        val y0 = (size.height - 84 * u) / 2f
        val tri = Path().apply {
            moveTo(50 * u, y0)
            lineTo(100 * u, y0 + 84 * u)
            lineTo(0f, y0 + 84 * u)
            close()
        }
        drawPath(tri, Color.White)
        drawPath(tri, Color.White, style = Stroke(width = 8 * u, join = StrokeJoin.Round))
        drawRoundRect(
            Cores.Vermelho, topLeft = Offset(45 * u, y0 + 28 * u),
            size = Size(10 * u, 30 * u), cornerRadius = CornerRadius(4 * u)
        )
        drawCircle(Cores.Vermelho, radius = 6 * u, center = Offset(50 * u, y0 + 70 * u))
    }
}

// ───────────────────────── Ícones pequenos ─────────────────────────

/** Pino de localização dentro de um círculo menta. */
@Composable
fun BadgeLocalizacao(ativa: Boolean, tamanho: Dp = 38.dp) {
    val cor = if (ativa) Cores.Teal else Cores.Amarelo
    val fundo = if (ativa) Cores.Menta else Cores.AmareloFundo
    Canvas(Modifier.size(tamanho)) {
        val u = size.width / 56f
        drawCircle(fundo)
        val ponta = Path().apply {
            moveTo(28 * u, 51 * u)
            lineTo(15 * u, 27 * u)
            lineTo(41 * u, 27 * u)
            close()
        }
        drawPath(ponta, cor)
        drawCircle(cor, radius = 14 * u, center = Offset(28 * u, 21 * u))
        drawCircle(fundo, radius = 5 * u, center = Offset(28 * u, 21 * u))
    }
}

/** Círculo verde com um "check" branco. */
@Composable
fun IconeCheck(tamanho: Dp = 24.dp) {
    Canvas(Modifier.size(tamanho)) {
        val s = size.width
        drawCircle(Cores.Teal)
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

/** Indicador de etapas (barrinhas). */
@Composable
fun IndicadorEtapas(total: Int, atual: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (i == atual) 22.dp else 6.dp)
                    .background(if (i == atual) Cores.Teal else Cores.Menta2, CircleShape)
            )
        }
    }
}
