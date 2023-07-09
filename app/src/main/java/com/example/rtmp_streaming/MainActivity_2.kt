package com.example.rtmp_streaming

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.rtmp_streaming.utils.PathUtils
import com.google.android.material.navigation.NavigationView
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera1Base}
 * {@link com.pedro.rtplibrary.rtmp.RtmpCamera1}
 */

class MainActivity_2 : AppCompatActivity(), View.OnClickListener, ConnectCheckerRtmp,
    SurfaceHolder.Callback,
    OnTouchListener {

    private var rtmpip:String? = "rtmp://43.201.165.228/live/test"; //src

    private val orientations = arrayOf(0, 90, 180, 270)

    private lateinit var next_ver: Button


    private lateinit var rtmpCamera1: RtmpCamera1
    private lateinit var bStartStop: Button
    private lateinit var bRecord:android.widget.Button
    private lateinit var etUrl: EditText
    private var currentDateAndTime = ""
    private var folder: File? = null

    //options menu
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
    private var rgChannel: RadioGroup? = null
    private var spResolution: Spinner? = null
    private var cbEchoCanceler: CheckBox? = null
    private  var cbNoiseSuppressor:CheckBox? = null
    private var etVideoBitrate: EditText? = null
    private  var etFps:EditText? = null
    private  var etAudioBitrate:EditText? = null
    private  var etSampleRate:EditText? = null
    private  var etWowzaUser:EditText? = null
    private var etWowzaPassword: EditText? = null
    private var lastVideoBitrate: String? = null
    private var tvBitrate: TextView? = null

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private val PERMISSIONS_A_13 = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions() //카메라 마이크 파일 권한 설정

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        folder = PathUtils.getRecordPath()
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setHomeButtonEnabled(true)

        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        surfaceView.holder.addCallback(this)
        surfaceView.setOnTouchListener(this)
        rtmpCamera1 = RtmpCamera1(surfaceView, this)

        prepareOptionsMenuViews()

        tvBitrate = findViewById(R.id.tv_bitrate)
        etUrl = findViewById(R.id.et_rtp_url)
        etUrl.setHint(R.string.hint_rtmp)
        etUrl.setText(rtmpip) //내 경로 설정

        next_ver = findViewById(R.id.next_ver)
        next_ver.setOnClickListener(this)

        bStartStop = findViewById(R.id.b_start_stop)
        bStartStop.setOnClickListener(this)
        bRecord = findViewById(R.id.b_record)
        bRecord.setOnClickListener(this)
        val switchCamera = findViewById<Button>(R.id.switch_camera)
        switchCamera.setOnClickListener(this)
    }
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_A_13, 1)
            }
        } else {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
            }
        }
    }
    private fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(context, *PERMISSIONS_A_13)
        } else {
            hasPermissions(context, *PERMISSIONS)
        }
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    //옆에 메뉴 추가하고 수정해주는 부분
    private fun prepareOptionsMenuViews() {
        drawerLayout = findViewById(R.id.activity_main)
        navigationView = findViewById(R.id.nv_rtp)
        navigationView.inflateMenu(R.menu.options_rtmp)
        actionBarDrawerToggle = object : ActionBarDrawerToggle(
            this, drawerLayout, R.string.rtmp_streamer,
            R.string.rtmp_streamer
        ) {
            override fun onDrawerOpened(drawerView: View) {
                actionBarDrawerToggle!!.syncState()
                lastVideoBitrate = etVideoBitrate!!.text.toString()
            }

            override fun onDrawerClosed(view: View) {
                actionBarDrawerToggle!!.syncState()
                if (lastVideoBitrate != null && lastVideoBitrate != etVideoBitrate!!.text.toString() && rtmpCamera1!!.isStreaming
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        val bitrate = etVideoBitrate!!.text.toString().toInt() * 1024
                        rtmpCamera1!!.setVideoBitrateOnFly(bitrate)
                        Toast.makeText(
                            this@MainActivity_2,
                            "New bitrate: $bitrate",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity_2, "Bitrate on fly ignored, Required min API 19",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        drawerLayout.addDrawerListener(actionBarDrawerToggle as ActionBarDrawerToggle)
        //checkboxs

        //AEC(Acoustic Echo Canceller)는 캡처된 오디오 신호에서 원격 당사자로부터 수신된 신호의 기여도를 제거하는 오디오 전처리기
        cbEchoCanceler =
            navigationView.getMenu().findItem(R.id.cb_echo_canceler).actionView as CheckBox?

        //노이즈 억제(NS)는 캡처된 신호에서 배경 노이즈를 제거하는 오디오 전처리기
        cbNoiseSuppressor =
            navigationView.getMenu().findItem(R.id.cb_noise_suppressor).actionView as CheckBox?

        //radiobuttons
        val rbTcp = navigationView.getMenu().findItem(R.id.rb_tcp).actionView as RadioButton?
        rgChannel = navigationView.getMenu().findItem(R.id.channel).actionView as RadioGroup?
        rbTcp!!.isChecked = true

        //spinners - 해상도 생성
        spResolution = navigationView.getMenu().findItem(R.id.sp_resolution).actionView as Spinner?
//        val orientationAdapter =
//            ArrayAdapter<Int>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
//        orientationAdapter.addAll(*orientations)

        val resolutionAdapter =
            ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
        val list: MutableList<String> = ArrayList()
        for (size in rtmpCamera1!!.resolutionsBack) {
            Log.d("TAG_R", "resolutionAdapter: ")
            Log.d("TAG_R", size.width.toString())
            Log.d("TAG_R", size.height.toString())

            list.add(size.width.toString() + "X" + size.height)
        }
        resolutionAdapter.addAll(list)
        spResolution!!.adapter = resolutionAdapter
        //edittexts - 비트레이트
        etVideoBitrate =
            navigationView.getMenu().findItem(R.id.et_video_bitrate).actionView as EditText?
        etFps = navigationView.getMenu().findItem(R.id.et_fps).actionView as EditText?
        etAudioBitrate =
            navigationView.getMenu().findItem(R.id.et_audio_bitrate).actionView as EditText?
        etSampleRate = navigationView.getMenu().findItem(R.id.et_samplerate).actionView as EditText?

        //하드 코딩
        etVideoBitrate!!.setText("2500")
        etFps!!.setText("30")
        etAudioBitrate!!.setText("128")
        etSampleRate!!.setText("44100")

        etWowzaUser = navigationView.getMenu().findItem(R.id.et_user).actionView as EditText?
        etWowzaPassword =
            navigationView.getMenu().findItem(R.id.et_password).actionView as EditText?
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        actionBarDrawerToggle?.syncState()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START)
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                true
            }

            R.id.microphone -> {
                if (!rtmpCamera1.isAudioMuted) {
                    item.icon = resources.getDrawable(R.drawable.icon_microphone_off)
                    rtmpCamera1.disableAudio()
                } else {
                    item.icon = resources.getDrawable(R.drawable.icon_microphone)
                    rtmpCamera1.enableAudio()
                }
                true
            }

            else -> false
        }
    }
    override fun onClick(p0: View?) {
        if (p0 != null) {
            when (p0.id) {
                R.id.b_start_stop -> {
                    Log.d("TAG_R", "b_start_stop: ")
                    if (!rtmpCamera1!!.isStreaming) {
                        bStartStop.text = resources.getString(R.string.stop_button)
                        val user = etWowzaUser!!.text.toString()
                        val password = etWowzaPassword!!.text.toString()
                        if (!user.isEmpty() && !password.isEmpty()) {
                            rtmpCamera1!!.setAuthorization(user, password)
                        }
                        if (rtmpCamera1!!.isRecording || prepareEncoders()) {
                            rtmpCamera1!!.startStream(etUrl.text.toString())
                        } else {
                            //If you see this all time when you start stream,
                            //it is because your encoder device dont support the configuration
                            //in video encoder maybe color format.
                            //If you have more encoder go to VideoEncoder or AudioEncoder class,
                            //change encoder and try
                            Toast.makeText(
                                this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT
                            ).show()
                            bStartStop.text = resources.getString(R.string.start_button)
                        }
                    } else {
                        bStartStop.text = resources.getString(R.string.start_button)
                        rtmpCamera1!!.stopStream()
                    }
                }

                R.id.b_record -> {
                    Log.d("TAG_R", "b_start_stop: ")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        if (!rtmpCamera1!!.isRecording) {
                            try {
                                if (!folder!!.exists()) {
                                    folder!!.mkdir()
                                }
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                currentDateAndTime = sdf.format(Date())
                                if (!rtmpCamera1!!.isStreaming) {
                                    if (prepareEncoders()) {
                                        rtmpCamera1!!.startRecord(
                                            folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                                        )
                                        bRecord.setText(R.string.stop_record)
                                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this, "Error preparing stream, This device cant do it",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    rtmpCamera1!!.startRecord(
                                        folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                                    )
                                    bRecord.setText(R.string.stop_record)
                                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: IOException) {
                                rtmpCamera1!!.stopRecord()
                                PathUtils.updateGallery(
                                    this,
                                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                                )
                                bRecord.setText(R.string.start_record)
                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            rtmpCamera1!!.stopRecord()
                            PathUtils.updateGallery(
                                this,
                                folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                            )
                            bRecord.setText(R.string.start_record)
                            Toast.makeText(
                                this,
                                "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                                Toast.LENGTH_SHORT
                            ).show()
                            currentDateAndTime = ""
                        }
                    } else {
                        Toast.makeText(
                            this, "You need min JELLY_BEAN_MR2(API 18) for do it...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                R.id.switch_camera -> try {
                    rtmpCamera1!!.switchCamera()
                } catch (e: CameraOpenException) {
                    Toast.makeText(this@MainActivity_2, e.message, Toast.LENGTH_SHORT).show()
                }

                R.id.next_ver -> {
                    Log.d("TAG_R", "next_ver: ")

                    //커스텀으로 이동
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }

                else -> {}
            }
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread { Toast.makeText(this@MainActivity_2, "Auth error", Toast.LENGTH_SHORT).show() }
    }

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@MainActivity_2, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity_2, "Connection failed. $reason", Toast.LENGTH_SHORT)
                .show()
            rtmpCamera1!!.stopStream()
            bStartStop.text = resources.getString(R.string.start_button)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtmpCamera1!!.isRecording
            ) {
                rtmpCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                bRecord.setText(R.string.start_record)
                Toast.makeText(
                    this@MainActivity_2,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
    }

    private fun prepareEncoders(): Boolean {

        val resolution = rtmpCamera1!!.resolutionsBack[spResolution!!.selectedItemPosition]
        val width = resolution.width
        val height = resolution.height

        Log.d("TAG_R", "prepareEncoders: ")
        Log.d("TAG_R", spResolution!!.selectedItemPosition.toString())
        Log.d("TAG_R", resolution.toString())
        Log.d("TAG_R", width.toString())
        Log.d("TAG_R", height.toString())


        return rtmpCamera1!!.prepareVideo(
            width, height, etFps!!.text.toString().toInt(),
            etVideoBitrate!!.text.toString().toInt() * 1024,
            CameraHelper.getCameraOrientation(this)
        ) && rtmpCamera1!!.prepareAudio(
            etAudioBitrate!!.text.toString().toInt() * 1024, etSampleRate!!.text.toString().toInt(),
            rgChannel!!.checkedRadioButtonId == R.id.rb_stereo, cbEchoCanceler!!.isChecked,
            cbNoiseSuppressor!!.isChecked
        )
    }
    override fun onConnectionStartedRtmp(rtmpUrl: String) {
    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@MainActivity_2, "Connection success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            Toast.makeText(this@MainActivity_2, "Disconnected", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtmpCamera1!!.isRecording
            ) {
                rtmpCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                bRecord.setText(R.string.start_record)
                Toast.makeText(
                    this@MainActivity_2,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        runOnUiThread { tvBitrate!!.text = "$bitrate bps" }
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        rtmpCamera1.startPreview()
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtmpCamera1!!.isRecording) {
            rtmpCamera1.stopRecord()
            PathUtils.updateGallery(this, folder!!.absolutePath + "/" + currentDateAndTime + ".mp4")
            bRecord.setText(R.string.start_record)
            Toast.makeText(
                this,
                "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                Toast.LENGTH_SHORT
            ).show()
            currentDateAndTime = ""
        }
        if (rtmpCamera1!!.isStreaming) {
            rtmpCamera1!!.stopStream()
            bStartStop.text = resources.getString(R.string.start_button)
        }
        rtmpCamera1!!.stopPreview()
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
//        val action: Int = motionEvent.getAction()

        if (motionEvent != null) {
            val action: Int = motionEvent.action
            if (motionEvent != null) {
                if (motionEvent.getPointerCount() > 1) {
                    if (action == MotionEvent.ACTION_MOVE) {
                        rtmpCamera1?.setZoom(motionEvent)
                    }
                } else if (action == MotionEvent.ACTION_DOWN) {
                    rtmpCamera1!!.tapToFocus(view, motionEvent)
                }
            }
        }
        return true
    }
}