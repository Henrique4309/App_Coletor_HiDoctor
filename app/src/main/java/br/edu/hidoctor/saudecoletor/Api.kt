package br.edu.hidoctor.saudecoletor

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class Medicao(
    val id: String,
    val usuario: String,
    val tipo: String,
    val valor: Any,            // número, ou "semDados" quando não há leitura
    val valor2: Any? = null,   // diastólica (tipo desativado por enquanto)
    val unidade: String,
    val dataHora: String? = null,  // momento da medição (ausente em "semDados")
    val enviadoEm: String          // momento do envio
)

data class Alerta(
    val id: String,
    val usuario: String,
    val tipo: String,
    val dataHora: String,
    val origem: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val localizacaoEm: String? = null  // quando a posição foi obtida
)

interface ApiService {
    @POST("medicoes")
    suspend fun enviarMedicao(@Body medicao: Medicao): Response<Unit>

    @POST("alertas")
    suspend fun enviarAlerta(@Body alerta: Alerta): Response<Unit>
}

object Api {
    const val BASE_URL = "http://localhost:3000/"
    // const val BASE_URL = "http://192.168.0.15:3000/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}