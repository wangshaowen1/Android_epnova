package com.kuaigeglobal.epnova.core.google

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object GoogleServices {

    fun isPlayServicesAvailable(context: Context): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context.applicationContext) == ConnectionResult.SUCCESS
    }
}
