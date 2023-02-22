package com.example.limitr.ui.blocker

import android.os.Parcel
import android.os.Parcelable
import android.widget.TextView

data class TextViewWrapper(val textView: TextView, val textViewId: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        TODO("textView"),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(textViewId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TextViewWrapper> {
        override fun createFromParcel(parcel: Parcel): TextViewWrapper {
            return TextViewWrapper(parcel)
        }

        override fun newArray(size: Int): Array<TextViewWrapper?> {
            return arrayOfNulls(size)
        }
    }
}