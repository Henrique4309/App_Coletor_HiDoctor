package br.edu.hidoctor.saudecoletor

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

private suspend inline fun <reified T : Record> ler(
    client: HealthConnectClient,
    filtro: TimeRangeFilter
): List<T> = client.readRecords(ReadRecordsRequest(T::class, filtro)).records

suspend fun lerMedicoes(client: HealthConnectClient, horas: Long = 25): List<Medicao> {
    val fim = Instant.now()
    val filtro = TimeRangeFilter.between(fim.minus(horas, ChronoUnit.HOURS), fim)
    val saida = mutableListOf<Medicao>()

    ler<HeartRateRecord>(client, filtro).forEach { r ->
        r.samples.forEachIndexed { i, s ->
            saida += Medicao("${r.metadata.id}-$i", "frequencia_cardiaca",
                s.beatsPerMinute.toDouble(), null, "bpm", s.time.toString())
        }
    }
    ler<RestingHeartRateRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "frequencia_repouso",
            r.beatsPerMinute.toDouble(), null, "bpm", r.time.toString())
    }
    ler<HeartRateVariabilityRmssdRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "variabilidade_fc",
            r.heartRateVariabilityMillis, null, "ms", r.time.toString())
    }
    ler<OxygenSaturationRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "saturacao_oxigenio",
            r.percentage.value, null, "%", r.time.toString())
    }
    ler<RespiratoryRateRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "frequencia_respiratoria",
            r.rate, null, "rpm", r.time.toString())
    }
    ler<BodyTemperatureRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "temperatura_corporal",
            r.temperature.inCelsius, null, "°C", r.time.toString())
    }
    ler<BloodGlucoseRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "glicemia",
            r.level.inMilligramsPerDeciliter, null, "mg/dL", r.time.toString())
    }
    ler<BloodPressureRecord>(client, filtro).forEach { r ->
        saida += Medicao(r.metadata.id, "pressao_arterial",
            r.systolic.inMillimetersOfMercury, r.diastolic.inMillimetersOfMercury,
            "mmHg", r.time.toString())
    }
    ler<SkinTemperatureRecord>(client, filtro).forEach { r ->
        r.deltas.forEachIndexed { i, d ->
            saida += Medicao("${r.metadata.id}-$i", "temperatura_pele",
                d.delta.inCelsius, null, "°C (variação)", d.time.toString())
        }
    }
    return saida
}

/** Envia uma a uma (o JSON Server não aceita lote). Retorna um resumo em texto. */
suspend fun enviarMedicoes(lista: List<Medicao>): String {
    var novos = 0
    var duplicados = 0
    var falhas = 0
    for (m in lista) {
        try {
            val r = Api.service.enviarMedicao(m)
            when {
                r.isSuccessful -> novos++
                r.code() == 500 -> duplicados++   // JSON Server 0.17 responde 500 para id repetido
                else -> falhas++
            }
        } catch (e: IOException) {
            return "Sem conexão com o servidor (${e.message}). Enviados antes da falha: $novos."
        }
    }
    return "Lidos: ${lista.size} | novos: $novos | já existentes: $duplicados | falhas: $falhas"
}

suspend fun enviarSemPerder(lista: List<Medicao>): Boolean {
    for (m in lista) {
        try {
            val r = Api.service.enviarMedicao(m)
            // 500 = id repetido no JSON Server 0.17 (já estava lá), conta como entregue
            if (!r.isSuccessful && r.code() != 500) return false
        } catch (e: IOException) {
            return false
        }
    }
    return true
}