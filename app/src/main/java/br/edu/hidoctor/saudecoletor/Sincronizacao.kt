package br.edu.hidoctor.saudecoletor

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToLong

/** Estado compartilhado entre o serviço e a tela. */
object Estado {
    val sincronizacao = MutableStateFlow("Sincronização parada")
    val rodando = MutableStateFlow(false)
    val leituras = MutableStateFlow<Map<String, Leitura>>(emptyMap())
    val historico = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val ultimoEnvio = MutableStateFlow<Long?>(null)
    val servidorOk = MutableStateFlow(true)
    val emergencias = MutableStateFlow<Set<Emergencia>>(emptySet())
}

/** Liga ou desliga uma simulação. Parada e arritmia não coexistem; hipo e hiperglicemia também não. */
fun alternarEmergencia(e: Emergencia) {
    val atual = Estado.emergencias.value.toMutableSet()
    if (!atual.remove(e)) {
        atual.add(e)
        when (e) {
            Emergencia.PARADA -> atual.remove(Emergencia.ARRITMIA)
            Emergencia.ARRITMIA -> atual.remove(Emergencia.PARADA)
            Emergencia.HIPOGLICEMIA -> atual.remove(Emergencia.HIPERGLICEMIA)
            Emergencia.HIPERGLICEMIA -> atual.remove(Emergencia.HIPOGLICEMIA)
        }
    }
    Estado.emergencias.value = atual
}

/** Número inteiro quando for inteiro; senão, no máximo duas casas decimais. */
private fun numero(v: Double): Number =
    if (v % 1.0 == 0.0) v.toLong() else (v * 100.0).roundToLong() / 100.0

/** Publica as leituras para a tela (valores e minigráficos). */
fun publicarLeituras(leituras: List<Leitura>) {
    Estado.leituras.value = leituras.associateBy { it.tipo }
    val h = Estado.historico.value.toMutableMap()
    for (l in leituras) h[l.tipo] = (h[l.tipo].orEmpty() + l.valor).takeLast(15)
    Estado.historico.value = h
}

fun montarMedicoes(usuario: String, leituras: List<Leitura>): List<Medicao> {
    val agora = Instant.now().toString()
    return leituras.map { l ->
        Medicao(
            id = UUID.randomUUID().toString(),
            usuario = usuario,
            tipo = l.tipo,
            valor = numero(l.valor),
            valor2 = l.valor2?.let { numero(it) },
            unidade = l.unidade,
            dataHora = l.momento.toString(),
            enviadoEm = agora
        )
    }
}

/** Envia uma a uma. Devolve true só se o servidor aceitou todas. */
suspend fun enviarMedicoes(lista: List<Medicao>): Boolean {
    for (m in lista) {
        try {
            if (!Api.service.enviarMedicao(m).isSuccessful) return false
        } catch (e: IOException) {
            return false
        }
    }
    return true
}
