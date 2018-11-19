package com.rmkrishna.ibeaconreceiver

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.rmkrishna.ibeacon.IBeaconManager

class MainActivity : AppCompatActivity(), IBeaconManager.IBeaconListener {

    var iBeaconManager: IBeaconManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iBeaconManager = IBeaconManager(this)
    }

    override fun onStart() {
        super.onStart()

        iBeaconManager?.startListening()
        iBeaconManager?.setListener(this)
    }

    override fun onStop() {
        super.onStop()

        iBeaconManager?.stopListening()
    }

    override fun receivedIBeacon(uuid: String?, major: Int, minor: Int) {

    }
}
