package com.example.limitr.ui.home.model

import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable

data class AppInfoModel(
    val appName: String?,
    val appIcon: Drawable
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        TODO("appIcon")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AppInfoModel> {
        override fun createFromParcel(parcel: Parcel): AppInfoModel {
            return AppInfoModel(parcel)
        }

        override fun newArray(size: Int): Array<AppInfoModel?> {
            return arrayOfNulls(size)
        }
    }

}