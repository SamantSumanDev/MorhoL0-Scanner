package com.samant.morphoscanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast

class UsbBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_USB_PERMISSION = "android.permission.USB_PERMISSION"
    }


    override fun onReceive(context: Context?, intent: Intent?) {

        if (ACTION_USB_PERMISSION == intent?.action) {
            synchronized(this) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.let {
                        Log.d("USBBROAD", "tag $device")
                        (context as MainActivity).setupDevice(it)
                    }
                } else {
                    Log.d("USBBROAD", "Permission denied for device $device")
                    Toast.makeText(context, "Permission denied for device $device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
