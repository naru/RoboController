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
  TextView textviewAzimuth, textviewPitch, textviewRoll, textviewSpeed,
      textviewTilt;
  private static SensorManager mySensorManager;
  private boolean sersorrunning;
  Button connectProxyButton;
  Button disconnectProxyButton;
  Socket socket;
  DataOutputStream out;
  List<Sensor> mySensors;
  boolean send = false;

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
      out.close();
      socket.close();
    } catch (IOException e) {
      Log.e(TAG, "IOException", e);
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

}
