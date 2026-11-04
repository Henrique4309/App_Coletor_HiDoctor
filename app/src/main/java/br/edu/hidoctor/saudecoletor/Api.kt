package br.edu.hidoctor.saudecoletor

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class Medicao(
    val id: String,
    val tipo: String,
    val valor: Double,
    val valor2: Double? = null,   // só usado na pressão (diastólica)
    val unidade: String,
    val dataHora: String
)

data class Alerta(
    val id: String,
    val tipo: String,
    val dataHora: String,
    val origem: String
)

interface ApiService {
    @POST("medicoes")
    suspend fun enviarMedicao(@Body medicao: Medicao): Response<Unit>

    @POST("alertas")
    suspend fun enviarAlerta(@Body alerta: Alerta): Response<Unit>
}

object Api {
    // Emulador com "adb reverse tcp:3000 tcp:3000": localhost
    const val BASE_URL = "http://localhost:3000/"
    // Celular real na mesma rede Wi-Fi: use o IP do PC
    // const val BASE_URL = "http://192.168.0.15:3000/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}