package com.xbrc.myapplication.activity

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.services.help.Tip
import com.tbruyelle.rxpermissions2.RxPermissions
import com.xbrc.myapplication.R
import com.xbrc.myapplication.bean.PositionItem
import kotlinx.android.synthetic.main.activity_location.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_start_location.setOnClickListener {
            val rxPermissions = RxPermissions(this)
            val permissions: Array<String> = arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE
            )
            rxPermissions.request(*permissions)
                    .subscribe { aBoolean ->
                        when (aBoolean) {
                            true -> {
                                startActivityForResult<LocationActivity>(LocationActivity.POSITION_REQUEST_CODE)
                            }
                            false -> {
                                toast("请开启权限")
                            }
                        }
                    }

        }
    }

    //接收返回的数据
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LocationActivity.POSITION_REQUEST_CODE && resultCode == RESULT_OK) {
            val positionItem = data!!.getParcelableExtra<PositionItem>(LocationActivity.SURE_POSITION)
            info("选择的位置为====$positionItem")
            tv_address.text = "选择的位置为====$positionItem"
        }
    }
}
