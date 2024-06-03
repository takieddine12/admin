package com.app.livestreamingapp


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
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


class AdminActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQ_ID = 22
    }
    private var isUsingFrontCamera = true
    private lateinit var remoteVideoContainers: Array<FrameLayout>
    private lateinit var remoteSurfaceView: SurfaceView
    private val appId = "c24451635a5144aa85101bb7e211faee"
    private val channelName = "security"
    private var mRtcEngine: RtcEngine? = null
    private val surfaceViews = mutableMapOf<Int, SurfaceView>()
    private val userContainerMap = mutableMapOf<Int, Int>()
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Monitor remote users in the channel and obtain their uid
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                onRemoteUserLeft(uid)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_main)


        remoteVideoContainers = arrayOf(
            findViewById(R.id.remote_video_view_container),
            findViewById(R.id.remote_video_view_container1),
            findViewById(R.id.remote_video_view_container2),
            findViewById(R.id.remote_video_view_container3)
        )
        remoteSurfaceView = SurfaceView(baseContext)
        if (checkPermissions()) {
            initialize()
        }
        else {
            ActivityCompat.requestPermissions(this, getRequiredPermissions()!!, PERMISSION_REQ_ID);
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
        val options = ChannelMediaOptions()
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        mRtcEngine?.joinChannel(appId, channelName, System.currentTimeMillis().toInt(), options)

        mRtcEngine!!.enableLocalVideo(false)
        mRtcEngine!!.enableLocalAudio(false)
        mRtcEngine!!.disableAudio()
    }
    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceViews[uid] = surfaceView
        val containerIndex = userContainerMap.getOrPut(uid) {
            remoteVideoContainers.indexOfFirst { it.childCount == 0 }
        }
        if (containerIndex >= 0) {
            val container = remoteVideoContainers[containerIndex]
            container.removeAllViews()
            container.addView(surfaceView)
            mRtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        }
    }

    private fun onRemoteUserLeft(uid: Int) {
        val containerIndex = userContainerMap[uid]
        containerIndex?.let {
            val container = remoteVideoContainers[it]
            container.removeAllViews()
        }
        surfaceViews.remove(uid)
        userContainerMap.remove(uid)
    }
    private fun getRequiredPermissions(): Array<String>? {
        // Determine the permissions required when targetSDKVersion is 31 or above
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf<String>(
                android.Manifest.permission.RECORD_AUDIO,  // Recording permission
                android.Manifest.permission.CAMERA,  // Camera permission
                android.Manifest.permission.READ_PHONE_STATE,  // Permission to read phone status
                android.Manifest.permission.BLUETOOTH_CONNECT // Bluetooth connection permission
            )
        } else {
            arrayOf<String>(
                android.Manifest.permission.RECORD_AUDIO,
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
        mRtcEngine?.stopPreview()
        mRtcEngine?.leaveChannel()
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