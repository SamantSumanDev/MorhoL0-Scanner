package com.samant.morphoscanner

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.xml.sax.InputSource
import java.io.ByteArrayOutputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_USB_PERMISSION = "com.samant.morphoscanner.USB_PERMISSION" // corrected action string
        private const val AUTHENTICATION_REQUEST = 1
        const val TAG = "MainActivity"
        private const val VENDOR_ID = 123
        private const val PRODUCT_ID = 123
    }

    private lateinit var usbManager: UsbManager
    private lateinit var permissionIntent: PendingIntent
    private var device: UsbDevice? = null
    private lateinit var pbr: ProgressBar
    private lateinit var scanButton: AppCompatButton

    private val usbReceiver = UsbBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById<AppCompatButton>(R.id.btnScan)
        pbr = findViewById(R.id.pbr)
        scanButton.setOnClickListener {
            try {
                usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
                permissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                val filter = IntentFilter(ACTION_USB_PERMISSION)
                registerReceiver(usbReceiver, filter,null,null)
            } catch (e: Exception) {
                Log.e("tag", e.toString())
            }
            checkForConnectedDevices()
        }
    }
    private fun checkForConnectedDevices() {
        val usbManager = this.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceLista = usbManager.deviceList

        if (deviceLista.isEmpty()) {
            // No USB devices connected
          //  Toast.makeText(this, "No USB devices connected", Toast.LENGTH_SHORT).show()
        } else {
            // Loop through connected devices
            for (device in deviceLista.values) {
                val deviceName = device.productName
                // ... Access other device information
            }
        }

     /*   val deviceList = usbManager.deviceList
        val deviceIterator = deviceList.values.iterator()
        while (deviceIterator.hasNext()) {
            val usbDevice = deviceIterator.next()
            // Check if this is the Morpho device
            *//* if (usbDevice.vendorId == VENDOR_ID && usbDevice.productId == PRODUCT_ID) {
              *//*
            device = usbDevice
            usbManager.requestPermission(device, permissionIntent)
            break
            *//* }*//*
        }*/

        if (device == null) {
            Log.d(TAG, "No Morpho device connected.")
            Toast.makeText(this, "No fingerprint device connected.", Toast.LENGTH_SHORT).show()
            pbr.visibility = View.GONE
            scanButton.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "Morpho device connected.")
            startFingerprintCapture()
            pbr.visibility = View.GONE
            scanButton.visibility = View.VISIBLE
        }
    }

    public fun setupDevice(device: UsbDevice) {
        startFingerprintCapture()
    }

    private fun startFingerprintCapture() {
        try {
            val responseXml = """
          <PidOptions 
          ver=\"1.0\">' + '<Opts fCount=\"1\" fType=\"2\" iCount=\"\" iType=\"\" pCount=\"\" pType=\"\" format=\"0\" pidVer=\"2.0\" timeout=\"10000\" otp=\"\" wadh=\"\" posh=\"\"/>' + '
          </PidOptions>
        """.trimIndent()

            val intent = Intent("in.gov.uidai.rdservice.fp.CAPTURE")
            intent.setPackage("com.scl.rdservice")
            intent.putExtra("PID_OPTIONS", responseXml)
            startActivityForResult(intent, AUTHENTICATION_REQUEST)
        }catch (e:Exception){
            Log.e("tag",e.toString())
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    try {

        if (requestCode == AUTHENTICATION_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                val bundle = data.extras
                bundle?.let {
                    val pidData = it.getString("PID_DATA")
                    val dnc = it.getString("DNC", "")
                    val dnr = it.getString("DNR", "")

                    if (pidData != null) {
                        handleFingerprintData(pidData)
                    } else if (dnc.isNotEmpty()) {
                        Log.d(TAG, "Device not connected: $dnc")
                    } else if (dnr.isNotEmpty()) {
                        Log.d(TAG, "Device not registered: $dnr")
                    } else {
                        Log.d(TAG, "Unknown error")
                    }
                }
            } else {
                Toast.makeText(this, "Fingerprint capture failed", Toast.LENGTH_SHORT).show()
            }
        }
    }catch (e:Exception){
        Log.e("tag",e.toString())
    }
    }

    private fun handleFingerprintData(pidData: String) {
        try {
            val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                InputSource(
                    StringReader(pidData)
                )
            )
            xmlDoc.documentElement.normalize()

            val errCode = xmlDoc.getElementsByTagName("Resp")
                .item(0).attributes.getNamedItem("errCode").nodeValue.toInt()
            val errInfo =
                xmlDoc.getElementsByTagName("Resp").item(0).attributes.getNamedItem("errInfo").nodeValue

            if (errCode == 0) {
                // Authentication successful
                executeAuthentication()
                pbr.visibility = View.GONE
                scanButton.visibility = View.VISIBLE
            } else {
                // Authentication failed
                Toast.makeText(this, "Authentication failed: $errInfo", Toast.LENGTH_SHORT).show()

                pbr.visibility = View.GONE
                scanButton.visibility = View.VISIBLE
            }
        }catch (e: Exception){
            Log.e("tag",e.toString())
        }

    }

    private fun executeAuthentication() {
        Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }



}
