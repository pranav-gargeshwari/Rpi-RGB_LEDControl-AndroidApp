package com.example.pranav.BluetoothApp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;
import static android.bluetooth.BluetoothAdapter.STATE_TURNING_ON;
import static android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;

public class MainActivity extends Activity {
    Button BTon, searchDevice, listDevices;
    ImageButton led,red,green,blue;
    TextView stat, txt,receiveData,staticData;
    private BluetoothAdapter BA;
    private BluetoothDevice BD;
    BluetoothSocket BS=null;
    private Set<BluetoothDevice> pairedDevices;
    ListView lv;
    private static final String TAG = "MyActivity";
    int REQUEST_COARSE_LOCATION=1;
    int ledstate=0;
    ArrayList list_name = new ArrayList();
    ArrayList list_address = new ArrayList();
    UUID serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectedThread mConnectedThread;
    Handler bluetoothIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BTon = (Button) findViewById(R.id.button);
        searchDevice = (Button) findViewById(R.id.button2);
        listDevices = (Button) findViewById(R.id.button4);
        stat = (TextView) findViewById(R.id.textView3);
        txt = (TextView) findViewById(R.id.textView2);
        lv = (ListView) findViewById(R.id.listView);
        receiveData= (TextView) findViewById(R.id.textView5);
        staticData= (TextView) findViewById(R.id.textView4);
        led = (ImageButton) findViewById(R.id.button5);
        led.setVisibility(View.INVISIBLE);
        red = (ImageButton) findViewById(R.id.red);
        red.setVisibility(View.INVISIBLE);
        green = (ImageButton) findViewById(R.id.green);
        green.setVisibility(View.INVISIBLE);
        blue = (ImageButton) findViewById(R.id.blue);
        blue.setVisibility(View.INVISIBLE);

        BA = BluetoothAdapter.getDefaultAdapter();
        lv = (ListView) findViewById(R.id.listView);
        if (BluetoothAdapter.STATE_OFF == BA.getState() || BluetoothAdapter.STATE_TURNING_OFF == BA.getState()) {
            BTon.setText("TURN ON");
            BluetoothOff();

        } else if (BluetoothAdapter.STATE_ON == BA.getState() || BluetoothAdapter.STATE_TURNING_ON == BA.getState()) {
            BTon.setText("TURN OFF");
            BluetoothOn();
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //selectedFromList =(String) (lv.getItemAtPosition(myItemInt));
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                String stringText;
                stringText= text1.getText().toString();
                Toast.makeText(getApplicationContext(), "Connecting to " + stringText, Toast.LENGTH_LONG).show();
                BD = BA.getRemoteDevice(text2.getText().toString());
                BD.createBond();
                try {
                    final Method m = BD.getClass().getMethod("createRfcommSocketToServiceRecord", new Class[] { UUID.class });
                    BS=(BluetoothSocket) m.invoke(BD, serviceUUID);
                } catch (Exception e) {
                    Log.e(TAG, "Could not create RFComm Connection Socket",e);
                }
                try {
                    if(BA.isDiscovering())
                    {
                        BA.cancelDiscovery();
                        Log.i(TAG, "Cancel Discovery");
                    }
                    BS.connect();
                    Log.i(TAG, "Connected to Add:" + BD.getAddress() + "Name:" + BD.getName());
                } catch (IOException e) {
                    Log.i(TAG, "Exception while connect");
                }
                if(BS.isConnected())
                {
                    Toast.makeText(getApplicationContext(), "Connected to " + BD.getName(), Toast.LENGTH_LONG).show();
                    mConnectedThread = new ConnectedThread(BS);
                    mConnectedThread.start();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Failed to connect to " + BD.getName(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(mReceiver_, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mReceiver_, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(mReceiver_, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver_);
    }


    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    final String readMessage = new String(buffer, 0, bytes);
                    Log.i(TAG, "Data received:" + readMessage.toString());
                    stat.post(new Runnable() {
                        public void run() {
                            receiveData.setText((CharSequence) readMessage.toString());
                        }
                    });
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

    protected void BluetoothOn(){
        searchDevice.setVisibility(View.VISIBLE);
        listDevices.setVisibility(View.VISIBLE);
        txt.setVisibility(View.VISIBLE);
        receiveData.setVisibility(View.VISIBLE);
        staticData.setVisibility(View.VISIBLE);
    }

    protected void BluetoothOff(){
        searchDevice.setVisibility(View.INVISIBLE);
        listDevices.setVisibility(View.INVISIBLE);
        txt.setVisibility(View.INVISIBLE);
        receiveData.setVisibility(View.INVISIBLE);
        staticData.setVisibility(View.INVISIBLE);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Log.i(TAG, "ACTION_DISCOVERY_STARTED");
                stat.setText("DISCOVERY");
            }

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.i(TAG, "ACTION_DISCOVERY_FINISHED");
                stat.setText("");
            }

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        BluetoothOff();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        BluetoothOn();
                        break;
                }
            }
        }
    };



    private final BroadcastReceiver mReceiver_ = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();


            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i(TAG, "New device BT found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "New device: " + device.getName());
                if (device.getName() != null && !device.getName().isEmpty() && !device.getName().equals("null"))
                {
                    list_name.add(device.getName());
                    list_address.add(device.getAddress());

                    final ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_2,android.R.id.text1, list_name)
                    {
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                            text1.setText((CharSequence) list_name.get(position));
                            text2.setText((CharSequence) list_address.get(position));
                            return view;
                        }
                    };

                    lv.setAdapter(adapter);
                }
            }

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.i(TAG, "Device Connected");
                led.setVisibility(View.VISIBLE);
                red.setVisibility(View.VISIBLE);
                green.setVisibility(View.VISIBLE);
                blue.setVisibility(View.VISIBLE);
                stat.setText(BD.getName());
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.i(TAG, "Device Disconnected");
                led.setVisibility(View.INVISIBLE);
                red.setVisibility(View.INVISIBLE);
                green.setVisibility(View.INVISIBLE);
                blue.setVisibility(View.INVISIBLE);
                stat.setText("");
            }

        }
    };

    public void red(View v){
        mConnectedThread.write("R");
    }

    public void green(View v){
        mConnectedThread.write("G");
    }

    public void blue(View v){
        mConnectedThread.write("B");
    }

    public void led(View v){
        if(ledstate ==0) {
            led.setImageResource(R.drawable.led_on);
            ledstate = 1;
            mConnectedThread.write("X");
        }
        else if(ledstate ==1)
        {
            led.setImageResource(R.drawable.led_off);
            ledstate = 0;
            mConnectedThread.write("O");
        }
    }

    public void on(View v) {
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
            BTon.setText("TURN OFF");
            BluetoothOn();
        } else {
            BA.disable();
            Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
            BTon.setText("TURN ON");
            BluetoothOff();
        }
    }

    public void SearchDevice(View v) {
        checkLocationPermission();
        BA.startDiscovery();
        txt.setText("AVAILABLE DEVICES");
        list_name.clear();
        list_address.clear();
        Toast.makeText(getApplicationContext(), "Searching devices", Toast.LENGTH_SHORT).show();
    }


    public void list(View v) {
        pairedDevices = BA.getBondedDevices();
        txt.setText("PAIRED DEVICES");
        list_name.clear();
        list_address.clear();

        for (BluetoothDevice bt : pairedDevices) {
            list_name.add(bt.getName());
            list_address.add(bt.getAddress());
        }
        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();

        //final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list_name);
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2,android.R.id.text1, list_name)
        {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText((CharSequence) list_name.get(position));
                text2.setText((CharSequence) list_address.get(position));
                return view;
            }
        };

        lv.setAdapter(adapter);
    }

    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_COARSE_LOCATION);
            BA.startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BA.startDiscovery();
                } else {
                    //TODO re-request
                }
                break;
            }
        }

    }
}