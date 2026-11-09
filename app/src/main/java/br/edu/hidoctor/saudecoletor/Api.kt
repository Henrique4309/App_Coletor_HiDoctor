package br.edu.hidoctor.saudecoletor

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class Medicao(
    val id: String,                 // criado quando o dado é coletado, não quando é enviado
    val usuario: String,
    val tipo: String,
    val valor: Any,                 // número, ou "semDados" quando não há leitura
    val valor2: Any? = null,        // diastólica (pressão arterial)
    val unidade: String? = null,
    val dataHora: String? = null,   // momento da medição (ausente em "semDados")
    val enviadoEm: String? = null   // preenchido na hora de cada tentativa de envio
)

data class Alerta(
    val id: String,
    val usuario: String,
    val tipo: String,
    val dataHora: String? = null,   // gravado só depois que o servidor confirma o 1º envio
    val origem: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val localizacaoEm: String? = null  // quando a posição foi obtida
)

interface ApiService {
    @POST("medicoes")
    suspend fun enviarMedicao(@Body medicao: Medicao): Response<Unit>

    @PUT("medicoes/{id}")
    suspend fun atualizarMedicao(@Path("id") id: String, @Body medicao: Medicao): Response<Unit>

    @POST("alertas")
    suspend fun enviarAlerta(@Body alerta: Alerta): Response<Unit>

    @PUT("alertas/{id}")
    suspend fun atualizarAlerta(@Path("id") id: String, @Body alerta: Alerta): Response<Unit>
}

object Api {
    const val BASE_URL = "https://api-hidoctor.onrender.com/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
