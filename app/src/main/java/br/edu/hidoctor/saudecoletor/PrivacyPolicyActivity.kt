package br.edu.seugrupo.saudecoletor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text(
                "Este app lê do Health Connect a frequência cardíaca, a frequência cardíaca em " +
                        "repouso, a variabilidade da frequência cardíaca e a saturação de oxigênio, e os " +
                        "envia, junto do seu nome de usuário, à API do projeto a cada poucos segundos. " +
                        "Quando você aciona o alerta (3 toques no botão de volume), envia também a sua " +
                        "localização. Os dados são usados apenas para fins acadêmicos e não são " +
                        "compartilhados com terceiros.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}