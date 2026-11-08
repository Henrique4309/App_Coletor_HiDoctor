package br.edu.hidoctor.saudecoletor

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object Config {
    private val REGEX = Regex("^[A-Za-z0-9]+$")

    private fun prefs(context: Context) =
        context.getSharedPreferences("config", Context.MODE_PRIVATE)

    fun valido(nome: String) = REGEX.matches(nome)

    fun usuario(context: Context): String? = prefs(context).getString("usuario", null)

    fun salvarUsuario(context: Context, nome: String) {
        prefs(context).edit().putString("usuario", nome).apply()
    }

    fun jaFez(context: Context, chave: String) = prefs(context).getBoolean(chave, false)

    fun marcarFeito(context: Context, chave: String) {
        prefs(context).edit().putBoolean(chave, true).apply()
    }
}

fun concedida(context: Context, permissao: String): Boolean =
    ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED

fun acessibilidadeAtiva(context: Context): Boolean {
    val ativos = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val alvo = ComponentName(context, ServicoAlertaVolume::class.java)
    return ativos.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == alvo }
}