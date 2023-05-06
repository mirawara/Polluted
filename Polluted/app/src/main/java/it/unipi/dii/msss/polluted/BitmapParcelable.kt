package it.unipi.dii.msss.polluted

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable

class BitmapParcelable(val bitmap: Bitmap?) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.createByteArray()?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bitmap, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BitmapParcelable> {
        override fun createFromParcel(parcel: Parcel): BitmapParcelable {
            return BitmapParcelable(parcel)
        }

        override fun newArray(size: Int): Array<BitmapParcelable?> {
            return arrayOfNulls(size)
        }
    }
}