package br.edu.hidoctor.saudecoletor

import androidx.health.connect.client.records.*

fun converter(r: Record): List<Medicao> = when (r) {
    is HeartRateRecord -> r.samples.mapIndexed { i, s ->
        Medicao("${r.metadata.id}-$i", "frequencia_cardiaca",
            s.beatsPerMinute.toDouble(), null, "bpm", s.time.toString())
    }
    is RestingHeartRateRecord -> listOf(
        Medicao(r.metadata.id, "frequencia_repouso",
            r.beatsPerMinute.toDouble(), null, "bpm", r.time.toString()))
    is HeartRateVariabilityRmssdRecord -> listOf(
        Medicao(r.metadata.id, "variabilidade_fc",
            r.heartRateVariabilityMillis, null, "ms", r.time.toString()))
    is OxygenSaturationRecord -> listOf(
        Medicao(r.metadata.id, "saturacao_oxigenio",
            r.percentage.value, null, "%", r.time.toString()))
    is RespiratoryRateRecord -> listOf(
        Medicao(r.metadata.id, "frequencia_respiratoria",
            r.rate, null, "rpm", r.time.toString()))
    is BodyTemperatureRecord -> listOf(
        Medicao(r.metadata.id, "temperatura_corporal",
            r.temperature.inCelsius, null, "°C", r.time.toString()))
    is BloodGlucoseRecord -> listOf(
        Medicao(r.metadata.id, "glicemia",
            r.level.inMilligramsPerDeciliter, null, "mg/dL", r.time.toString()))
    is BloodPressureRecord -> listOf(
        Medicao(r.metadata.id, "pressao_arterial",
            r.systolic.inMillimetersOfMercury, r.diastolic.inMillimetersOfMercury,
            "mmHg", r.time.toString()))
    is SkinTemperatureRecord -> r.deltas.mapIndexed { i, d ->
        Medicao("${r.metadata.id}-$i", "temperatura_pele",
            d.delta.inCelsius, null, "°C (variação)", d.time.toString())
    }
    else -> emptyList()
}