package com.iictbuet.pgd0413311001;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import static android.content.ContentValues.TAG;
import static com.iictbuet.pgd0413311001.R.id.ibCapture;

public class MyService extends Service implements SensorEventListener {

	public SensorManager mSensorManager;
	public Sensor mAccelerometer;
	public float mAccel; // acceleration apart from gravity
	public float mAccelCurrent; // current acceleration including gravity
	public float mAccelLast; // last acceleration including gravity

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_UI, new Handler());
//		return START_NOT_STICKY;
 	return START_STICKY;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		EmergencyButtonActivity emergencyButtonActivity = new EmergencyButtonActivity();
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		mAccelLast = mAccelCurrent;
		mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
		float delta = mAccelCurrent - mAccelLast;
		mAccel = mAccel * 0.9f + delta; // perform low-cut filter

		if (mAccel > 11) {
//  		final EmergencyButtonActivity emergencyButtonActivity = new EmergencyButtonActivity();
//			if (emergencyButtonActivity != null) {
				EmergencyActivity.armEmergencyActivity(getApplicationContext());
System.out.println("radhasonobject"+getApplicationContext());
//				Intent myIntent = new Intent(getApplicationContext(), EmergencyActivity.class);
 		Intent myIntent = new Intent(getApplicationContext(), EmergencyButtonActivity.class);
				// FLAG_ACTIVITY_NEW_TASK is needed because we're not in an activity
				// already, without it we crash.
			myIntent.putExtra("Fire",2);

//			Intent myIntent_second = new Intent(getApplicationContext(), EmergencyActivity.class);
 				myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 				getApplicationContext().startActivity(myIntent);



//			emergencyButtonActivity.ibCapture.setOnClickListener(new View.OnClickListener() {
//				public void onClick(View v) {
//					try {
//						emergencyButtonActivity.mCamera.takePicture(null, null, emergencyButtonActivity.mPicture);
//					}catch (Exception e){
//						Log.d("ashwary", String.valueOf(e));
//
//					}
//			System.out.println("radhasonobject"+emergencyButtonActivity);
// 			emergencyButtonActivity.redButtonPressed();

//					EmergencyButtonActivity.this.redButtonPressed();
//				}
//			});
//			}
			showNotification();
		}
	}


    @Override
    public void onDestroy() {
//		stopService(this);
//		service.stop();
		super.onDestroy();
		Toast.makeText(getApplicationContext(), "Now the Service has been stopped!",
                Toast.LENGTH_LONG).show();

    }
	/**
	 * show notification when Accel is more then the given int.
	 */
	private void showNotification() {
		final NotificationManager mgr = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder note = new NotificationCompat.Builder(this);
		note.setContentTitle("Device Accelerometer Notification");
		note.setTicker("New Message Alert!");
		note.setAutoCancel(true);
		// to set default sound/light/vibrate or all
		note.setDefaults(Notification.DEFAULT_ALL);
		// Icon to be set on Notification
//		note.setSmallIcon(R.drawable.ic_launcher);
		// This pending intent will open after notification click
		PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this,
				EmergencyButtonActivity.class), 0);
		// set pending intent to notification builder
		note.setContentIntent(pi);
 	mgr.notify(101, note.build());
	}
}
