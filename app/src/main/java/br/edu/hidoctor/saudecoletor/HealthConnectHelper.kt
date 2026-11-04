package br.edu.hidoctor.saudecoletor

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.*
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.reflect.KClass
import androidx.health.connect.client.time.TimeRangeFilter

val TIPOS: List<KClass<out Record>> = listOf(
    BloodGlucoseRecord::class,
    BloodPressureRecord::class,
    BodyTemperatureRecord::class,
    HeartRateRecord::class,
    HeartRateVariabilityRmssdRecord::class,
    OxygenSaturationRecord::class,
    RespiratoryRateRecord::class,
    RestingHeartRateRecord::class,
    SkinTemperatureRecord::class,
)

val PERMISSOES_LEITURA: Set<String> =
    TIPOS.map { HealthPermission.getReadPermission(it) }.toSet()

val PERMISSOES_ESCRITA: Set<String> =
    TIPOS.map { HealthPermission.getWritePermission(it) }.toSet()

val TODAS_PERMISSOES: Set<String> = PERMISSOES_LEITURA + PERMISSOES_ESCRITA

/** Insere 24 horas de dados fictícios (1 registro por hora). Retorna um resumo. */
suspend fun inserirDadosDeTeste(client: HealthConnectClient): String {
    val agora = Instant.now()
    val zona = ZoneId.systemDefault().rules.getOffset(agora)
    val registros = mutableListOf<Record>()

    for (h in 24 downTo 1) {
        val t = agora.minus(h.toLong(), ChronoUnit.HOURS)

        registros += HeartRateRecord(
            startTime = t, startZoneOffset = zona,
            endTime = t.plusSeconds(60), endZoneOffset = zona,
            samples = listOf(HeartRateRecord.Sample(t, (60..100).random().toLong())),
            metadata = Metadata.manualEntry()
        )
        registros += RestingHeartRateRecord(
            time = t, zoneOffset = zona,
            beatsPerMinute = (52..68).random().toLong(),
            metadata = Metadata.manualEntry()
        )
        registros += HeartRateVariabilityRmssdRecord(
            time = t, zoneOffset = zona,
            heartRateVariabilityMillis = 20.0 + Random.nextDouble() * 60.0,
            metadata = Metadata.manualEntry()
        )
        registros += OxygenSaturationRecord(
            time = t, zoneOffset = zona,
            percentage = Percentage(94.0 + Random.nextDouble() * 5.0),
            metadata = Metadata.manualEntry()
        )
        registros += RespiratoryRateRecord(
            time = t, zoneOffset = zona,
            rate = 12.0 + Random.nextDouble() * 8.0,
            metadata = Metadata.manualEntry()
        )
        registros += BodyTemperatureRecord(
            time = t, zoneOffset = zona,
            temperature = Temperature.celsius(36.0 + Random.nextDouble() * 1.5),
            metadata = Metadata.manualEntry()
        )
        registros += BloodGlucoseRecord(
            time = t, zoneOffset = zona,
            level = BloodGlucose.milligramsPerDeciliter(70.0 + Random.nextDouble() * 60.0),
            metadata = Metadata.manualEntry()
        )
        registros += BloodPressureRecord(
            time = t, zoneOffset = zona,
            systolic = Pressure.millimetersOfMercury(105.0 + Random.nextInt(0, 30)),
            diastolic = Pressure.millimetersOfMercury(65.0 + Random.nextInt(0, 20)),
            metadata = Metadata.manualEntry()
        )
    }

    client.insertRecords(registros)

    // Temperatura da pele é separada: nem todo aparelho suporta.
    val pele = inserirTemperaturaDaPele(client, agora)
    return "${registros.size} registros inseridos. " +
            if (pele) "Temperatura da pele: ok." else "Temperatura da pele: sem suporte neste aparelho."
}

private suspend fun inserirTemperaturaDaPele(client: HealthConnectClient, agora: Instant): Boolean {
    val zona = ZoneId.systemDefault().rules.getOffset(agora)
    val inicio = agora.minus(1, ChronoUnit.HOURS)
    val deltas = (0 until 6).map { i ->
        SkinTemperatureRecord.Delta(
            time = inicio.plus((i * 10).toLong(), ChronoUnit.MINUTES),
            delta = TemperatureDelta.celsius(Random.nextDouble() * 0.6 - 0.3)
        )
    }
    return try {
        client.insertRecords(
            listOf(
                SkinTemperatureRecord(
                    startTime = inicio, startZoneOffset = zona,
                    endTime = agora, endZoneOffset = zona,
                    deltas = deltas,
                    baseline = Temperature.celsius(33.0),
                    metadata = Metadata.manualEntry()
                )
            )
        )
        true
    } catch (e: Exception) {
        false // aparelho sem suporte ou permissão negada
    }
}

/** Apaga do Health Connect os registros gravados por este app. */
suspend fun limparDadosDeTeste(client: HealthConnectClient): String {
    val filtro = TimeRangeFilter.between(
        Instant.EPOCH,
        Instant.now().plus(1, ChronoUnit.DAYS)
    )
    TIPOS.forEach { client.deleteRecords(it, filtro) }
    return "Dados de teste apagados do Health Connect."
}

/** Grava UMA leitura de cada tipo com a hora atual, para simular dados chegando em tempo real. */
suspend fun inserirLeituraAgora(client: HealthConnectClient): String {
    val agora = Instant.now().minusSeconds(1) // evita horário "no futuro"
    val inicio = agora.minusSeconds(1)
    val zona = ZoneId.systemDefault().rules.getOffset(agora)

    val registros = listOf<Record>(
        HeartRateRecord(
            startTime = inicio, startZoneOffset = zona,
            endTime = agora, endZoneOffset = zona,
            samples = listOf(HeartRateRecord.Sample(inicio, (60..110).random().toLong())),
            metadata = Metadata.manualEntry()
        ),
        RestingHeartRateRecord(
            time = agora, zoneOffset = zona,
            beatsPerMinute = (52..68).random().toLong(),
            metadata = Metadata.manualEntry()
        ),
        HeartRateVariabilityRmssdRecord(
            time = agora, zoneOffset = zona,
            heartRateVariabilityMillis = 20.0 + Random.nextDouble() * 60.0,
            metadata = Metadata.manualEntry()
        ),
        OxygenSaturationRecord(
            time = agora, zoneOffset = zona,
            percentage = Percentage(94.0 + Random.nextDouble() * 5.0),
            metadata = Metadata.manualEntry()
        ),
        RespiratoryRateRecord(
            time = agora, zoneOffset = zona,
            rate = 12.0 + Random.nextDouble() * 8.0,
            metadata = Metadata.manualEntry()
        ),
        BodyTemperatureRecord(
            time = agora, zoneOffset = zona,
            temperature = Temperature.celsius(36.0 + Random.nextDouble() * 1.5),
            metadata = Metadata.manualEntry()
        ),
        BloodGlucoseRecord(
            time = agora, zoneOffset = zona,
            level = BloodGlucose.milligramsPerDeciliter(70.0 + Random.nextDouble() * 60.0),
            metadata = Metadata.manualEntry()
        ),
        BloodPressureRecord(
            time = agora, zoneOffset = zona,
            systolic = Pressure.millimetersOfMercury(105.0 + Random.nextInt(0, 30)),
            diastolic = Pressure.millimetersOfMercury(65.0 + Random.nextInt(0, 20)),
            metadata = Metadata.manualEntry()
        ),
    )
    client.insertRecords(registros)

    val pele = try {
        client.insertRecords(
            listOf(
                SkinTemperatureRecord(
                    startTime = inicio, startZoneOffset = zona,
                    endTime = agora, endZoneOffset = zona,
                    deltas = listOf(
                        SkinTemperatureRecord.Delta(
                            inicio, TemperatureDelta.celsius(Random.nextDouble() * 0.6 - 0.3)
                        )
                    ),
                    baseline = Temperature.celsius(33.0),
                    metadata = Metadata.manualEntry()
                )
            )
        )
        true
    } catch (e: Exception) {
        false
    }
    return "Leitura gravada (${registros.size + if (pele) 1 else 0} registros). " +
            "A sincronização automática deve enviar em instantes."
}