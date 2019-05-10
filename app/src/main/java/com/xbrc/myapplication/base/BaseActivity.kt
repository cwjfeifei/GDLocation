package com.xbrc.myapplication.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import com.gyf.barlibrary.ImmersionBar
import com.xbrc.myapplication.R
import com.xbrc.myapplication.utils.InputUtil

abstract class BaseActivity : AppCompatActivity() {

    lateinit var mContext: AppCompatActivity
    //布局文件id
    protected abstract fun attachLayoutRes(): Int

    //初始化数据
    abstract fun initViewAndData(savedInstanceState: Bundle?)

    override fun onCreate(savedInstanceState: Bundle?) {
        mContext = this
        super.onCreate(savedInstanceState)
        // 无标题
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(attachLayoutRes())
        //着色状态栏
        setStatusBar()
        initViewAndData(savedInstanceState)
    }

    open fun setStatusBar() {
        ImmersionBar.with(mContext).statusBarColor(R.color.white).fitsSystemWindows(true).statusBarDarkFont(true, 0.2f)
            .init()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 必须调用该方法，防止内存泄漏
        ImmersionBar.with(this).destroy()
        InputUtil.fixInputMethodManagerLeak(this)
    }
}