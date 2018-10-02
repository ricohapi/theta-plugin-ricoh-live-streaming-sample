// Copyright 2018 Ricoh Company, Ltd. All rights reserved.

package com.theta360.sample.livestreaming

import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.json.Json


class AuthClient {
    private val client = OkHttpClient()

    /**
     * Issues access token by client credentials grant.
     *
     * see https://api.ricoh/docs/ricoh-cloud-api-reference/client-credentials-grant/#!/default/post_token
     */
    fun getToken(clientID: String, clientSecret: String): String {
        val reqBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("scope", "sfu.api.ricoh/v1/sfu")
                .build()

        val req = Request.Builder()
                .url("https://auth.api.ricoh/v1/token")
                .method("POST", reqBody)
                .addHeader("Authorization", Credentials.basic(clientID, clientSecret))
                .build()

        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            throw Error("failed to create access token : ${res.code()} ${res.body()}")
        }

        val resBody = Json.createReader(res.body()!!.byteStream()).readObject()
        return resBody.getString("access_token")
    }
}