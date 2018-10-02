// Copyright 2018 Ricoh Company, Ltd. All rights reserved.

package com.theta360.sample.livestreaming

import android.content.Context
import android.content.Intent
import android.hardware.Camera
import org.webrtc.CapturerObserver

import org.webrtc.SurfaceTextureHelper
import org.webrtc.ThreadUtils
import org.webrtc.VideoCapturer

class ThetaCapturer(
        private val shootingMode: ShootingMode
) : VideoCapturer {
    companion object {
        fun actionMainCameraClose(context: Context) {
            context.sendBroadcast(Intent("com.theta360.plugin.ACTION_MAIN_CAMERA_CLOSE"))
        }

        fun actionMainCameraOpen(context: Context) {
            context.sendBroadcast(Intent("com.theta360.plugin.ACTION_MAIN_CAMERA_OPEN"))
        }
    }

    private var camera: Camera? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturerObserver: CapturerObserver? = null


    override fun initialize(surfaceTextureHelper: SurfaceTextureHelper, context: Context, capturerObserver: CapturerObserver) {
        this.surfaceTextureHelper = surfaceTextureHelper
        this.capturerObserver = capturerObserver
    }

    override fun startCapture(width: Int, height: Int, framerate: Int) {
        this.camera = Camera.open()

        surfaceTextureHelper!!
                .surfaceTexture
                .setDefaultBufferSize(shootingMode.width, shootingMode.height)
        surfaceTextureHelper!!.setTextureSize(shootingMode.width, shootingMode.height)
        camera!!.setPreviewTexture(surfaceTextureHelper!!.surfaceTexture)

        val params = camera!!.parameters.apply {
            set("RIC_SHOOTING_MODE", shootingMode.value)
            previewFrameRate = framerate
        }
        camera!!.parameters = params
        camera!!.startPreview()

        capturerObserver!!.onCapturerStarted(true)
        surfaceTextureHelper!!.startListening(capturerObserver!!::onFrameCaptured)
    }

    override fun stopCapture() {
        ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper!!.handler) {
            camera!!.stopPreview()
            surfaceTextureHelper!!.stopListening()
            capturerObserver!!.onCapturerStopped()
        }
    }

    override fun changeCaptureFormat(width: Int, height: Int, framerate: Int) {
        throw UnsupportedOperationException("changeCaptureFormat is not supported.")
    }

    override fun dispose() {
        camera?.release()
        camera = null
    }

    override fun isScreencast(): Boolean = false

    enum class ShootingMode(
            val value: String,
            val width: Int,
            val height: Int
    ) {
        RIC_MOVIE_PREVIEW_640("RicMoviePreview640", 640, 320),
        RIC_MOVIE_PREVIEW_1024("RicMoviePreview1024", 1024, 512),
        RIC_MOVIE_PREVIEW_1920("RicMoviePreview1920", 1920, 960),
        RIC_MOVIE_PREVIEW_3840("RicMoviePreview3840", 3840, 1920)
    }
}
