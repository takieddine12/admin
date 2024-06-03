package com.app.livestreamingapp


import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraCaptureSession
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration.MIRROR_MODE_TYPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ClientActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQ_ID = 22
    }
    private lateinit var surfaceView : SurfaceView
    private lateinit var startCall : ImageView
    private lateinit var endCall : ImageView
    private lateinit var switchCamera : ImageView
    private lateinit var container : FrameLayout
    private val appId = "c24451635a5144aa85101bb7e211faee"
    private val channelName = "security"
    private var mRtcEngine: RtcEngine? = null
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_activity_main)

        switchCamera = findViewById(R.id.switchCamera)
        startCall = findViewById(R.id.startCall)
        endCall = findViewById(R.id.endCall)
        container = findViewById(R.id.local_video_view_container)
        surfaceView = SurfaceView(baseContext)
        startCall.setOnClickListener {
            if (checkPermissions()) {
                initialize()
            } else {
                ActivityCompat.requestPermissions(this, getRequiredPermissions()!!, PERMISSION_REQ_ID);
            }
        }
        endCall.setOnClickListener {
            if (mRtcEngine != null){
                leaveChannel()
                startCall.visibility = View.VISIBLE
                endCall.visibility = View.GONE

            }
        }
        switchCamera.setOnClickListener {
            mRtcEngine!!.switchCamera()
        }

    }

    private fun initialize() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            mRtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }


        mRtcEngine!!.enableVideo()
        mRtcEngine!!.startPreview()
        container.addView(surfaceView)
        mRtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        val options = ChannelMediaOptions()
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        mRtcEngine?.joinChannel(appId, channelName, System.currentTimeMillis().toInt(), options)
        startCall.visibility = View.INVISIBLE
        endCall.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            if (switchCamera.isClickable){
                switchCamera.performClick()
                switchCamera.visibility = View.GONE
                switchCamera.isClickable = false
            }
        }
    }
    private fun leaveChannel(){
        mRtcEngine?.leaveChannel()
        container.removeView(surfaceView)
    }
    private fun getRequiredPermissions(): Array<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        }
    }
    private fun checkPermissions(): Boolean {
        for (permission in getRequiredPermissions()!!) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine!!.stopPreview()
        mRtcEngine!!.leaveChannel()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID && grantResults.isNotEmpty()){
            Toast.makeText(this,"Permissions granted",Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this,"Please grant permissions",Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, getRequiredPermissions()!!, PERMISSION_REQ_ID)
        }
    }



}