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
                "Este app lê do Health Connect os seguintes dados: glicemia, pressão arterial, " +
                        "temperatura corporal, frequência cardíaca, variabilidade da frequência cardíaca, " +
                        "saturação de oxigênio, frequência respiratória, frequência cardíaca em repouso " +
                        "e temperatura da pele. Os dados são enviados à API do projeto, apenas para " +
                        "gerar relatórios acadêmicos, e não são compartilhados com terceiros.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}