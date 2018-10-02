// Copyright 2018 Ricoh Company, Ltd. All rights reserved.

package com.theta360.sample.livestreaming

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.io.StringReader
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonValue

class SignalingChannel(
        private val endpoint: URI,
        private val listener: Listener?
) {
    companion object {
        private val TAG = SignalingChannel::class.simpleName
    }

    private val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

    private var websocket: WebSocket? = null

    interface Listener {
        fun onOpen()
        fun onOffer(offerSDP: SessionDescription, config: PeerConnection.RTCConfiguration)
        fun onClosed(code: Int, reason: String?)
    }

    enum class Role(val value: String) {
        UPSTREAM("upstream"),
        DOWNSTREAM("downstream");
    }

    enum class VideoCodec(val value: String) {
        VP8("VP8"),
        VP9("VP9"),
        H264("H264"),
    }

    enum class AudioCodec(val value: String) {
        OPUS("OPUS"),
        PMCU("PCMU"),
    }

    fun open() {
        val req = Request.Builder().url(endpoint.toString()).build()
        client.newWebSocket(req, WebSocketListener())
    }

    fun sendConnect(
            role: Role,
            channelID: String,
            accessToken: String,
            videoCodec: VideoCodec? = null,
            videoBitrate: Int? = null,
            audioCodec: AudioCodec? = null
    ) {
        val builder = Json.createObjectBuilder()
                .add("type", "connect")
                .add("role", role.value)
                .add("channel_id", channelID)
                .add("metadata", Json.createObjectBuilder()
                        .add("access_token", accessToken)
                )
                .add("plan_b", true)
        if (videoCodec != null) {
            builder.add("video", Json.createObjectBuilder()
                    .add("codec_type", videoCodec.value)
            )
            if (videoBitrate != null) {
                builder.add("bit_rate", videoBitrate)
            }
        }
        if (audioCodec != null) {
            builder.add("audio", Json.createObjectBuilder()
                    .add("codec_type", audioCodec.value)
            )
        }
        send(builder.build())
    }

    fun sendAnswer(sdp: SessionDescription) {
        val json = Json.createObjectBuilder()
                .add("type", "answer")
                .add("sdp", sdp.description)
                .build()
        send(json)
    }

    fun sendCandidate(sdp: String) {
        val json = Json.createObjectBuilder()
                .add("type", "candidate")
                .add("candidate", sdp)
                .build()
        send(json)
    }

    private fun pong() {
        val json = Json.createObjectBuilder().add("type", "pong").build()
        send(json)
    }

    private fun send(json: JsonObject) {
        Log.d(TAG, "send $json")
        websocket!!.send(json.toString())
    }

    private inner class WebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(ws: WebSocket?, res: Response?) {
            Log.d(TAG, "WebSocketListener.onOpen($ws, $res)")
            websocket = ws
            listener?.onOpen()
        }

        override fun onFailure(ws: WebSocket?, t: Throwable?, res: Response?) {
            Log.d(TAG, "WebSocketListener.onFailure($ws, $t, $res)")
            websocket = null
        }

        override fun onMessage(ws: WebSocket?, text: String?) {
            Log.d(TAG, "WebSocketListener.onMessage($ws, $text)")

            val json = Json.createReader(StringReader(text)).readObject()

            if (json["type"]?.valueType != JsonValue.ValueType.STRING) {
                Log.w(TAG, "received message does not contain correct 'type' attribute")
                return
            }
            val type = json.getString("type")

            when (type) {
                "offer" -> {
                    val offer = OfferMessage.valueOf(json)
                    listener?.onOffer(offer.sdp, offer.config)
                }
                "ping" -> {
                    pong()
                }
                else -> {
                    Log.w(TAG, "received message contains undefined 'type' value : $type")
                }
            }
        }

        override fun onMessage(ws: WebSocket?, bytes: ByteString?) {
            Log.d(TAG, "WebSocketListener.onMessage($ws, $bytes)")
        }

        override fun onClosing(ws: WebSocket?, code: Int, reason: String?) {
            Log.d(TAG, "WebSocketListener.onClosing($ws, $code, $reason)")
        }

        override fun onClosed(ws: WebSocket?, code: Int, reason: String?) {
            Log.d(TAG, "WebSocketListener.onClosing($ws, $code, $reason)")
            listener?.onClosed(code, reason)
        }
    }

    private data class OfferMessage(
            val sdp: SessionDescription,
            val clientID: String,
            val config: PeerConnection.RTCConfiguration
    ) {
        companion object {
            const val TYPE = "offer"

            fun valueOf(json: JsonObject): OfferMessage {
                val type = json.getString("type")
                if (type != TYPE) {
                    throw Error("unexpected message type : $type")
                }
                val sdpStr = json.getString("sdp")
                val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpStr)
                val clientID = json.getString("client_id")
                val configJson = json.getJsonObject("config")
                val config = convertConfig(configJson)
                return OfferMessage(sdp, clientID, config)
            }

            private fun convertIceServers(json: JsonObject): List<PeerConnection.IceServer> {
                val username = json.getString("username")
                val credential = json.getString("credential")
                return json.getJsonArray("urls")
                        .map { it.toString() }
                        .map { url ->
                            PeerConnection.IceServer.builder(url)
                                    .setUsername(username)
                                    .setPassword(credential)
                                    .createIceServer()
                        }
                        .toList()
            }

            private fun convertConfig(json: JsonObject): PeerConnection.RTCConfiguration {
                val iceTransportPolicy = json.getString("iceTransportPolicy")
                val iceServers = json.getJsonArray("iceServers")
                        .map { convertIceServers(it as JsonObject) }
                        .flatMap { it }
                        .toList()
                return PeerConnection.RTCConfiguration(iceServers).apply {
                    if (iceTransportPolicy == "relay") {
                        iceTransportsType = PeerConnection.IceTransportsType.RELAY
                    }
                    bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                    rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                    continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                    keyType = PeerConnection.KeyType.ECDSA
                }
            }
        }
    }
}
