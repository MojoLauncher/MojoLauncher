package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.util.ArrayMap
import android.util.Log
import com.google.gson.Gson
import net.kdt.pojavlaunch.Tools
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Objects

@Suppress("unused")
class ApiHandler {
    val baseUrl: String?
    val additionalHeaders: MutableMap<String?, String?>?

    constructor(url: String?) {
        baseUrl = url
        additionalHeaders = null
    }

    constructor(url: String?, apiKey: String?) {
        baseUrl = url
        additionalHeaders = ArrayMap<String?, String?>()
        additionalHeaders.put("x-api-key", apiKey)
    }

    fun <T> get(endpoint: String?, tClass: Class<T>): T? {
        return getFullUrl<T>(additionalHeaders, baseUrl + "/" + endpoint, tClass)
    }

    fun <T> get(endpoint: String?, query: HashMap<String?, Any?>, tClass: Class<T>): T? {
        return getFullUrl<T>(additionalHeaders, baseUrl + "/" + endpoint, query, tClass)
    }

    fun <T> post(endpoint: String?, body: T?, tClass: Class<T>): T? {
        return postFullUrl<T>(additionalHeaders, baseUrl + "/" + endpoint, body, tClass)
    }

    fun <T> post(
        endpoint: String?,
        query: HashMap<String?, Any?>,
        body: T?,
        tClass: Class<T>
    ): T? {
        return postFullUrl<T>(additionalHeaders, baseUrl + "/" + endpoint, query, body, tClass)
    }

    companion object {
        //Make a get request and return the response as a raw string;
        fun getRaw(url: String?): String? {
            return getRaw(null, url)
        }

        fun getRaw(headers: MutableMap<String?, String?>?, url: String?): String? {
            if (url == null) return null
            Log.d("ApiHandler", url)
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                addHeaders(conn, headers)
                val inputStream = conn.getInputStream()
                val data = Tools.read(inputStream)
                Log.d(ApiHandler::class.java.toString(), data ?: "null")
                inputStream.close()
                conn.disconnect()
                return data
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun postRaw(url: String?, body: String): String? {
            return postRaw(null, url, body)
        }

        fun postRaw(headers: MutableMap<String?, String?>?, url: String?, body: String): String? {
            if (url == null) return null
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestMethod("POST")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                addHeaders(conn, headers)
                conn.setDoOutput(true)

                val outputStream = conn.getOutputStream()
                val input = body.toByteArray(StandardCharsets.UTF_8)
                outputStream.write(input, 0, input.size)
                outputStream.close()

                val inputStream = conn.getInputStream()
                val data = Tools.read(inputStream)
                inputStream.close()

                conn.disconnect()
                return data
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        private fun addHeaders(
            connection: HttpURLConnection,
            headers: MutableMap<String?, String?>?
        ) {
            if (headers != null) {
                for (key in headers.keys) {
                    if (key != null) connection.addRequestProperty(key, headers.get(key))
                }
            }
        }

        private fun parseQueries(query: HashMap<String?, Any?>): String {
            val params = StringBuilder("?")
            for (param in query.keys) {
                val value = Objects.toString(query.get(param))
                params.append(urlEncodeUTF8(param))
                    .append("=")
                    .append(urlEncodeUTF8(value))
                    .append("&")
            }
            return params.substring(0, params.length - 1)
        }

        fun <T> getFullUrl(url: String?, tClass: Class<T>): T? {
            return getFullUrl<T>(null, url, tClass)
        }

        fun <T> getFullUrl(url: String?, query: HashMap<String?, Any?>, tClass: Class<T>): T? {
            return getFullUrl<T>(null, url, query, tClass)
        }

        fun <T> postFullUrl(url: String?, body: T?, tClass: Class<T>): T? {
            return postFullUrl<T>(null, url, body, tClass)
        }

        fun <T> postFullUrl(
            url: String?,
            query: HashMap<String?, Any?>,
            body: T?,
            tClass: Class<T>
        ): T? {
            return postFullUrl<T>(null, url, query, body, tClass)
        }

        fun <T> getFullUrl(
            headers: MutableMap<String?, String?>?,
            url: String?,
            tClass: Class<T>
        ): T? {
            val raw = getRaw(headers, url) ?: return null
            return Gson().fromJson<T>(raw, tClass)
        }

        fun <T> getFullUrl(
            headers: MutableMap<String?, String?>?,
            url: String?,
            query: HashMap<String?, Any?>,
            tClass: Class<T>
        ): T? {
            return getFullUrl<T>(headers, (url ?: "") + parseQueries(query), tClass)
        }

        fun <T> postFullUrl(
            headers: MutableMap<String?, String?>?,
            url: String?,
            body: T?,
            tClass: Class<T>
        ): T? {
            val raw = postRaw(headers, url, body.toString()) ?: return null
            return Gson().fromJson<T>(raw, tClass)
        }

        fun <T> postFullUrl(
            headers: MutableMap<String?, String?>?,
            url: String?,
            query: HashMap<String?, Any?>,
            body: T?,
            tClass: Class<T>
        ): T? {
            val raw = postRaw(headers, (url ?: "") + parseQueries(query), body.toString()) ?: return null
            return Gson().fromJson<T>(raw, tClass)
        }

        private fun urlEncodeUTF8(input: String?): String {
            try {
                return URLEncoder.encode(input ?: "", "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("UTF-8 is required")
            }
        }
    }
}
