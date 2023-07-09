package com.example.rtmp_streaming

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.media.Image
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SubBottomSheetFragment(context: Context, mainBottomSheetFragment:MainBottomSheetFragment) : BottomSheetDialogFragment(), View.OnClickListener {

//    private lateinit var subAdapter: SubMenuRecyclerAdapter
//    private lateinit var subMenuRecyclerAdapter: SubMenuRecyclerAdapter

    private lateinit var subList: MutableList<BottomDialogItem>
    private lateinit var mainType: String

    private val mContext: Context = context
    private val mainBottomSheetFragment: MainBottomSheetFragment = mainBottomSheetFragment

    private lateinit var view: View

    private lateinit var maxnumEt: EditText
    private lateinit var btnAdd: ImageView
    private lateinit var btnMinus: ImageView
    private lateinit var cancelBtn: TextView
    private lateinit var saveBtn: TextView

    private var count: Int = 0
    private lateinit var recyclerView: RecyclerView

//    private lateinit var resolutionTv: TextView
//    private lateinit var fpsTv: TextView
//    private lateinit var maxnumTv: TextView
//
//    private var ResWidthVal: Int = 0
//    private var ResHeightVal: Int = 0
//    private var FpsVal: Int = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("TAG_R", "onCreateView: ")

        //사용할 뷰 설정
        if(this.mainType == "maxnum"){ //인원일때만
            view = inflater.inflate(R.layout.max_num_setting, container, false)

            maxnumEt = view.findViewById(R.id.maxnum_et)
            btnAdd = view.findViewById(R.id.btn_add)
            btnMinus = view.findViewById(R.id.btn_minus)
            cancelBtn = view.findViewById(R.id.cancel_btn)
            saveBtn = view.findViewById(R.id.save_btn)
            btnAdd.setOnClickListener(this)
            btnMinus.setOnClickListener(this)
            cancelBtn.setOnClickListener(this)
            saveBtn.setOnClickListener(this)


        }else{ //리사이클러뷰
            view = inflater.inflate(R.layout.sub_bottomsheet, container, false)

            recyclerView = view.findViewById<View>(R.id.recyclerView) as RecyclerView
        }
        return view
    }
    fun setMaxNum(mainType: String, maxNum: Int){
        this.mainType = mainType
        count = maxNum
    }

    fun setRecyclerviewList(mainType:String, subList: MutableList<BottomDialogItem>){
        Log.d("TAG_R", "list: ")
        this.mainType = mainType
        this.subList = subList
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("TAG_R", "onViewCreated: ")



        if(this.mainType == "maxnum"){ //인원일때만
            maxnumEt.setText("$count")
        }else { //리사이클러뷰
            Log.d("TAG_R", "adapter: ")
            val linearManager = LinearLayoutManager(activity) //그리드 매니저 선언

            val SubAdapter = SubMenuRecyclerAdapter()
            recyclerView.setLayoutManager(linearManager) //리사이클러뷰 + 그리드 매니저 = 만들 형식

            SubAdapter.setItem(subList)
            recyclerView.adapter = SubAdapter


            //리스너를 통해서 recyclerview에서 클릭한 이벤트 정보 값을 activity에서 받음.
            SubAdapter.setOnItemClickListener(object : SubMenuRecyclerAdapter.OnItemClickListener {
                override fun onItemClick(v: View?, position: Int) {

                    //클릭시
                    Log.d("onItemClick", position.toString());

                    //클릭시 데이터 받아와서 해당 데이터를 적용시켜줘야함.
                    //현재 열려 있는 SubBottomSheetFragment를 닫아야함
                    dialog?.dismiss() //

                    //mainBottomSheet로 선택한 번호를 리턴해줌
                    mainBottomSheetFragment.returnSubData(mainType, position);
                }
            })
        }
    }

    //투명설정
    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    //다이얼로그 크기 비율 지정
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
//        if(Type == 0) {
//            dialog.setOnShowListener { dialogInterface ->
//                val bottomSheetDialog = dialogInterface as BottomSheetDialog
//                setupRatio(bottomSheetDialog)
//            }
//        }

        return dialog
    }
//    private fun setupRatio(bottomSheetDialog: BottomSheetDialog){
//        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
//        val behavior = BottomSheetBehavior.from(bottomSheet)
//        val layoutParams = bottomSheet.layoutParams
//        layoutParams.height = getBottomSheetDialogDefaultHeight()
//        bottomSheet.layoutParams = layoutParams
//        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//    }
//
//    //높이 비율 지정
//    private fun getBottomSheetDialogDefaultHeight(): Int {
//        return getWindowHeight() * 85 / 100
//    }
//    private fun getWindowHeight(): Int {
//        // Calculate window height for fullscreen use
//        val displayMetrics = DisplayMetrics()
//        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
//        return displayMetrics.heightPixels
//    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btn_add -> {
                    count++
                    maxnumEt.setText("$count")
                }
                R.id.btn_minus -> {
                    if(count != 0){
                        count--
                        maxnumEt.setText("$count")
                    }
                }
                R.id.cancel_btn -> {
                    dialog?.dismiss()
                }
                R.id.save_btn -> {
                    //저장하는 로직

                    dialog?.dismiss()
                    mainBottomSheetFragment.returnSubData_Maxnum(mainType, count);
                }


                else -> {}
            }
        }
    }

}