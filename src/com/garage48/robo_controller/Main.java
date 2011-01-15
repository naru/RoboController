package com.garage48.robo_controller;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {

  private PowerManager.WakeLock wl;
  private static final String TAG = "RoboController!!!";
  private static final int REQUEST_ENABLE_BT = 1;
  
  TextView textviewAzimuth, textviewPitch, textviewRoll, textviewSpeed,
      textviewTilt;
  private static SensorManager mySensorManager;
  private boolean sersorrunning;
  Button connectProxyButton, disconnectProxyButton, connectNxtButton;
  Socket socket;
  DataOutputStream out;
  List<Sensor> mySensors;
  boolean send = false;
  BluetoothAdapter mBluetoothAdapter;
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    textviewAzimuth = (TextView) findViewById(R.id.textazimuth);
    textviewPitch = (TextView) findViewById(R.id.textpitch);
    textviewRoll = (TextView) findViewById(R.id.textroll);

    textviewSpeed = (TextView) findViewById(R.id.textspeed);
    textviewTilt = (TextView) findViewById(R.id.texttilt);

    mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mySensors = mySensorManager.getSensorList(Sensor.TYPE_ORIENTATION);

    attachEvents();

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
    
    ensureBluetooth();
    
    // Register the BroadcastReceiver
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    registerReceiver(mReceiver, filter); // Don't forget to unregister during
                                         // onDestroy    
  }

  @Override
  protected void onPause() {
    super.onPause();
    wl.release();

    disconnectProxy();

    if (sersorrunning) {
      mySensorManager.unregisterListener(mySensorEventListener);
      Toast.makeText(Main.this, "unregisterListener", Toast.LENGTH_SHORT)
          .show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    wl.acquire();

    if (mySensors.size() > 0) {
      mySensorManager.registerListener(mySensorEventListener, mySensors.get(0),
          SensorManager.SENSOR_DELAY_NORMAL);
      sersorrunning = true;
      Toast.makeText(this, "Start ORIENTATION Sensor", Toast.LENGTH_LONG)
          .show();
    } else {
      Toast.makeText(this, "No ORIENTATION Sensor", Toast.LENGTH_LONG).show();
      sersorrunning = false;
      finish();
    }
  };
  
  @Override
  protected void onDestroy() {
    unregisterReceiver(mReceiver);
    super.onDestroy();
  }

  private void attachEvents() {
    connectProxyButton = (Button) findViewById(R.id.connect_proxy);

    connectProxyButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        connectToProxy();
      }
    });

    disconnectProxyButton = (Button) findViewById(R.id.disconnect_proxy);

    disconnectProxyButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (socket != null && socket.isConnected()) {
          disconnectProxy();
        }
      }
    });
    
    connectNxtButton = (Button) findViewById(R.id.connect_nxt);

    connectNxtButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        mBluetoothAdapter.startDiscovery();
      }
    }); 
  };
  
  private void ensureBluetooth() {
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    if (!mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  };

  private void connectToProxy() {
    try {
      socket = new Socket("62.237.153.254", 9000);
      out = new DataOutputStream(socket.getOutputStream());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    send = true;
  };

  private void disconnectProxy() {
    try {
      if (send) {
        out.close();
        socket.close();
      }
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
    } catch (Exception e) {
      Log.e(TAG, "Exception", e);
    }
    send = false;
  };

  private float getSpeed(float roll) {
    float speed = 0;

    speed = ((roll / 45) - 1) * -1;

    if (speed > 1) {
      speed = 1;
    } else if (speed < -1) {
      speed = -1;
    }

    return speed;
  };

  private float getTilt(float pitch) {
    float tilt;

    tilt = pitch / 90 * -1;

    if (tilt > 1) {
      tilt = 1;
    } else if (tilt < -1) {
      tilt = -1;
    }

    return tilt;
  };

  private SensorEventListener mySensorEventListener = new SensorEventListener() {

    public void onSensorChanged(SensorEvent event) {

      float azimuth = event.values[0];
      float pitch = event.values[1];
      float roll = event.values[2];

      textviewAzimuth.setText("Azimuth: " + azimuth);
      textviewPitch.setText("Pitch: " + pitch);
      textviewRoll.setText("Roll: " + roll);

      float speed = getSpeed(roll);
      float tilt = getTilt(pitch);

      textviewSpeed.setText("Speed: " + speed);
      textviewTilt.setText("Tilt: " + tilt);

      if (send) {
        try {
          Log.d(TAG, "speed " + speed + " " + tilt + "\n");
          out.writeBytes("speed " + speed + " " + tilt + "\n");
        } catch (IOException e) {
          Log.e(TAG, "IOException", e);
        }
      }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
  };

  // Create a BroadcastReceiver for ACTION_FOUND
  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      // When discovery finds a device
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        // Get the BluetoothDevice object from the Intent
        BluetoothDevice device = intent
            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        // Add the name and address to an array adapter to show in a ListView
        if (device.getAddress().equals("00:16:53:06:45:EB")) {
          Log.d(TAG, "*********** !!!!!! NXT FOUND !!!!!! **********");
          ConnectThread connectThread = new ConnectThread(device);
          connectThread.start();
        }

      }
    }
  };

  private class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
      // Use a temporary object that is later assigned to mmSocket,
      // because mmSocket is final
      BluetoothSocket tmp = null;
      mmDevice = device;

      // Get a BluetoothSocket to connect with the given BluetoothDevice
      try {
        // MY_UUID is the app's UUID string, also used by the server code
        tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
      } catch (IOException e) {
      }
      mmSocket = tmp;
    }

    public void run() {
      // Cancel discovery because it will slow down the connection
      mBluetoothAdapter.cancelDiscovery();
      
      Log.d(TAG, "******** CONNECTING TO NXT!!");

      try {
        // Connect the device through the socket. This will block
        // until it succeeds or throws an exception
        mmSocket.connect();
      } catch (IOException connectException) {
        Log.e(TAG, "Failed To Connect *****", connectException);
        // Unable to connect; close the socket and get out
        try {
          mmSocket.close();
        } catch (IOException closeException) {
        }
        return;
      }

      Log.d(TAG, "******** CONNECTED TO NXT!!");      
      
      // Do work to manage the connection (in a separate thread)
      ConnectedThread btConnThread = new ConnectedThread(mmSocket);
      btConnThread.start();
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {
      }
    }
  }
  
  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmOutStream = tmpOut;
    }

    /* Call this from the main Activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main Activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
}
