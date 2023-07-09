package com.example.rtmp_streaming

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class SubMenuRecyclerAdapter : RecyclerView.Adapter<SubMenuRecyclerAdapter.Holder>() {
    private var itemList: MutableList<BottomDialogItem> = ArrayList()


    //int listType = 0; // 1 : 성격리스트,  2: 과목리스트
    //리스너를 통해서 activity로 값을 전달함.
    interface OnItemClickListener {
        fun onItemClick(v: View?, position: Int)
    }
    // 리스너 객체 참조를 저장하는 변수
    private var mListener: SubMenuRecyclerAdapter.OnItemClickListener? = null
    fun setOnItemClickListener(listener:OnItemClickListener) {
        mListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubMenuRecyclerAdapter.Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sub_recyclerview, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: SubMenuRecyclerAdapter.Holder, position: Int) {
//        Log.d("TAG_R", "onBindViewHolder")
//        Log.d("TAG_R", itemList[position].toString())

        val item = itemList[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int {

//        Log.d("TAG_R", "getItemCount")
//        Log.d("TAG_R", itemList.size.toString())

        return itemList.size
    }

    fun setItem(items: MutableList<BottomDialogItem>) {
        if (!items.isNullOrEmpty()) {
            itemList = items
//            Log.d("TAG_R", "items: ")
//            Log.d("TAG_R", itemList.toString())

            notifyDataSetChanged()
        }
    }


    inner class Holder(val view: View) : RecyclerView.ViewHolder(view) {

        private val textView: TextView = itemView.findViewById(R.id.textView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    //클릭시 SubBottomSheetFragment페이지로 데이터 이동
                    mListener?.onItemClick(view, position)
                }
            }
        }

        fun bind(item: BottomDialogItem, position:Int) {
            textView.text = item.name
        }
    }



}