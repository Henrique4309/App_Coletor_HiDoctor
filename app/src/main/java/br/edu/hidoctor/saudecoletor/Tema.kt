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

object Cores {
    val Teal = Color(0xFF0F9C89)
    val TealEscuro = Color(0xFF0B786A)
    val Menta = Color(0xFFE2F5F0)
    val Menta2 = Color(0xFFCCECE4)
    val Fundo = Color(0xFFF7FCFA)
    val Tinta = Color(0xFF263240)
    val Suave = Color(0xFF6E7C88)
    val Vermelho = Color(0xFFE23636)
    val VermelhoEscuro = Color(0xFFB22024)
    val Rosa = Color(0xFFDE4650)
    val RosaFundo = Color(0xFFFFE9E9)
    val Amarelo = Color(0xFFD6961E)
    val AmareloFundo = Color(0xFFFFF3D6)
    val Azul = Color(0xFF3C78D2)
    val AzulFundo = Color(0xFFE2EEFC)
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
        primary = Cores.Teal,
        onPrimary = Color.White,
        background = Cores.Fundo,
        onBackground = Cores.Tinta,
        surface = Color.White,
        onSurface = Cores.Tinta,
        error = Cores.Vermelho
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
