package com.xbrc.myapplication.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.ViewGroup
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.AlphaAnimation
import com.amap.api.maps.model.animation.Animation
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.help.Tip
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.xbrc.myapplication.bean.PositionItem
import com.xbrc.myapplication.R
import com.xbrc.myapplication.base.BaseActivity
import kotlinx.android.synthetic.main.activity_location.*
import kotlinx.android.synthetic.main.activity_location.iv_title_back
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast


class LocationActivity : BaseActivity(), AnkoLogger {
    companion object {
        const val SEARCH_REQUEST_CODE = 2001
        const val POSITION_REQUEST_CODE = 2002
        const val SELECT_POSITION = "SELECT_POSITION"
        const val SURE_POSITION = "SURE_POSITION"
    }

    //布局
    override fun attachLayoutRes(): Int {
        return R.layout.activity_location
    }

    //地图交互核心方法
    private lateinit var aMap: AMap
    //地图中心点
    private lateinit var centerMaker: Marker
    //定位适配器
    private lateinit var adapter: LocationAdapter
    //定位列表数据
    private var mLocationList: MutableList<PositionItem> = ArrayList()
    //判断是来自地图还是列表的点击
    private var isTouch = false //false 不进行附件搜索

    //初始化界面数据
    override fun initViewAndData(savedInstanceState: Bundle?) {
        map_view.onCreate(savedInstanceState)
        aMap = map_view.map
        //地图初始化
        initMap(aMap)
        //列表初始化
        rlv_locations.layoutManager = LinearLayoutManager(this)
        adapter = LocationAdapter(mLocationList)
        val notDataView = layoutInflater.inflate(R.layout.view_data_empty, rlv_locations.parent as ViewGroup, false)
        adapter.emptyView = notDataView
        adapter.isUseEmpty(false)
        rlv_locations.adapter = adapter
        //列表点击事件
        adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
            if (mLocationList.isEmpty()) return@OnItemClickListener
            isTouch = false
            //地图移动
            val latLng = LatLng(mLocationList[position].latLonPoint.latitude, mLocationList[position].latLonPoint.longitude)
            aMap.animateCamera(CameraUpdateFactory.changeLatLng(latLng))
            //更改列表的选中
            mLocationList.forEach {
                it.isSelect = false
            }
            mLocationList[position].isSelect = true
            adapter.notifyDataSetChanged()
            //更改定位图标
            if (mLocationList[position].id == "当前位置") {
                iv_my_location.setBackgroundResource(R.drawable.ic_my_location_sel)
            } else {
                iv_my_location.setBackgroundResource(R.drawable.ic_my_location)
            }
        }
        //设置地图拖动监听
        aMap.setOnMapTouchListener {
            isTouch = true
        }
        aMap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChangeFinish(cameraPosition: CameraPosition) {
                if (isTouch) {
                    val latLng = cameraPosition.target
                    mLocationList.clear()
                    adapter.setNewData(mLocationList)
                    //查找附近数据
                    searchNearby(latLng)
                }
            }

            override fun onCameraChange(p0: CameraPosition?) {
            }

        })
        //定位按钮的点击事件
        iv_my_location.setOnClickListener {
            //获取当前位置
            getCurrentLocation { locationSuccess(it) }
        }
        //搜索按钮点击事件
        iv_position_search.setOnClickListener {
            startActivityForResult<SearchPositionActivity>(SEARCH_REQUEST_CODE)
        }
        //返回键
        iv_title_back.setOnClickListener { finish() }
        //获取当前位置
        getCurrentLocation { locationSuccess(it) }
        //发送当前位置
        tv_send_position.setOnClickListener {
            if (mLocationList.isNotEmpty()) {
                val positionItem = mLocationList.filter { it.isSelect }[0]
                positionItem.let {
                    val data = Intent()
                    data.putExtra(SURE_POSITION, positionItem)
                    setResult(Activity.RESULT_OK, data)
                }
            }
            finish()
        }
    }

    //接收返回的数据
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            val resultTip = data!!.getParcelableExtra<Tip>(SELECT_POSITION)
            //清空列表数据
            mLocationList.clear()
            adapter.setNewData(mLocationList)
            isTouch = false
            val latLng = LatLng(resultTip.point.latitude, resultTip.point.longitude)
            //地图移动
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            //更改定位图标
            iv_my_location.setBackgroundResource(R.drawable.ic_my_location)
            //构造列表的第一个定位数据
            val poiItem = PositionItem("", LatLonPoint(resultTip.point.latitude, resultTip.point.longitude), resultTip.name, if (resultTip.address.isEmpty()) resultTip.district else resultTip.address )
            poiItem.isSelect = true
            poiItem.isMyLocation = true
            mLocationList.add(poiItem)
            //查找附近的数据
            searchNearby(latLng)
        }
    }

    //获取当前位置
    private fun getCurrentLocation(success: (AMapLocation) -> Unit) {
        iv_my_location.setBackgroundResource(R.drawable.ic_my_location_sel)
        //清空列表数据
        mLocationList.clear()
        adapter.setNewData(mLocationList)
        val mLocationClient = AMapLocationClient(this)
        val mLocationOption = AMapLocationClientOption()
        //高精度模式
        mLocationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        //定位请求超时时间
        mLocationOption.httpTimeOut = 50000
        // 关闭缓存机制
        mLocationOption.isLocationCacheEnable = false
        // 设置是否只定位一次
        mLocationOption.isOnceLocation = true
        //设置参数
        mLocationClient.setLocationOption(mLocationOption)
        // 启动定位
        mLocationClient.startLocation()
        //定位监听
        mLocationClient.setLocationListener { aMapLocation ->
            //定位成功之后取消定位
            mLocationClient.stopLocation()
            if (aMapLocation != null && aMapLocation.errorCode == 0) {
                info("地址成功===" + aMapLocation.toStr())
                success(aMapLocation)
            } else {
                toast("定位失败，请重新定位")
            }
        }
    }

    //定位列表适配器
    class LocationAdapter(data: List<PositionItem>) :
            BaseQuickAdapter<PositionItem, BaseViewHolder>(R.layout.item_location, data) {
        override fun convert(helper: BaseViewHolder, item: PositionItem) {
            helper.apply {
                setText(R.id.tv_location_name, item.poiName)
                setText(R.id.tv_location_name_dec, if (item.isMyLocation) item.address else item.provinceName + item.cityName + item.address)
                setGone(R.id.iv_position_select, item.isSelect)
            }
        }
    }

    //定位成功之后，构造Marker对象和数据
    private fun locationSuccess(location: AMapLocation) {
        val latLng = LatLng(location.latitude, location.longitude)
        //定位蓝点
        val marker_location_point = LayoutInflater.from(this).inflate(R.layout.marker_location_point, map_view, false)
        val marker = aMap.addMarker(MarkerOptions().position(latLng).title("").snippet("").icon(BitmapDescriptorFactory.fromView(marker_location_point)))
        //蓝点动画
        val alpha = AlphaAnimation(1f, 0.5f)//新建透明度动画
        alpha.setDuration(1000)//设置动画持续时间
        alpha.repeatCount = -1
        alpha.repeatMode = Animation.REVERSE
        marker.setAnimation(alpha)//图片设置动画
        marker.startAnimation()//开始动画
        //定位针
        val marker_position_needle = LayoutInflater.from(this).inflate(R.layout.marker_position_needle, map_view, false)
        centerMaker = aMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromView(marker_position_needle)).position(latLng).draggable(true).title("").snippet(""))
        //定位针一直在中心
        centerMaker.setPositionByPixels(map_view.width / 2, map_view.height / 2)
        //移动地图
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        //构造列表的第一个定位数据
        val poiItem =
                PositionItem("当前位置", LatLonPoint(location.latitude, location.longitude), location.poiName, location.address)
        poiItem.provinceName = location.province
        poiItem.cityName = location.city
        poiItem.adName = location.adCode
        poiItem.cityCode = location.cityCode
        poiItem.isSelect = true
        poiItem.isMyLocation = true
        mLocationList.add(poiItem)
        //查找附近的数据
        searchNearby(latLng)
    }

    //查找附近的数据
    private fun searchNearby(latLng: LatLng) {
        //keyWord表示搜索字符串，
        //第二个参数表示POI搜索类型，二者选填其一，选用POI搜索类型时建议填写类型代码，码表可以参考下方（而非文字）
        //cityCode表示POI搜索区域，可以是城市编码也可以是城市名称，也可以传空字符串，空字符串代表全国在全国范围内进行搜索
        val query = PoiSearch.Query("", "", "")
        query.pageSize = 10// 设置每页最多返回多少条poiitem
        val search = PoiSearch(this, query)
        //设置周边搜索的中心点以及半径
        search.bound = PoiSearch.SearchBound(LatLonPoint(latLng.latitude, latLng.longitude), 10000)
        search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
            override fun onPoiItemSearched(p0: PoiItem?, p1: Int) {}

            override fun onPoiSearched(result: PoiResult?, code: Int) {
                //1000为成功，其他为失败
                if (code == 1000) {
                    val pois = result?.pois
                    //重新构造数据
                    pois?.forEach {
                        mLocationList.add(PositionItem(it.poiId, it.latLonPoint, it.title, it.snippet, it.provinceName, it.cityName, it.adName, it.cityCode))
                    }
                    //如果有当前定位的数据的话，就直接选中，否则用返回的数据的第一条，选中
                    if (mLocationList.isEmpty() || !mLocationList[0].isMyLocation) {
                        mLocationList[0].isSelect = true
                    }
                    adapter.isUseEmpty(mLocationList.isEmpty())
                    //设置数据
                    adapter.setNewData(mLocationList)
                } else {
                    adapter.isUseEmpty(true)
                }
            }
        })
        //发送请求
        search.searchPOIAsyn()
    }

    //初始化地图参数
    fun initMap(aMap: AMap) {
        //设置地图是否显示放大
        aMap.uiSettings.isZoomControlsEnabled = false
        // 设置地图默认的指南针是否显示
        aMap.uiSettings.isCompassEnabled = false
        // 设置默认定位按钮是否显示
        aMap.uiSettings.isMyLocationButtonEnabled = false
        //隐藏logo
        aMap.uiSettings.setLogoBottomMargin(-80)
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.isMyLocationEnabled = false
    }

//    override fun onResume() {
//        super.onResume()
//        map_view.onResume()
//    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

//    override fun onPause() {
//        super.onPause()
//        map_view.onPause()
//    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }
}
