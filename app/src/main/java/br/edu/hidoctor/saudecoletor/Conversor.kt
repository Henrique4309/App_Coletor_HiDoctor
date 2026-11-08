package br.edu.hidoctor.saudecoletor

import androidx.health.connect.client.records.*
import java.time.Instant
import kotlin.reflect.KClass

class InfoTipo(val nome: String, val unidade: String)

val INFO_TIPOS: Map<KClass<out Record>, InfoTipo> = mapOf(
    HeartRateRecord::class to InfoTipo("frequencia_cardiaca", "bpm"),
    RestingHeartRateRecord::class to InfoTipo("frequencia_repouso", "bpm"),
    HeartRateVariabilityRmssdRecord::class to InfoTipo("variabilidade_fc", "ms"),
    OxygenSaturationRecord::class to InfoTipo("saturacao_oxigenio", "%"),
    // Desativados por enquanto (veja TIPOS_DESATIVADOS)
    RespiratoryRateRecord::class to InfoTipo("frequencia_respiratoria", "rpm"),
    BodyTemperatureRecord::class to InfoTipo("temperatura_corporal", "°C"),
    BloodGlucoseRecord::class to InfoTipo("glicemia", "mg/dL"),
    BloodPressureRecord::class to InfoTipo("pressao_arterial", "mmHg"),
    SkinTemperatureRecord::class to InfoTipo("temperatura_pele", "°C (variação)"),
)

data class Leitura(
    val tipo: String,
    val valor: Double,
    val valor2: Double?,
    val unidade: String,
    val momento: Instant
)

private fun leitura(
    classe: KClass<out Record>, valor: Double, valor2: Double?, momento: Instant
): Leitura {
    val info = INFO_TIPOS.getValue(classe)
    return Leitura(info.nome, valor, valor2, info.unidade, momento)
}

fun converter(r: Record): List<Leitura> = when (r) {
    is HeartRateRecord -> r.samples.map {
        leitura(HeartRateRecord::class, it.beatsPerMinute.toDouble(), null, it.time)
    }
    is RestingHeartRateRecord -> listOf(
        leitura(RestingHeartRateRecord::class, r.beatsPerMinute.toDouble(), null, r.time))
    is HeartRateVariabilityRmssdRecord -> listOf(
        leitura(HeartRateVariabilityRmssdRecord::class, r.heartRateVariabilityMillis, null, r.time))
    is OxygenSaturationRecord -> listOf(
        leitura(OxygenSaturationRecord::class, r.percentage.value, null, r.time))
    is RespiratoryRateRecord -> listOf(
        leitura(RespiratoryRateRecord::class, r.rate, null, r.time))
    is BodyTemperatureRecord -> listOf(
        leitura(BodyTemperatureRecord::class, r.temperature.inCelsius, null, r.time))
    is BloodGlucoseRecord -> listOf(
        leitura(BloodGlucoseRecord::class, r.level.inMilligramsPerDeciliter, null, r.time))
    is BloodPressureRecord -> listOf(
        leitura(BloodPressureRecord::class, r.systolic.inMillimetersOfMercury,
            r.diastolic.inMillimetersOfMercury, r.time))
    is SkinTemperatureRecord -> r.deltas.map {
        leitura(SkinTemperatureRecord::class, it.delta.inCelsius, null, it.time)
    }
    else -> emptyList()
}