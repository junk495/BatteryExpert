package com.batteryexpert

import android.app.Application
import com.batteryexpert.data.ble.Mc5000BleManager

class BatteryExpertApp : Application() {
    val bleManager: Mc5000BleManager by lazy { Mc5000BleManager(applicationContext) }
}
