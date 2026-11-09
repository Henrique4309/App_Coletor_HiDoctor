package br.edu.hidoctor.saudecoletor

import android.content.Context

object Config {
    private val REGEX = Regex("^[A-Za-z0-9]+$")

    private fun prefs(context: Context) =
        context.getSharedPreferences("config", Context.MODE_PRIVATE)

    fun valido(nome: String) = REGEX.matches(nome)

    fun usuario(context: Context): String? = prefs(context).getString("usuario", null)

    fun salvarUsuario(context: Context, nome: String) {
        prefs(context).edit().putString("usuario", nome).apply()
    }
}
