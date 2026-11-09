package br.edu.hidoctor.saudecoletor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Paleta em tons de cinza (versão de simulação). */
object Cores {
    val Escuro = Color(0xFF555B61)       // cor principal (no lugar do verde)
    val MaisEscuro = Color(0xFF383C40)
    val Claro = Color(0xFFEDEEEF)
    val Borda = Color(0xFFD8DADC)
    val Fundo = Color(0xFFF7F7F8)
    val Tinta = Color(0xFF25282B)
    val Suave = Color(0xFF747A80)
    val Sombra = Color(0x33000000)
    val SombraLeve = Color(0x22000000)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

@Composable
fun TemaHidoctor(conteudo: @Composable () -> Unit) {
    val esquema = lightColorScheme(
        primary = Cores.Escuro,
        onPrimary = Color.White,
        background = Cores.Fundo,
        onBackground = Cores.Tinta,
        surface = Color.White,
        onSurface = Cores.Tinta,
        error = Cores.MaisEscuro
    )
    MaterialTheme(colorScheme = esquema, content = conteudo)
}

/** Texto com a fonte do app. Tamanho em sp. */
@Composable
fun Txt(
    texto: AnnotatedString,
    tam: Int,
    peso: FontWeight = FontWeight.Normal,
    cor: Color = Cores.Tinta,
    modifier: Modifier = Modifier,
    alinhar: TextAlign? = null,
    maxLinhas: Int = Int.MAX_VALUE
) {
    Text(
        text = texto,
        color = cor,
        fontSize = tam.sp,
        fontWeight = peso,
        fontFamily = Poppins,
        lineHeight = (tam * 1.3f).sp,
        textAlign = alinhar,
        maxLines = maxLinhas,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun Txt(
    texto: String,
    tam: Int,
    peso: FontWeight = FontWeight.Normal,
    cor: Color = Cores.Tinta,
    modifier: Modifier = Modifier,
    alinhar: TextAlign? = null,
    maxLinhas: Int = Int.MAX_VALUE
) = Txt(AnnotatedString(texto), tam, peso, cor, modifier, alinhar, maxLinhas)
