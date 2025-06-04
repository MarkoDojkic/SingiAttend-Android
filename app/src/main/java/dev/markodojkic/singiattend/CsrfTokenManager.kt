package dev.markodojkic.singiattend

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

data class CsrfSession(
    var jsessionId: String,
    var xsrfToken: String,
    var csrfTokenSecret: String,
    var csrfHeaderName: String
)

class CsrfTokenManager(
    private val serverUrl: String,
    private val credentials: String,
    var proxyIdentifier: String,
    var sessionData: CsrfSession = CsrfSession("", "", "", "")
) {
    private val client = OkHttpClient()

    fun fetchCsrfSession(callback: (Boolean) -> Unit) {
        if(proxyIdentifier.isEmpty()) callback(false)

        val request = Request.Builder()
            .url("$serverUrl/api/csrfLogin")
            .get()
            .addHeader("Authorization", "Basic $credentials")
            .addHeader("X-Tenant-ID", proxyIdentifier)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(false)
                        return
                    }

                    val cookies = it.headers.values("Set-Cookie")
                    val jsessionId = cookies.find { cookie -> cookie.startsWith("JSESSIONID=") }
                        ?.substringAfter("JSESSIONID=")?.substringBefore(";")
                    val xsrfToken = cookies.find { cookie -> cookie.startsWith("XSRF-TOKEN=") }
                        ?.substringAfter("XSRF-TOKEN=")?.substringBefore(";")

                    val body = it.body?.string() ?: ""
                    val json = JSONObject(body)
                    val csrfTokenSecret = json.optString("token")
                    val csrfHeaderName = json.optString("headerName")

                    if (!jsessionId.isNullOrEmpty() && !xsrfToken.isNullOrEmpty()) {
                        sessionData = CsrfSession(jsessionId, xsrfToken, csrfTokenSecret, csrfHeaderName)
                        callback(true)
                    } else callback(false)
                }
            }
        })
    }

    fun logoutFromCsrfSession() {
        val request = Request.Builder()
            .url("$serverUrl/api/csrfLogout")
            .get()
            .addHeader("Accept", "text/plain;charset=UTF-8")
            .addHeader("Authorization", "Basic $credentials")
            .addHeader("X-Tenant-ID", proxyIdentifier)
            .addHeader(sessionData.csrfHeaderName, sessionData.csrfTokenSecret)
            .addHeader("Cookie", "JSESSIONID=${sessionData.jsessionId}; XSRF-TOKEN=${sessionData.xsrfToken}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                response.close()

                if (success) {
                    proxyIdentifier = ""
                    sessionData = CsrfSession("", "", "", "")
                }
            }
        })
    }
}