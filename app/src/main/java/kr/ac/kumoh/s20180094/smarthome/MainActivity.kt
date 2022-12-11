package kr.ac.kumoh.s20180094.smarthome

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kr.ac.kumoh.s20180094.smarthome.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var btAdapter: BluetoothAdapter
    lateinit var pairedDevices: Set<BluetoothDevice>
    lateinit var btArrayAdapter: ArrayAdapter<String>
    lateinit var deviceAddressArray: ArrayList<String>

    val BluetoothConnectID : UUID = UUID.randomUUID()


    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(this@MainActivity,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_REQUEST_CODE)
                    }

                }
                val deviceName = device!!.name
                val deviceHardwareAddress = device.address // MAC address
                btArrayAdapter.add(deviceName)
                deviceAddressArray.add(deviceHardwareAddress)
                btArrayAdapter.notifyDataSetChanged()

            }
        }
    }

    companion object{
        const val BLUETOOTH_REQUEST_CODE = 1
    }

    @RequiresApi(Build.VERSION_CODES.S)
    var permission_list = arrayOf<String>(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, permission_list,  BLUETOOTH_REQUEST_CODE);

        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_REQUEST_CODE)
            }
            startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE)
        }

        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceAddressArray = ArrayList()
        binding.listview.adapter = btArrayAdapter

        binding.btnPaired.setOnClickListener {
            btArrayAdapter.clear()
            if (deviceAddressArray.isNotEmpty()) {
                deviceAddressArray.clear()
            }
            pairedDevices = btAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                // There are paired devices. Get the name and address of each paired device.
                for (device in pairedDevices) {
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    btArrayAdapter.add(deviceName)
                    deviceAddressArray.add(deviceHardwareAddress)
                }
            }
        }

        binding.btnSearch.setOnClickListener {
           onClickButtonSearch(it)
        }

        binding.listview.onItemClickListener = myOnItemClickListener()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onClickButtonSearch(view: View?) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN), BLUETOOTH_REQUEST_CODE)
        }
        if (btAdapter.isDiscovering) {
            btAdapter.cancelDiscovery()
        } else {
            if (btAdapter.isEnabled) {
                btAdapter.startDiscovery()
                btArrayAdapter.clear()
                if (deviceAddressArray.isNotEmpty()) {
                    deviceAddressArray.clear()
                }
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, filter)
            } else {
                Toast.makeText(applicationContext, "bluetooth not on", Toast.LENGTH_SHORT).show()
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

    inner class myOnItemClickListener : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            Toast.makeText(
                ApplicationProvider.getApplicationContext(),
                btArrayAdapter.getItem(position),
                Toast.LENGTH_SHORT
            ).show()
            lateinit var btSocket: BluetoothSocket
            binding.textStatus.setText("try...")
            val name: String = btArrayAdapter.getItem(position).toString() // get name
            val address: String = deviceAddressArray[position] // get address
            var flag = true
            val device: BluetoothDevice = btAdapter.getRemoteDevice(address)

            // create & connect socket
            try {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                val btSocket = device.createRfcommSocketToServiceRecord(BluetoothConnectID)
                btSocket.connect()
            } catch (e: IOException) {
                flag = false
                binding.textStatus.setText("connection failed!")
                e.printStackTrace()
            }
            if (flag) {
                binding.textStatus.text = "connected to $name"
                val connectedThread = ConnectedThread(btSocket)
                connectedThread.start()
            }
        }
    }
}

