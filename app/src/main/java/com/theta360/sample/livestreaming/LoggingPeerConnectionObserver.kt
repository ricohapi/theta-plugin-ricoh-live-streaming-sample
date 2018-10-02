// Copyright 2018 Ricoh Company, Ltd. All rights reserved.

package com.theta360.sample.livestreaming

import android.util.Log
import org.webrtc.*

abstract class LoggingPeerConnectionObserver : PeerConnection.Observer {
    companion object {
        private val TAG = LoggingPeerConnectionObserver::class.simpleName
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Log.d(TAG, "PeerConnection.Observer#onSignalingChange(newState=$newState)")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "PeerConnection.Observer#onIceConnectionChange(newState=$newState)")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Log.d(TAG, "PeerConnection.Observer#onIceConnectionReceivingChange(receiving=$receiving)")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "PeerConnection.Observer#onIceGatheringChange(newState=$newState)")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(TAG, "PeerConnection.Observer#onIceCandidate(candidate=$candidate)")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        Log.d(TAG, "PeerConnection.Observer#onIceCandidatesRemoved(candidates=$candidates)")
    }

    override fun onAddStream(stream: MediaStream?) {
        Log.d(TAG, "PeerConnection.Observer#onAddStream(stream=$stream)")
    }

    override fun onRemoveStream(stream: MediaStream?) {
        Log.d(TAG, "PeerConnection.Observer#onRemoveStream(stream=$stream)")
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        Log.d(TAG, "PeerConnection.Observer#onDataChannel(dataChannel=$dataChannel)")
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "PeerConnection.Observer#onRenegotiationNeeded()")
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        Log.d(TAG, "PeerConnection.Observer#onAddTrack(receiver=$receiver, mediaStreams=$mediaStreams)")
    }
}
