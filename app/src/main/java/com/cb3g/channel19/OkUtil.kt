package com.cb3g.channel19

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Date


class OkUtil {

    private fun compact(claims: Map<String, Any>, key: String): String {
        val keyBytes = Decoders.BASE64.decode(key)
        val signingKey = Keys.hmacShaKeyFor(keyBytes)
        return Jwts.builder().claims(claims).issuedAt(Date(System.currentTimeMillis())).expiration(Date(System.currentTimeMillis() + 60000)).signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    fun request(url: String, claims: Map<String, Any>) : Request{
        val formBody = FormBody.Builder().add("data", compact(claims, RadioService.operator.key)).build()
        return Request.Builder().url(url).post(formBody).build()
    }
    fun request(url: String, claims: Map<String, Any>, key: String) : Request{
        val formBody = FormBody.Builder().add("data", compact(claims, key)).build()
        return Request.Builder().url(url).post(formBody).build()
    }

    private fun compact(key: String): String {
        val keyBytes = Decoders.BASE64.decode(key)
        val signingKey = Keys.hmacShaKeyFor(keyBytes)
        return Jwts.builder().issuedAt(Date(System.currentTimeMillis())).expiration(Date(System.currentTimeMillis() + 60000)).signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    fun call(client: OkHttpClient, url: String, claims: Map<String, Any>, signingKey: String, callback: Callback) {
        client.newCall(request(url, claims, signingKey)).enqueue(callback)
    }

    fun call(file: String, claims: Map<String, Any>, callback: Callback) {
        RadioService.client.newCall(request(RadioService.SITE_URL + file, claims)).enqueue(callback)
    }
    fun call(file: String, claims: Map<String, Any>) {
        RadioService.client.newCall(request(RadioService.SITE_URL + file, claims)).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
            }

        })
    }
    fun call(file: String, callback: Callback) {
        val formBody = FormBody.Builder().add("data", compact(RadioService.operator.key)).build()
        val request: Request = Request.Builder().url(RadioService.SITE_URL + file).post(formBody).build()
        RadioService.client.newCall(request).enqueue(callback)
    }
}