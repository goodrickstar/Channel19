package com.cb3g.channel19

import android.util.Log
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class UtilsK {
    fun call(url: String, payload: String, callback: Callback) {
        val okHttpClient = OkHttpClient()
        val requestBody = payload.toRequestBody()
        val request = Request.Builder()
            .post(requestBody)
            .url(url)
            .build()
        okHttpClient.newCall(request).enqueue(callback)
    }

}

fun String.log(){
    Log.i("logging", this)
}
