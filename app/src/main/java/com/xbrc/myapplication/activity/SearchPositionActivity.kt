package com.xbrc.myapplication.activity

import android.app.Activity
import android.os.Bundle
import com.xbrc.myapplication.base.BaseActivity
import kotlinx.android.synthetic.main.activity_search_position.*
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import android.view.inputmethod.EditorInfo
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.amap.api.mapcore.util.it
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.xbrc.myapplication.bean.PositionItem
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.Tip
import com.xbrc.myapplication.R
import com.xbrc.myapplication.views.Linker


class SearchPositionActivity : BaseActivity(), AnkoLogger {
    override fun attachLayoutRes(): Int {
        return R.layout.activity_search_position
    }

    companion object {
        const val DELAY_MESSAGE = 2003
    }

    //搜索适配器
    private lateinit var adapter: SearchAdapter
    //搜索列表数据
    private var mSearchList: MutableList<Tip> = ArrayList()

    override fun initViewAndData(savedInstanceState: Bundle?) {
        //列表初始化
        rlv_search.layoutManager = LinearLayoutManager(this)
        adapter = SearchAdapter(mSearchList)
        val notDataView = layoutInflater.inflate(R.layout.view_data_empty, rlv_search.parent as ViewGroup, false)
        adapter.emptyView = notDataView
        adapter.isUseEmpty(false)
        rlv_search.adapter = adapter
        //点击键盘的搜索按钮
        et_search.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mSearchList.clear()
                adapter.setNewData(mSearchList)
                //开始搜索
                val search_string = et_search.text.toString()
                getInputquery(search_string)
                // 当按了搜索之后关闭软键盘
                (mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                    mContext.currentFocus!!.windowToken, HIDE_NOT_ALWAYS
                )
            }
            false
        }
        //自动弹出软键盘
        openSoftKey(et_search)
        //返回
        iv_title_back.setOnClickListener { finish() }
        //搜索
        et_search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                handler.removeMessages(DELAY_MESSAGE)
                val msg = handler.obtainMessage()
                msg.what = DELAY_MESSAGE
                msg.obj = s.toString()
                handler.sendMessageDelayed(msg, 500)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        //列表点击事件
        adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            val data = Intent()
            data.putExtra(LocationActivity.SELECT_POSITION, mSearchList[position])
            setResult(Activity.RESULT_OK, data)
            finish()
        }

    }

    val handler = Handler {
        val search_string = (it.obj as String)
        if (search_string.isNotEmpty()) {
            getInputquery(search_string)
        } else {
            mSearchList.clear()
            adapter.setNewData(mSearchList)
        }
        true
    }

    //自动弹出软键盘
    fun openSoftKey(editText: EditText) {
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.requestFocus()
        val inputManager = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(editText, 0)
    }

    //输入内容自动提示
    private fun getInputquery(keyWord: String) {
        //第二个参数传入null或者“”代表在全国进行检索，否则按照传入的city进行检索
        val inputquery = InputtipsQuery(keyWord, "")
        inputquery.cityLimit = false//限制在当前城市
        val inputTips = Inputtips(this, inputquery)
        inputTips.setInputtipsListener { p0, p1 ->
            mSearchList.clear()
            if (p1 == 1000 && p0.isNotEmpty()) {
                p0.filter { it.point != null || it.poiID.isNotEmpty() }.forEach {
                    info("搜索到的数据为===$it")
                    mSearchList.add(it)
                }
            }
            adapter.setSearchText(keyWord)
            adapter.isUseEmpty(mSearchList.isEmpty())
            adapter.setNewData(mSearchList)
        }
        inputTips.requestInputtipsAsyn()
    }

    //搜索列表适配器
    class SearchAdapter(data: List<Tip>) : BaseQuickAdapter<Tip, BaseViewHolder>(R.layout.item_search, data) {
        private var search_string = ""
        fun setSearchText(search_string: String) {
            this.search_string = search_string
        }

        override fun convert(helper: BaseViewHolder, item: Tip) {
            helper.apply {
                setText(R.id.tv_search_name_dec, item.district + item.address)
                //文字提示
                val androidRules = arrayOf(search_string)
                Linker.Builder()
                    .content(item.name)
                    .textView(getView(R.id.tv_search_name))
                    .links(androidRules)
                    .linkColor(ContextCompat.getColor(mContext, R.color.colorAccent))
                    .apply()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.let {
            handler.removeCallbacksAndMessages(0)
        }
    }
}
