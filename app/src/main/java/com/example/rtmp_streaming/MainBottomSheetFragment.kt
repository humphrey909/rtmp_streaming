package com.example.rtmp_streaming

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pedro.rtplibrary.rtmp.RtmpCamera1

class MainBottomSheetFragment(context: Context, mainActivity:MainActivity) : BottomSheetDialogFragment(), View.OnClickListener {

    private val mContext: Context = context
    private val mainActivity: MainActivity = mainActivity


    private var Type: Int = 0
    private lateinit var view: View

    private lateinit var resolutionTv: TextView
    private lateinit var fpsTv: TextView
    private lateinit var maxnumTv: TextView
    private lateinit var cancelBtn: TextView
    private lateinit var onoffValue: TextView
    private lateinit var broadcastTimeValue: TextView


    private var ResWidthVal: Int = 0
    private var ResHeightVal: Int = 0
    private var FpsVal: Int = 0
    private var MaxNumVal: Int = 0
    private var Visible_Time: String = ""

    private lateinit var subBottomSheetFragment:SubBottomSheetFragment
    private lateinit var rtmpCamera1: RtmpCamera1
    private lateinit var fps_list: MutableList<BottomDialogItem>
//    private lateinit var maxnum_list: MutableList<BottomDialogItem>

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.d("TAG_R", "onCreateView")
        Log.d("TAG_R", "onCreateView!!")
        Log.d("TAG_R", "onCreateView!!!")

        //사용할 뷰 설정
        if(Type == 0){
            view = inflater.inflate(R.layout.streaming_management, container, false)

            cancelBtn = view.findViewById(R.id.cancel_button)
            cancelBtn.setOnClickListener(this)

            broadcastTimeValue = view.findViewById(R.id.broadcast_time_value)

//            Log.d("TAG_R", "Visible_Time")
//            Log.d("TAG_R", Visible_Time)

            if(Visible_Time == ""){
                Log.d("TAG_R", "***Visible_Time - 1")

                broadcastTimeValue.text = "00:00:00"
            }else{
                Log.d("TAG_R", "***Visible_Time - 2")

                broadcastTimeValue.text = Visible_Time
            }


            onoffValue = view.findViewById(R.id.onoff_value)
            if (!rtmpCamera1.isStreaming) { //스트리밍 중이 아닐때
                //방송 상태 변경
                onoffValue!!.text = "오프라인";
                onoffValue!!.setBackgroundResource(R.drawable.stream_off_back)
                onoffValue!!.setTextColor(Color.parseColor("#000000"))
            } else { //스트리밍 중일때
                //방송 상태 변경
                onoffValue!!.text = "생방송";
                onoffValue!!.setBackgroundResource(R.drawable.stream_on_back)
                onoffValue!!.setTextColor(Color.parseColor("#ffffff"))
            }

        }else{
            view = inflater.inflate(R.layout.streaming_setting, container, false)

            resolutionTv = view.findViewById(R.id.resolution_tv)
            fpsTv = view.findViewById(R.id.fps_tv)
            maxnumTv = view.findViewById(R.id.maxnum_tv)

            resolutionTv.setOnClickListener(this)
            fpsTv.setOnClickListener(this)
            maxnumTv.setOnClickListener(this)

            //기본 데이터 세팅
            setViewData()
        }
        //sub메뉴 리스트 강제 생성
        fps_list = mutableListOf()
        fps_list.add(BottomDialogItem("30"))
        fps_list.add(BottomDialogItem("60"))

//        maxnum_list = mutableListOf()
//        for(i:Int in 10..100 step(10)){
//            maxnum_list.add(BottomDialogItem(i.toString()))
//        }



        return view
    }
    fun setType(type: Int){
        if(type == 0){ //방송 관리자
            Type = 0
        }else{ //방송 설정
            Type = 1
        }
    }

    fun setVideoOption(rtmpCamera1: RtmpCamera1, resolution:Int, Fps:Int, MaxNum:Int){
        this.rtmpCamera1 = rtmpCamera1

        if(Type == 1){ //방송 설정
            Log.d("TAG_R", "resolutionSearch: ")
            Log.d("TAG_R", resolution.toString())

            resolutionSearch(resolution);
            this.FpsVal = Fps
            this.MaxNumVal = MaxNum
        }
    }
    fun setTimerOption(Visible_Time:String){
        this.Visible_Time = Visible_Time

//        broadcastTimeValue = view.findViewById(R.id.broadcast_time_value)
        //textview 를 못잡고 에러가 나네.. 어떻게 바꿀까
        broadcastTimeValue.text = Visible_Time
    }

    //subBottomSheet에서 선택한 정보
    fun returnSubData(mainType:String, position: Int){

        if(mainType == "resolution"){
            resolutionSearch(position);
            resolutionTv.text = "${ResWidthVal} X ${ResHeightVal}"

            //Streaming_screen으로 데이터 전송
            mainActivity.setChangeData(mainType, position)
        }else if(mainType == "fps"){
            fpsTv.text = "${fps_list[position].name} fps"
            FpsVal = fps_list[position].name.toInt()

            //Streaming_screen으로 데이터 전송
            mainActivity.setChangeData(mainType, FpsVal)
        }else{

        }
        //else if(mainType == "maxnum"){
//            maxnumTv.text = "${maxnum_list[position].name} 명"
//            MaxNumVal = maxnum_list[position].name.toInt()

            //Streaming_screen으로 데이터 전송
//            streaming_screen.setChangeData(mainType, MaxNumVal)
        //}
    }

    fun returnSubData_Maxnum(mainType:String, value: Int){
        if(mainType == "maxnum"){
            this.MaxNumVal = value
            maxnumTv.text = "${value} 명"
            mainActivity.setChangeData(mainType, MaxNumVal)
        }
    }

    //선택한 해상도 가져오기
    fun resolutionSearch(resolution:Int){
        val resolutionData = this.rtmpCamera1.resolutionsBack[resolution]
        val width = resolutionData.width
        val height = resolutionData.height

        Log.d("TAG_R", "resolutionSearch: ")
        Log.d("TAG_R", width.toString())
        Log.d("TAG_R", height.toString())

        ResWidthVal = width
        ResHeightVal = height
    }

    fun setViewData(){
        resolutionTv.text = "${ResWidthVal} X ${ResHeightVal}"
        fpsTv.text = "${FpsVal} fps"
        maxnumTv.text = "${MaxNumVal} 명"
    }

    //투명설정
    override fun getTheme(): Int = R.style.BottomSheetDialogTheme


    private lateinit var dialog: Dialog
    //다이얼로그 크기 비율 지정
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = super.onCreateDialog(savedInstanceState)
        if(Type == 0) {
            dialog.setOnShowListener { dialogInterface ->
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                setupRatio(bottomSheetDialog)
            }
        }

        return dialog
    }

    private fun setupRatio(bottomSheetDialog: BottomSheetDialog){
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = getBottomSheetDialogDefaultHeight()
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    //높이 비율 지정
    private fun getBottomSheetDialogDefaultHeight(): Int {
        return getWindowHeight() * 90 / 100
    }
    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    override fun onClick(v : View?) {
            if (v != null) {
                when (v.id) {
                    R.id.cancel_button -> {
                        dialog.dismiss()
                    }
                    R.id.resolution_tv -> {
                        if (!rtmpCamera1.isStreaming) { //스트리밍 중이 아닐때
                            val resolution_list: MutableList<BottomDialogItem> = mutableListOf()
                            for (size in rtmpCamera1!!.resolutionsBack) {
                                resolution_list.add(BottomDialogItem(size.width.toString() + "X" + size.height))
                            }

                            MakeList("resolution", resolution_list)
                        }
                    }
                    R.id.fps_tv -> {
                        if (!rtmpCamera1.isStreaming) { //스트리밍 중이 아닐때

                            MakeList("fps",fps_list)
                        }
                    }
                    R.id.maxnum_tv -> {
                        if (!rtmpCamera1.isStreaming) { //스트리밍 중이 아닐때
                            subBottomSheetFragment = SubBottomSheetFragment(mContext, this) //밑에서 나오는 메뉴 뷰
                            subBottomSheetFragment.setMaxNum("maxnum", MaxNumVal);

                            //subBottomSheetFragment 열기
                            activity?.let { subBottomSheetFragment.show(it.supportFragmentManager, subBottomSheetFragment.tag) }
                        }
                    }
                    else -> {}
                }
        } else { //스트리밍 중일때

        }
    }

    fun MakeList(mainType:String,list:MutableList<BottomDialogItem>){



        subBottomSheetFragment = SubBottomSheetFragment(mContext, this) //밑에서 나오는 메뉴 뷰
        subBottomSheetFragment.setRecyclerviewList(mainType, list);

        //subBottomSheetFragment 열기
        activity?.let { subBottomSheetFragment.show(it.supportFragmentManager, subBottomSheetFragment.tag) }
    }
}