package com.example.rtmp_streaming

//import kotlinx.coroutines.flow.internal.NoOpContinuation.context
import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import com.example.rtmp_streaming.utils.PathUtils
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener, ConnectCheckerRtmp,
    SurfaceHolder.Callback, PopupMenu.OnMenuItemClickListener,
    View.OnTouchListener {

    private var rtmpip:String? = "rtmp://43.201.165.228/live/test2"; //src

    private lateinit var rtmpCamera1: RtmpCamera1
    private lateinit var bStartStop: ImageView
    private lateinit var menuBtn: ImageView
    private lateinit var broadcastTimeValue: TextView
    private var onoffValue: TextView? = null
    private var tvBitrate: TextView? = null
    private lateinit var mikeBtn: ImageView

    //영상 스트림시 보낼 정보
    private var User: String = ""
    private var Password: String = ""
    private var Resolution: Int = 0 // rtmpCamera1!!.resolutionsBack에서 가능한 해상도 리스트 생성
    private var Fps: Int = 30
    private var AudioBitrate: Int = 128
    private var VideoBitrate: Int = 2500
    private var SampleRate: Int = 44100
    private var Channel: Boolean = true //rtmpCamera1!!.prepareAudio -> rgChannel!!.checkedRadioButtonId
    private var EchoCanceler: Boolean = true
    private var NoiseSuppressor: Boolean = true
    private var MaxNum: Int = 100

    private lateinit var onoffBox: LinearLayout
    private lateinit var broadcastFuncArea: LinearLayout

    private lateinit var bCloseArea: LinearLayout
    private lateinit var bFunctionBox: LinearLayout
    private lateinit var sCloseBtn: ImageView
    private lateinit var bLiveArea: LinearLayout
    private lateinit var actioninfoBox: LinearLayout

    private lateinit var mainBottomSheetFragment:MainBottomSheetFragment
    var MenuOpenTimer:Int = 0 //메뉴 오픈 체크

    private var folder: File? = null
    private var currentDateAndTime = ""

    var TimerHandler: Handler? = null // 스트림 핸들러
    var TimerThread: Thread? = null // 스트림 스레드
    var initTimer:Int = 0 //타이머 초기화 변수
    var StreamTime:Int = 0 // 스레드로 인해 변화되는 타이머 변수
    var StreamComplete:Int = 0 // 라이브 스트림 종료 여부
    var Visible_Time:String = "" //변환된 타이머 시간

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
        setContentView(R.layout.activity_streaming_screen)

        requestPermissions() //카메라 마이크 파일 권한 설정

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        folder = PathUtils.getRecordPath()

        //surfaceView때문에 바로 버튼이 안눌리는 문제 있음!!!!!!!!!
        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView_n)
        surfaceView.holder.addCallback(this)
        surfaceView.setOnTouchListener(this)
        rtmpCamera1 = RtmpCamera1(surfaceView, this)

//        prepareOptionsMenuViews() //옵션설정 목록, 공간 만들어주는 부분

        menuBtn = findViewById(R.id.menu_btn)
        menuBtn.setOnClickListener(this)

        broadcastTimeValue = findViewById(R.id.broadcast_time_value)

        broadcastFuncArea = findViewById(R.id.broadcast_func_area) //마이크 카메라 전환 메뉴를 감싸는 뷰
        mainBottomSheetFragment = MainBottomSheetFragment(applicationContext, this) //밑에서 나오는 메뉴 뷰

        mikeBtn = findViewById(R.id.mike_btn)
        mikeBtn.setOnClickListener(this)

        onoffValue = findViewById(R.id.onoff_value)
        tvBitrate = findViewById(R.id.tv_bitrate)

        bFunctionBox = findViewById(R.id.broadcast_function_box)

        bCloseArea = findViewById(R.id.broadcast_close_area)
        bCloseArea.setOnClickListener(this)

        onoffBox = findViewById(R.id.onoff_box)
        onoffBox.setOnClickListener(this)

        sCloseBtn = findViewById(R.id.stream_close_btn)
        actioninfoBox = findViewById(R.id.actioninfo_box)

        bLiveArea = findViewById(R.id.broadcast_live_area)
        bLiveArea.setOnClickListener(this)

        bStartStop = findViewById(R.id.b_start_stop)

        val switchCamera = findViewById<ImageView>(R.id.switch_cam)
        switchCamera.setOnClickListener(this)

        //핸들러 생성
        MakeHandler()
    }

    //스트림 메뉴 클릭
    private fun showMenu(v: View) {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenuStyle)
        val popup = PopupMenu( wrapper, broadcastFuncArea, Gravity.CENTER, 0, R.style.PopupMenuPosition )
//        menuInflater.inflate(R.menu.sub_menu, popup.menu);

        val PMenu: Menu = popup.menu
        changeTitleName("방송 관리자", 0, PMenu)
        changeTitleName("방송 설정", 1, PMenu)

        popup.setOnMenuItemClickListener( this );
        popup.show()
    }

    //팝업 메뉴 객체 가운데 정렬하는 부분
    private fun changeTitleName(addMenuName:String, itemId:Int, PMenu: Menu){

        //add(groupId, itemId, order, title)
        val menuItem: MenuItem = PMenu.add(0, itemId, 0, addMenuName)

        val spannableString = SpannableString(menuItem.title)
        spannableString.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            spannableString.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        menuItem.setTitle(spannableString)
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

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.broadcast_live_area -> {
                    Log.d("TAG_R", "b_start_stop: ")
                    if (!rtmpCamera1.isStreaming) { //스트리밍 중이 아닐때
//                        bStartStop.text = resources.getString(R.string.stop_button)
                        bStartStop.setImageResource(R.drawable.stream_stop)
                        bLiveArea.setBackgroundResource(R.drawable.stream_stop_back)

                        bCloseArea.setVisibility(View.INVISIBLE)//닫기 버튼 없애기
                        actioninfoBox.setVisibility(View.INVISIBLE)//해당 경매 정보 없애기

                        //방송 상태 변경
                        onoffValue!!.text = "생방송";
                        onoffValue!!.setBackgroundResource(R.drawable.stream_on_back)
                        onoffValue!!.setTextColor(Color.parseColor("#ffffff"))


                        val user = User
                        val password = Password
                        if (!user.isEmpty() && !password.isEmpty()) {
                            rtmpCamera1!!.setAuthorization(user, password)
                        }


                        if (rtmpCamera1!!.isRecording || prepareEncoders()) {

                            //밑으로 내리는 애니메이션
                            ObjectAnimator.ofFloat(bFunctionBox, "translationY", 150f).apply {
                                duration = 1000
                                start()
                            }

                            //stream 주소 - 스트리밍 시작
                            rtmpCamera1!!.startStream(rtmpip)
                            StreamComplete = 1 // 라이브 스트림 종료 여부
                            //스레드 타이머 시작
                            StreamTimerThread()

                        } else {
                            //If you                                    see this all time when you start stream,
                            //it is because your encoder device dont support the configuration
                            //in video encoder maybe color format.
                            //If you have more encoder go to VideoEncoder or AudioEncoder class,
                            //change encoder and try
                            Toast.makeText(
                                this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT
                            ).show()

                            bStartStop.setImageResource(R.drawable.stream_start);
                            bLiveArea.setBackgroundResource(R.drawable.stream_start_back);

                            bCloseArea.setVisibility(View.VISIBLE) // 닫기 버튼 생성
                            actioninfoBox.setVisibility(View.VISIBLE)//해당 경매 정보 생성

                            //방송 상태 변경
                            onoffValue!!.text = "오프라인";
                            onoffValue!!.setBackgroundResource(R.drawable.stream_off_back)
                            onoffValue!!.setTextColor(Color.parseColor("#000000"))

                            //밑으로 내리는 애니메이션
                            ObjectAnimator.ofFloat(bFunctionBox, "translationY", 0f).apply {
                                duration = 1000
                                start()
                            }

                            StreamComplete = 0
                        }
                    } else { //스트리밍 중일때
                        StreamComplete = 0

                        //밑으로 내리는 애니메이션
                        ObjectAnimator.ofFloat(bFunctionBox, "translationY", 0f).apply {
                            duration = 1000
                            start()
                        }

                        bStartStop.setImageResource(R.drawable.stream_start);
                        bLiveArea.setBackgroundResource(R.drawable.stream_start_back);

                        bCloseArea.setVisibility(View.VISIBLE) // 닫기 버튼 생성
                        actioninfoBox.setVisibility(View.VISIBLE)//해당 경매 정보 생성

                        //방송 상태 변경
                        onoffValue!!.text = "오프라인";
                        onoffValue!!.setBackgroundResource(R.drawable.stream_off_back)
                        onoffValue!!.setTextColor(Color.parseColor("#000000"))

                        rtmpCamera1!!.stopStream()
                    }
                }

                R.id.b_record -> {
//                    Log.d("TAG_R", "b_start_stop: ")
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                        if (!rtmpCamera1!!.isRecording) {
//                            try {
//                                if (!folder!!.exists()) {
//                                    folder!!.mkdir()
//                                }
//                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//                                currentDateAndTime = sdf.format(Date())
//                                if (!rtmpCamera1!!.isStreaming) {
//                                    if (prepareEncoders()) {
//                                        rtmpCamera1!!.startRecord(
//                                            folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
//                                        )
//                                        bRecord.setText(R.string.stop_record)
//                                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
//                                    } else {
//                                        Toast.makeText(
//                                            this, "Error preparing stream, This device cant do it",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                    }
//                                } else {
//                                    rtmpCamera1!!.startRecord(
//                                        folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
//                                    )
//                                    bRecord.setText(R.string.stop_record)
//                                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
//                                }
//                            } catch (e: IOException) {
//                                rtmpCamera1!!.stopRecord()
//                                PathUtils.updateGallery(
//                                    this,
//                                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
//                                )
//                                bRecord.setText(R.string.start_record)
//                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//                            }
//                        } else {
//                            rtmpCamera1!!.stopRecord()
//                            PathUtils.updateGallery(
//                                this,
//                                folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
//                            )
//                            bRecord.setText(R.string.start_record)
//                            Toast.makeText(
//                                this,
//                                "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
//                                Toast.LENGTH_SHORT
//                            ).show()
//                            currentDateAndTime = ""
//                        }
//                    } else {
//                        Toast.makeText(
//                            this, "You need min JELLY_BEAN_MR2(API 18) for do it...",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
                }

                R.id.switch_cam -> try {
                    rtmpCamera1!!.switchCamera()
                } catch (e: CameraOpenException) {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }

                R.id.broadcast_close_area -> {
                    finish();
                }
                R.id.mike_btn -> {
                    if (!rtmpCamera1.isAudioMuted) {
                        mikeBtn.setImageResource(R.drawable.mike_off)
//                        item.icon = resources.getDrawable(R.drawable.icon_microphone_off)
                        rtmpCamera1.disableAudio()
                    } else {
                        mikeBtn.setImageResource(R.drawable.mike_on)
//                        item.icon = resources.getDrawable(R.drawable.icon_microphone)
                        rtmpCamera1.enableAudio()
                    }
                }
                R.id.menu_btn -> {
                    showMenu(v)
                }
                else -> {}
            }
        }

    }

    override fun onAuthErrorRtmp() {
        runOnUiThread { Toast.makeText(this@MainActivity, "Auth error", Toast.LENGTH_SHORT).show() }
    }

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    //연결 실패하였을 경우
    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {


            //실패 이유 : 작성

            Toast.makeText(this@MainActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
                .show()
            rtmpCamera1!!.stopStream()

            //밑으로 내리는 애니메이션
            ObjectAnimator.ofFloat(bFunctionBox, "translationY", 0f).apply {
                duration = 1000
                start()
            }

            bStartStop.setImageResource(R.drawable.stream_start);
            bLiveArea.setBackgroundResource(R.drawable.stream_start_back);

            bCloseArea.setVisibility(View.VISIBLE) // 닫기 버튼 생성
            actioninfoBox.setVisibility(View.VISIBLE)//해당 경매 정보 생성

            //방송 상태 변경
            onoffValue!!.text = "오프라인";
            onoffValue!!.setBackgroundResource(R.drawable.stream_off_back)
            onoffValue!!.setTextColor(Color.parseColor("#000000"))

            rtmpCamera1!!.stopStream()

            StreamComplete = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtmpCamera1!!.isRecording
            ) {
                rtmpCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
//                bRecord.setText(R.string.start_record)
                Toast.makeText(
                    this@MainActivity,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
    }

    override fun onConnectionStartedRtmp(rtmpUrl: String) {
    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
//            Toast.makeText(this@Streaming_screen, "Connection success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
//            Toast.makeText(this@Streaming_screen, "Disconnected", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtmpCamera1!!.isRecording
            ) {
                rtmpCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                Toast.makeText(
                    this@MainActivity,
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
//        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        rtmpCamera1.startPreview()
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtmpCamera1!!.isRecording) {
            rtmpCamera1.stopRecord()
            PathUtils.updateGallery(this, folder!!.absolutePath + "/" + currentDateAndTime + ".mp4")
//            bRecord.setText(R.string.start_record)
            Toast.makeText(
                this,
                "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                Toast.LENGTH_SHORT
            ).show()
            currentDateAndTime = ""
        }
        if (rtmpCamera1!!.isStreaming) {
            rtmpCamera1!!.stopStream()
            bStartStop.setImageResource(R.drawable.stream_start);
            bLiveArea.setBackgroundResource(R.drawable.stream_start_back);

        }
        rtmpCamera1!!.stopPreview()
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
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

    private fun prepareEncoders(): Boolean {
        val resolution = rtmpCamera1!!.resolutionsBack[Resolution]
        val width = resolution.width
        val height = resolution.height

        Log.d("TAG_R", "prepareEncoders: ")
        Log.d("TAG_R", width.toString())
        Log.d("TAG_R", height.toString())

        //기본 값으로 세팅
        return rtmpCamera1!!.prepareVideo(
            width, height, Fps,
            VideoBitrate * 1024,
            CameraHelper.getCameraOrientation(this)
        ) && rtmpCamera1!!.prepareAudio(
            AudioBitrate * 1024, SampleRate,
            Channel, EchoCanceler,
            NoiseSuppressor
        )
    }

    //메뉴에서 하위 메뉴 클릭시 동작
    override fun onMenuItemClick(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            0 -> { //방송 관리자
                mainBottomSheetFragment.setType(0)
                mainBottomSheetFragment.setVideoOption(rtmpCamera1, Resolution, Fps, MaxNum)
                mainBottomSheetFragment.show(supportFragmentManager, mainBottomSheetFragment.tag)

                MenuOpenTimer = 1
            }

            1-> { //방송 설정
                mainBottomSheetFragment.setType(1)
                mainBottomSheetFragment.setVideoOption(rtmpCamera1, Resolution, Fps, MaxNum)
                mainBottomSheetFragment.show(supportFragmentManager, mainBottomSheetFragment.tag)
            }
        }

        return false
    }


    //video 설정 값에 대한 변수 변경
    fun setChangeData(type:String, value:Int) {
        if(type == "resolution"){
            Resolution = value
        }else if(type == "fps"){
            Fps = value
        }else if(type == "maxnum"){
            MaxNum = value
        }
    }

    //스레드가 먼저 실행
    fun StreamTimerThread() {
        TimerThread = object : Thread() {
            override fun run() {
                while (true) {
                    Log.d("TAG_R", "***3")
//                    Log.d("TAG_R", StreamTime.toString())

                    try {
                        val msg: Message = TimerHandler!!.obtainMessage()
                        if(StreamComplete == 0){ //종료 되었을때
                            TimerThread?.interrupt()
//                            Log.d("TAG_R", "***4")
                            StreamTime = initTimer// 시간 초기화

                            msg.what = 0
                            msg.arg1 = StreamTime
                            TimerHandler!!.sendMessage(msg);

//                            Log.d("TAG_R", "***5")
//                            Log.d("TAG_R", isInterrupted.toString()) //true

                            //인터럽트로 스레드 끝내기.
                            if(isInterrupted){
                                Log.d("TAG_R", "인터럽트 발생")
                                Log.d("TAG_R", "자원정리")
                                break
                            }
                        }else{
                            StreamTime++

                            msg.what = 1
                            // msg.obj = "timer";
                            msg.arg1 = StreamTime
                            msg.arg2 = StreamComplete
                            TimerHandler?.sendMessage(msg)
                            sleep(1000)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }catch(e:InterruptedException){
                        Log.d("TAG_R", "InterruptedException 인터럽트 발생")
                        Log.d("TAG_R", "InterruptedException 자원정리")
                    }
                }
            }
        }
        (TimerThread as Thread).start()
    }

    fun MakeHandler() {

        //광고 이미지 변경 핸들러
        TimerHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == 1) {
                    Log.d("TAG_R", "Handler what 1")
                    if (msg.arg2 == 1) {
                        val timerval: String = timertrans(msg.arg1) //변환된 타이머 문자 가져옴
                        broadcastTimeValue.setText(timerval)
                        if(MenuOpenTimer == 1){
                            mainBottomSheetFragment.setTimerOption(Visible_Time)
                        }
                    }
                }
                else{
                    Log.d("TAG_R", "Handler what 0")
                    val timerval: String = timertrans(msg.arg1)
                    broadcastTimeValue.setText(timerval)
                }
            }
        }
    }

    //타이머 변환기
    fun timertrans(timernum: Int): String {
        var hour: Int = timernum / (60 * 60)
        var minute: Int = timernum/60-(hour*60)
        var second: Int = timernum % 60


        var hourVal: String? = null
        var minuteVal: String? = null
        var secondVal: String? = null
        if(hour.toString().length == 1){
            hourVal = "0${hour}"
        }else{
            hourVal = hour.toString()
        }

        if(minute.toString().length == 1){
            minuteVal = "0${minute}"
        }else{
            minuteVal = minute.toString()
        }

        if(second.toString().length == 1){
            secondVal = "0${second}"
        }else{
            secondVal = second.toString()
        }

        Visible_Time = "$hourVal:$minuteVal:$secondVal"

        return "$hourVal:$minuteVal:$secondVal"
    }
}