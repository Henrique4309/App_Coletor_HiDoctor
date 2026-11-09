package br.edu.hidoctor.saudecoletor

import java.time.Instant
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Situações que o app pode simular. O app não julga nada: só gera e envia os números. */
enum class Emergencia { PARADA, ARRITMIA, HIPOGLICEMIA, HIPERGLICEMIA }

class Leitura(
    val tipo: String,
    val valor: Double,
    val valor2: Double?,
    val unidade: String,
    val momento: Instant
)

class Posicao(val latitude: Double, val longitude: Double, val momento: Instant)

/** Nomes dos tipos de medição enviados ao servidor (os mesmos do app que lê o Health Connect). */
object Tipos {
    const val FC = "frequencia_cardiaca"
    const val FC_REPOUSO = "frequencia_repouso"
    const val HRV = "variabilidade_fc"
    const val SPO2 = "saturacao_oxigenio"
    const val RESP = "frequencia_respiratoria"
    const val TEMP = "temperatura_corporal"
    const val GLICEMIA = "glicemia"
    const val PRESSAO = "pressao_arterial"
}

/**
 * Gera sinais vitais e posição com aparência real.
 * Cada valor "caminha" em direção a um alvo, com passo máximo por ciclo (5 s):
 * por isso os números mudam aos poucos, como num sensor de verdade.
 * Só as simulações pedidas fazem os valores mudarem de forma brusca.
 */
object Simulador {

    // Região onde a posição simulada começa (centro de São Paulo). Troque se quiser outra cidade.
    private const val LATITUDE_BASE = -23.5505
    private const val LONGITUDE_BASE = -46.6333

    private val rnd = Random()
    private fun gauss() = rnd.nextGaussian()
    private fun entre(a: Double, b: Double) = a + rnd.nextDouble() * (b - a)

    private fun mover(x: Double, alvo: Double, passoMax: Double, k: Double, ruido: Double): Double {
        val delta = (k * (alvo - x) + gauss() * ruido).coerceIn(-passoMax, passoMax)
        return x + delta
    }

    private fun r0(v: Double) = Math.round(v).toDouble()
    private fun r1(v: Double) = Math.round(v * 10.0) / 10.0

    // ───────── Corpo ─────────
    private var atividade = 0.1            // 0 = repouso, 1 = esforço
    private var alvoAtividade = 0.1
    private var ciclosAteMudar = 20
    private var fcRepouso = entre(58.0, 66.0)
    private var fc = fcRepouso + 5.0
    private var hrv = entre(42.0, 58.0)
    private var spo2 = entre(96.5, 98.5)
    private var resp = entre(13.0, 16.0)
    private var temp = entre(36.3, 36.8)
    private var glicemia = entre(88.0, 104.0)
    private var alvoGlicemia = entre(90.0, 108.0)
    private var sist = entre(112.0, 122.0)
    private var dia = entre(72.0, 79.0)
    private var anterior: Set<Emergencia> = emptySet()
    private var transicao = 0

    /** Gera o próximo conjunto de leituras (uma por tipo). Chamar a cada ciclo. */
    @Synchronized
    fun proximo(em: Set<Emergencia>): List<Leitura> {
        val parada = Emergencia.PARADA in em
        val arritmia = !parada && Emergencia.ARRITMIA in em
        val hipo = Emergencia.HIPOGLICEMIA in em
        val hiper = !hipo && Emergencia.HIPERGLICEMIA in em

        if (em != anterior) {
            anterior = em
            transicao = 12
        } else if (transicao > 0) {
            transicao -= 1
        }
        val rapido = transicao > 0

        // Nível de atividade: muda devagar, de tempos em tempos
        ciclosAteMudar -= 1
        if (ciclosAteMudar <= 0) {
            alvoAtividade = if (rnd.nextDouble() < 0.6) entre(0.0, 0.15) else entre(0.3, 0.8)
            ciclosAteMudar = 12 + rnd.nextInt(30)
        }
        atividade = mover(atividade, alvoAtividade, 0.04, 0.1, 0.005).coerceIn(0.0, 1.0)

        fcRepouso = mover(fcRepouso, 62.0, 0.1, 0.01, 0.03).coerceIn(56.0, 70.0)

        // Frequência cardíaca
        val baseFc = fcRepouso + 4.0 + 28.0 * atividade
        fc = when {
            parada -> mover(fc, 0.0, 45.0, 0.7, 0.0)
            arritmia -> mover(fc, entre(100.0, 170.0), 35.0, 0.8, 3.0)
            rapido -> mover(fc, baseFc, 25.0, 0.5, 1.0)
            else -> mover(fc, baseFc, 3.0, 0.2, 1.0)
        }.coerceAtLeast(0.0)
        if (parada && fc < 1.0) fc = 0.0

        // Variabilidade da FC (RMSSD)
        val baseHrv = 58.0 - 22.0 * atividade
        hrv = when {
            parada -> mover(hrv, 0.0, 15.0, 0.5, 0.0)
            arritmia -> mover(hrv, entre(80.0, 170.0), 45.0, 0.8, 3.0)
            rapido -> mover(hrv, baseHrv, 20.0, 0.4, 1.0)
            else -> mover(hrv, baseHrv, 3.0, 0.2, 1.2)
        }.coerceIn(0.0, 200.0)
        if (parada && hrv < 1.0) hrv = 0.0

        // Saturação de oxigênio
        val baseSpo2 = 97.8 - 0.8 * atividade
        spo2 = when {
            parada -> mover(spo2, 55.0, 4.0, 0.3, 0.3)
            arritmia -> mover(spo2, entre(92.0, 95.0), 1.5, 0.5, 0.3)
            rapido -> mover(spo2, baseSpo2, 4.0, 0.4, 0.2)
            else -> mover(spo2, baseSpo2, 0.4, 0.2, 0.2)
        }.coerceIn(50.0, 100.0)

        // Frequência respiratória
        val baseResp = 14.0 + 5.0 * atividade
        resp = when {
            parada -> mover(resp, 0.0, 5.0, 0.6, 0.0)
            arritmia -> mover(resp, entre(20.0, 26.0), 2.0, 0.6, 0.5)
            rapido -> mover(resp, baseResp, 3.0, 0.5, 0.3)
            else -> mover(resp, baseResp, 0.35, 0.15, 0.15)
        }.coerceIn(0.0, 40.0)
        if (parada && resp < 0.5) resp = 0.0

        // Temperatura corporal
        temp = mover(temp, 36.5 + 0.35 * atividade, 0.02, 0.05, 0.01).coerceIn(35.8, 37.4)

        // Pressão arterial
        val baseSist = 114.0 + 14.0 * atividade
        val baseDia = 74.0 + 6.0 * atividade
        sist = when {
            parada -> mover(sist, 0.0, 18.0, 0.4, 0.0)
            arritmia -> mover(sist, entre(92.0, 118.0), 14.0, 0.6, 2.0)
            rapido -> mover(sist, baseSist, 12.0, 0.4, 1.0)
            else -> mover(sist, baseSist, 1.6, 0.15, 0.6)
        }.coerceAtLeast(0.0)
        dia = when {
            parada -> mover(dia, 0.0, 12.0, 0.4, 0.0)
            arritmia -> mover(dia, entre(55.0, 75.0), 10.0, 0.6, 1.5)
            rapido -> mover(dia, baseDia, 8.0, 0.4, 0.8)
            else -> mover(dia, baseDia, 1.0, 0.15, 0.5)
        }.coerceAtLeast(0.0)
        if (parada && sist < 1.0) sist = 0.0
        if (parada && dia < 1.0) dia = 0.0
        if (dia > sist) dia = sist

        // Glicemia
        if (rnd.nextInt(60) == 0) alvoGlicemia = entre(88.0, 112.0)
        glicemia = when {
            hipo -> mover(glicemia, entre(36.0, 48.0), 8.0, 0.25, 1.0)
            hiper -> mover(glicemia, entre(410.0, 460.0), 18.0, 0.2, 3.0)
            abs(glicemia - alvoGlicemia) > 25.0 -> mover(glicemia, alvoGlicemia, 14.0, 0.25, 0.8)
            else -> mover(glicemia, alvoGlicemia, 1.5, 0.1, 0.5)
        }.coerceIn(20.0, 500.0)

        val agora = Instant.now()
        return listOf(
            Leitura(Tipos.FC, r0(fc), null, "bpm", agora),
            Leitura(Tipos.FC_REPOUSO, r0(fcRepouso), null, "bpm", agora),
            Leitura(Tipos.HRV, r0(hrv), null, "ms", agora),
            Leitura(Tipos.SPO2, r0(spo2), null, "%", agora),
            Leitura(Tipos.RESP, r0(resp), null, "rpm", agora),
            Leitura(Tipos.TEMP, r1(temp), null, "°C", agora),
            Leitura(Tipos.GLICEMIA, r0(glicemia), null, "mg/dL", agora),
            Leitura(Tipos.PRESSAO, r0(sist), r0(dia), "mmHg", agora)
        )
    }

    // ───────── Posição ─────────
    private var lat = LATITUDE_BASE + entre(-0.012, 0.012)
    private var lon = LONGITUDE_BASE + entre(-0.012, 0.012)
    private var rumo = entre(0.0, 2.0 * Math.PI)
    private var vel = 1.2                       // m/s (passo de caminhada)
    private var ultimoMovimento = System.currentTimeMillis()

    /** Posição atual: uma caminhada lenta e contínua, com pequena oscilação de GPS. */
    @Synchronized
    fun posicao(): Posicao {
        val agora = System.currentTimeMillis()
        val dt = ((agora - ultimoMovimento) / 1000.0).coerceIn(0.0, 30.0)
        ultimoMovimento = agora

        if (Emergencia.PARADA in Estado.emergencias.value) {
            vel = 0.0
        } else {
            vel = (vel + 0.1 * (1.2 - vel) + gauss() * 0.12).coerceIn(0.0, 1.8)
            rumo += gauss() * 0.2
        }
        val distancia = vel * dt
        lat += distancia * cos(rumo) / 111_320.0
        lon += distancia * sin(rumo) / (111_320.0 * cos(Math.toRadians(lat)))
        return Posicao(lat + gauss() * 0.000008, lon + gauss() * 0.000008, Instant.now())
    }
}
