package com.xbrc.myapplication.bean

import android.os.Parcel
import android.os.Parcelable
import com.amap.api.services.core.LatLonPoint

//构造定位数据结构
data class PositionItem(
        var id: String = "",
        var latLonPoint: LatLonPoint,
        var poiName: String = "",
        var address: String = "",
        var provinceName: String = "",
        var cityName: String = "",
        var adName: String = "",
        var cityCode: String = "",
        var isMyLocation:Boolean=false,
        var isSelect: Boolean = false) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readParcelable(LatLonPoint::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeParcelable(latLonPoint, flags)
        parcel.writeString(poiName)
        parcel.writeString(address)
        parcel.writeString(provinceName)
        parcel.writeString(cityName)
        parcel.writeString(adName)
        parcel.writeString(cityCode)
        parcel.writeByte(if (isMyLocation) 1 else 0)
        parcel.writeByte(if (isSelect) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PositionItem> {
        override fun createFromParcel(parcel: Parcel): PositionItem {
            return PositionItem(parcel)
        }

        override fun newArray(size: Int): Array<PositionItem?> {
            return arrayOfNulls(size)
        }
    }
}