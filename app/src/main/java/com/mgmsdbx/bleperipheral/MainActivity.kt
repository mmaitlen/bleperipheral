package com.mgmsdbx.bleperipheral
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*

class MainActivity : ComponentActivity() {

    val daUUID: UUID = UUID.fromString("00001805-0000-1000-8333-00805f9b34fb")
    private var bleGattServer: BluetoothGattServer? = null
    private var gattBluetoothGatt: BluetoothGatt? = null

    fun log(msg:String) {
        Log.d("mgmble", msg)
    }

    private var bleMgr: BluetoothManager? = null
    private var bleAdapter: BluetoothAdapter? = null

    private fun createService(): BluetoothGattService {
        val service = BluetoothGattService(daUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        return service
    }

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    startAdvertising()
                    startServer()
                }
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            log("LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            log("LE Advertise Failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private  val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                val name = device?.name
                log("bluetoothdevice CONNECTED: $device $name")


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log("bluetoothdevice DISCONNECTED: $device")

            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                Modifier.padding(all = 8.dp)
            ) {
                Text(text = "Bluetooth LE Peripheral/Server")

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    initialize()
                }) {
                    Text(text = "init")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {

                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    registerReceiver(bleReceiver, filter)
                    if (!bleAdapter!!.isEnabled) {
                        log( "Bluetooth is currently disabled...enabling")
                        bleAdapter!!.enable()
                    } else {
                        log( "Bluetooth enabled...starting services")
                        startAdvertising()
                        startServer()
                    }
                    log("t4")
                }) {
                    Text(text = "reg receiver")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    writeData()
                }) {
                    Text(text = "write data")
                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startServer() {
        bleGattServer = bleMgr?.openGattServer(this, gattServerCallback)

        bleGattServer?.addService(createService()) ?: log("unable to create GATT server")
    }

    private fun initialize() {
        if (bleMgr == null) {
            bleMgr = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            if (bleMgr == null) {
                log("Unable to init BluetoothMgr")
                return
            }
        }
        bleAdapter = bleMgr?.adapter
        if (bleAdapter == null) {
            log("Unable to inti BluetoothAdapter")
            return
        }
        log("BluetoothManager and Adapter initialized ${bleMgr?.adapter?.name}")
    }

    private fun writeData() {

    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val bleAdvertising: BluetoothLeAdvertiser? = bleAdapter?.bluetoothLeAdvertiser
        bleAdvertising?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(daUUID))
                .build()

            log("begin advertistin")
            it.startAdvertising(settings, data, advertiseCallback)
            log("t1 advertistin")
        }
    }
}