// Copyright 2018 Ricoh Company, Ltd. All rights reserved.

package com.theta360.sample.livestreaming

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.net.URI
import javax.json.Json
import javax.json.JsonObject


class RoomClient(
        private val accessToken: String
) {
    private val client = OkHttpClient()

    /**
     * Create ticket for the channel.
     *
     * see https://api.ricoh/docs/ricoh-cloud-api-reference/live-streaming/#MakeTicket
     */
    fun createTicket(channelID: String): Ticket {
        val mimeType = MediaType.parse("application/json")
        val reqBody = RequestBody.create(mimeType, """{"direction":"up"}""")
        val req = Request.Builder()
                .url("https://sfu.api.ricoh/v1/rooms/$channelID/tickets")
                .method("POST", reqBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            throw Error("failed to create ticket : ${res.code()} ${res.body()}")
        }

        val resBody = Json.createReader(res.body()!!.byteStream()).readObject()
        return Ticket.valueOf(resBody)
    }
}

data class Ticket(
        val id: String,
        val accessToken: String,
        val direction: Direction,
        val url: URI
) {
    companion object {
        fun valueOf(json: JsonObject): Ticket {
            val id = json.getString("id")
            val accessToken = json.getString("access_token")
            val directionStr = json.getString("direction")
            val direction = Direction.parse(directionStr)
            val urlStr = json.getString("url")
            val url = URI(urlStr)
            return Ticket(id, accessToken, direction, url)
        }
    }

    enum class Direction(val value: String) {
        UP("up"),
        DOWN("down");

        companion object {
            fun parse(value: String): Direction {
                return Direction.values().find { it.value == value }
                        ?: throw Error("undefined direction : $value")
            }
        }
    }

}
