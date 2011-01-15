package com.garage48.robo_controller;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
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
  TextView textviewAzimuth, textviewPitch, textviewRoll;
  private static SensorManager mySensorManager;
  private boolean sersorrunning;
  Button connectProxyButton;
  Socket socket;
  DataOutputStream out;
  List<Sensor> mySensors;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    textviewAzimuth = (TextView) findViewById(R.id.textazimuth);
    textviewPitch = (TextView) findViewById(R.id.textpitch);
    textviewRoll = (TextView) findViewById(R.id.textroll);

    mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mySensors = mySensorManager.getSensorList(Sensor.TYPE_ORIENTATION);

    attachEvents();

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
  }

  @Override
  protected void onPause() {
    super.onPause();
    wl.release();

    try {
      out.close();
      socket.close();
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
    }

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
  }

  private void attachEvents() {
    connectProxyButton = (Button) findViewById(R.id.connect_proxy);

    connectProxyButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (!socket.isConnected()) {
          connectToProxy();
        }
      }
    });
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

  };

  private SensorEventListener mySensorEventListener = new SensorEventListener() {

    public void onSensorChanged(SensorEvent event) {

      String azimuth = String.valueOf(event.values[0]);
      String pitch = String.valueOf(event.values[1]);
      String roll = String.valueOf(event.values[2]);

      textviewAzimuth.setText("Azimuth: " + azimuth);
      textviewPitch.setText("Pitch: " + pitch);
      textviewRoll.setText("Roll: " + roll);

      if (socket != null && socket.isConnected()) {
        try {
          out
              .writeBytes("android " + azimuth + " " + pitch + " " + roll
                  + "\n");
        } catch (IOException e) {
          Log.e(TAG, "IOException", e);
        }
      }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
  };

}
