package br.edu.hidoctor.saudecoletor

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

const val SEM_DADOS = "semDados"
const val JANELA_DIAS = 30L   // quão para trás procurar a última leitura existente

/** Texto de status mostrado na tela e na notificação. */
object Estado {
    val sincronizacao = MutableStateFlow("Aguardando...")
    val leituras = MutableStateFlow<Map<String, Leitura>>(emptyMap())
    val historico = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val ultimoEnvio = MutableStateFlow<Long?>(null)
    val servidorOk = MutableStateFlow(true)
}

fun guardarSeMaisRecente(mapa: MutableMap<String, Leitura>, nova: Leitura) {
    val atual = mapa[nova.tipo]
    if (atual == null || nova.momento.isAfter(atual.momento)) mapa[nova.tipo] = nova
}

/** Até 2 casas decimais; se o valor for inteiro, vai como inteiro (80, e não 80.0). */
fun arredondar(v: Double): Number {
    val r = Math.round(v * 100.0) / 100.0
    return if (r == Math.rint(r)) r.toLong() else r
}

/** Lê, para cada tipo ATIVO, a leitura mais recente que já existe no Health Connect. */
suspend fun lerUltimas(client: HealthConnectClient): Map<String, Leitura> {
    val fim = Instant.now()
    val filtro = TimeRangeFilter.between(fim.minus(JANELA_DIAS, ChronoUnit.DAYS), fim)
    val saida = mutableMapOf<String, Leitura>()
    for (tipo in TIPOS) {
        val registros = client.readRecords(
            ReadRecordsRequest(tipo, filtro, ascendingOrder = false, pageSize = 20)
        ).records
        registros.flatMap { converter(it) }.forEach { guardarSeMaisRecente(saida, it) }
    }
    return saida
}

/** Uma medição por tipo ativo: a última leitura conhecida ou "semDados". */
fun montarMedicoes(usuario: String, ultimas: Map<String, Leitura>): List<Medicao> {
    val agora = Instant.now().toString()
    return TIPOS.map { tipo ->
        val info = INFO_TIPOS.getValue(tipo)
        val l = ultimas[info.nome]
        Medicao(
            id = UUID.randomUUID().toString(),
            usuario = usuario,
            tipo = info.nome,
            valor = l?.valor?.let { arredondar(it) } ?: SEM_DADOS,
            valor2 = l?.valor2?.let { arredondar(it) },
            unidade = info.unidade,
            dataHora = l?.momento?.toString(),
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

/** Publica as últimas leituras para a tela (valores e minigráficos). */
fun publicarLeituras(ultimas: Map<String, Leitura>) {
    Estado.leituras.value = ultimas.toMap()
    val h = Estado.historico.value.toMutableMap()
    for ((tipo, l) in ultimas) h[tipo] = (h[tipo].orEmpty() + l.valor).takeLast(15)
    Estado.historico.value = h
}