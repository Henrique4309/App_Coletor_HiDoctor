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

    /** Última posição conhecida, devolvida na hora (sem esperar). Null se não houver nenhuma. */
    @SuppressLint("MissingPermission")
    fun ultimaConhecida(context: Context): Location? {
        if (!temPermissao(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.allProviders.mapNotNull {
                try { lm.getLastKnownLocation(it) } catch (e: Exception) { null }
            }.maxByOrNull { it.time }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Posição nova e precisa. Tenta o GPS (até 30 s) e, se não vier, os demais
     * provedores (até 10 s cada). Null se nenhum entregar.
     */
    @SuppressLint("MissingPermission")
    suspend fun precisa(context: Context): Location? {
        if (!temPermissao(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val ordem = buildList {
            add(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter {
            try { lm.isProviderEnabled(it) } catch (e: Exception) { false }
        }
        for (provedor in ordem) {
            val limite = if (provedor == LocationManager.GPS_PROVIDER) 30_000L else 10_000L
            val local = withTimeoutOrNull(limite) { posicaoAtual(context, lm, provedor) }
            if (local != null) return local
        }
        return null
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
}
