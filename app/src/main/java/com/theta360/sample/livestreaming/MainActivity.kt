// Copyright 2018 Ricoh Company, Ltd. All rights reserved.

package com.theta360.sample.livestreaming

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.experimental.launch
import org.webrtc.*

class MainActivity : Activity() {
    companion object {
        private val TAG = MainActivity::class.simpleName
        private val SHOOTING_MODE = ThetaCapturer.ShootingMode.RIC_MOVIE_PREVIEW_1920
    }

    private var localView: SurfaceViewRenderer? = null

    private var signaling: SignalingChannel? = null
    private var peer: PeerChannel? = null
    private var capturer: ThetaCapturer? = null
    private var eglBase: EglBase? = null

    private var ticket: Ticket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        localView = findViewById(R.id.local_view)

        eglBase = EglBase.create()
        localView!!.init(eglBase!!.eglBaseContext, null)

        launch {
            start()
        }
    }

    private fun start() {
        val accessToken = AuthClient().getToken(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET)
        ticket = RoomClient(accessToken).createTicket(BuildConfig.ROOM_ID)
        signaling = SignalingChannel(ticket!!.url, SignalingChannelListener())
        signaling!!.open()
    }

    private inner class SignalingChannelListener : SignalingChannel.Listener {
        override fun onOpen() {
            Log.d(TAG, "SignalingChannel.Listener#onOpen()")

            signaling!!.sendConnect(
                    SignalingChannel.Role.UPSTREAM,
                    BuildConfig.ROOM_ID,
                    ticket!!.accessToken,
                    SignalingChannel.VideoCodec.VP9,
                    2000,
                    SignalingChannel.AudioCodec.OPUS
            )
        }

        override fun onOffer(offerSDP: SessionDescription, config: PeerConnection.RTCConfiguration) {
            Log.d(TAG, "SignalingChannel.Listener#onOffer(offerSDP=$offerSDP, config=$config)")

            // Configures RICOH THETA's microphone and camera. This is not a general Android configuration.
            // see https://api.ricoh/docs/theta-plugin-reference/audio-manager-api/
            // see https://api.ricoh/docs/theta-plugin-reference/broadcast-intent/#notifying-camera-device-control
            (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .setParameters("RicUseBFormat=false") // recording in monaural
            ThetaCapturer.actionMainCameraClose(applicationContext)

            Log.d(TAG, "create peer connection")
            peer = PeerChannel(
                    applicationContext,
                    config,
                    PeerConnectionObserver(),
                    eglBase!!.eglBaseContext
            )

            Log.d(TAG, "set up audio capturer, source, track")
            val audioConstraints = MediaConstraints()
            val audioSource = peer!!.createAudioSource(audioConstraints)
            val audioTrack = peer!!.createAudioTrack(audioSource)

            Log.d(TAG, "set up video capturer, source, track")
            capturer = ThetaCapturer(SHOOTING_MODE)
            val videoSource = peer!!.createVideoSource(capturer!!)
            val videoTrack = peer!!.createVideoTrack(videoSource).apply {
                setEnabled(true)
                addSink(localView)
            }

            Log.d(TAG, "set up local stream")
            val stream = peer!!.createLocalMediaStream().apply {
                addTrack(audioTrack)
                addTrack(videoTrack)
            }
            peer!!.addStream(stream)

            Log.d(TAG, "start capture")
            capturer!!.startCapture(0, 0, 30)

            launch {
                peer!!.setRemoteDescription(offerSDP)
                val answerSDP = peer!!.createAnswer(MediaConstraints())
                peer!!.setLocalDescription(answerSDP)
                signaling!!.sendAnswer(answerSDP)
            }
        }

        override fun onClosed(code: Int, reason: String?) {
            Log.d(TAG, "SignalingChannel.Listener#onClosed(code=$code, reason=$reason)")

            Toast.makeText(applicationContext, "signaling channel is closed (code=$code, reason=$reason)", Toast.LENGTH_SHORT).show()

            capturer?.stopCapture()

            // Configures RICOH THETA's camera. This is not a general Android configuration.
            // see https://api.ricoh/docs/theta-plugin-reference/broadcast-intent/#notifying-camera-device-control
            ThetaCapturer.actionMainCameraOpen(applicationContext)
        }
    }

    private inner class PeerConnectionObserver : LoggingPeerConnectionObserver() {
        override fun onIceCandidate(candidate: IceCandidate?) {
            super.onIceCandidate(candidate)

            if (candidate != null) {
                signaling!!.sendCandidate(candidate.sdp)
            }
        }
    }
}
