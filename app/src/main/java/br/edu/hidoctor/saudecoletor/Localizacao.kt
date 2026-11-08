package br.edu.hidoctor.saudecoletor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object Localizacao {

    fun temPermissao(context: Context): Boolean =
        concedida(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                concedida(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun ligada(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    /** Posição atual (espera até 5 s) ou, se não vier a tempo, a última conhecida. Null se não houver nenhuma. */
    @SuppressLint("MissingPermission")
    suspend fun obter(context: Context): Location? {
        if (!temPermissao(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val provedor = provedoresAtivos(lm).firstOrNull()
            val atual = if (provedor != null) {
                withTimeoutOrNull(5_000) { posicaoAtual(context, lm, provedor) }
            } else null
            atual ?: ultimaConhecida(lm)
        } catch (e: SecurityException) {
            null
        }
    }

    private fun provedoresAtivos(lm: LocationManager): List<String> {
        val candidatos = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        return candidatos.filter {
            try { lm.isProviderEnabled(it) } catch (e: Exception) { false }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun posicaoAtual(
        context: Context, lm: LocationManager, provedor: String
    ): Location? = suspendCancellableCoroutine { cont ->
        val sinal = CancellationSignal()
        cont.invokeOnCancellation { sinal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(
                lm, provedor, sinal, ContextCompat.getMainExecutor(context)
            ) { local -> if (cont.isActive) cont.resume(local) }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun ultimaConhecida(lm: LocationManager): Location? =
        lm.allProviders.mapNotNull {
            try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null }
        }.maxByOrNull { it.time }
}