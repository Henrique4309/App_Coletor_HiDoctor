package br.edu.hidoctor.saudecoletor

import android.content.Context
import com.google.gson.Gson
import java.io.File

/** Alerta que ainda não foi totalmente confirmado pelo servidor. Fica salvo em arquivo. */
data class AlertaPendente(
    val id: String,
    val usuario: String,
    val tipo: String,
    val origem: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var localizacaoEm: String? = null,
    var inicialOk: Boolean = false,        // 1º envio (última localização) confirmado
    var confirmadoEm: String? = null,      // horário em que o 1º envio foi confirmado
    var precisaPronta: Boolean = false,    // a busca da localização precisa terminou
    var precisaObtida: Boolean = false,    // ...e encontrou uma posição
    val prazoPrecisaMs: Long = 0L,         // depois disso, o 2º envio sai mesmo sem posição precisa
    var avisouFalha: Boolean = false
)

private class DadosFila {
    var medicoes: MutableList<Medicao> = mutableListOf()
    var alertas: MutableList<AlertaPendente> = mutableListOf()
}

/**
 * Fila de medições (até 100 por tipo) e de alertas pendentes, salva em arquivo
 * para sobreviver se o app for fechado.
 */
object Fila {
    const val LIMITE_POR_TIPO = 100

    private val gson = Gson()
    private var arquivo: File? = null
    private var dados = DadosFila()

    @Synchronized
    fun iniciar(context: Context) {
        if (arquivo != null) return
        val f = File(context.applicationContext.filesDir, "fila.json")
        arquivo = f
        if (f.exists()) {
            try {
                dados = gson.fromJson(f.readText(), DadosFila::class.java) ?: DadosFila()
                normalizar()
            } catch (e: Exception) {
                dados = DadosFila()
            }
        }
        Estado.filaTamanho.value = dados.medicoes.size
    }

    // O Gson lê números como Double; devolve a forma inteira (72.0 -> 72) antes de reenviar
    private fun inteiroSePossivel(v: Any): Any =
        if (v is Double && v % 1.0 == 0.0) v.toLong() else v

    private fun normalizar() {
        if (dados.medicoes == null) dados.medicoes = mutableListOf()
        if (dados.alertas == null) dados.alertas = mutableListOf()
        dados.medicoes = dados.medicoes.map { m ->
            m.copy(valor = inteiroSePossivel(m.valor), valor2 = m.valor2?.let { inteiroSePossivel(it) })
        }.toMutableList()
    }

    private fun salvar() {
        val f = arquivo ?: return
        try {
            val temporario = File(f.path + ".tmp")
            temporario.writeText(gson.toJson(dados))
            temporario.renameTo(f)
        } catch (e: Exception) {
            // sem espaço ou sem acesso: a fila continua valendo em memória
        }
    }

    // ───────── Medições ─────────

    /** Entra no fim da fila. Se o tipo já tem 100, o mais antigo desse tipo sai. */
    @Synchronized
    fun adicionarMedicao(m: Medicao) {
        if (dados.medicoes.count { it.tipo == m.tipo } >= LIMITE_POR_TIPO) {
            val i = dados.medicoes.indexOfFirst { it.tipo == m.tipo }
            if (i >= 0) dados.medicoes.removeAt(i)
        }
        dados.medicoes.add(m)
        Estado.filaTamanho.value = dados.medicoes.size
        salvar()
    }

    @Synchronized
    fun primeiraMedicao(): Medicao? = dados.medicoes.firstOrNull()

    /** Chamado só depois que o servidor confirmou o recebimento. */
    @Synchronized
    fun removerMedicao(id: String) {
        dados.medicoes.removeAll { it.id == id }
        Estado.filaTamanho.value = dados.medicoes.size
        salvar()
    }

    // ───────── Alertas ─────────

    @Synchronized
    fun adicionarAlerta(a: AlertaPendente) {
        dados.alertas.add(a)
        salvar()
    }

    @Synchronized
    fun alertas(): List<AlertaPendente> = dados.alertas.map { it.copy() }

    @Synchronized
    fun alerta(id: String): AlertaPendente? = dados.alertas.firstOrNull { it.id == id }?.copy()

    @Synchronized
    fun atualizarAlerta(id: String, alteracao: (AlertaPendente) -> Unit) {
        dados.alertas.firstOrNull { it.id == id }?.let {
            alteracao(it)
            salvar()
        }
    }

    @Synchronized
    fun removerAlerta(id: String) {
        dados.alertas.removeAll { it.id == id }
        salvar()
    }
}
