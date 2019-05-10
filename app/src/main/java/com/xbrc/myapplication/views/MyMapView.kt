package com.xbrc.myapplication.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.amap.api.mapcore.util.ev
import android.widget.FrameLayout
import com.amap.api.maps.MapView

//主要是处理和NestedScrollView嵌套冲突
class MyMapView : MapView {

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        if (ev.action == MotionEvent.ACTION_DOWN) {
            //请求父控件不拦截触摸事件
            parent.requestDisallowInterceptTouchEvent(true)
        } else if (ev.action == MotionEvent.ACTION_UP) {
            parent.requestDisallowInterceptTouchEvent(false)
        }

        return super.dispatchTouchEvent(ev)
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}
}