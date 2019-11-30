package com.button.emergency;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.button.emergency.SMSSender.SMSListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class EmergencyActivity extends Activity {
	
	Location location = null;
	Locator locator = null;

	public static final String KEY_IMAGE = "image";
	public static final String KEY_TEXT = "name";
	private static String LOG_TAG = "EmergencyActivity";
	public static long signalStartedTime = 0;
	
	private final static long COOLDOWN_TIME_MS = 10 * 1000;
	
	private final static int STATE_X_OR_V = 0;
	private final static int STATE_V = 1;
	private final static int STATE_X = 2;
	
	private static AtomicBoolean isSending = new AtomicBoolean(false);
	
	static String locationString = "";
	static String smsString = "";
	static String emailString = "";

	static int locationState = STATE_X_OR_V;
	static int smsState = STATE_X_OR_V;
	static int emailState = STATE_X_OR_V;
	
	static ImageView imgSms;
	static ImageView imgLocation;
	static ImageView imgEmail;

	static TextView txtSms;
	static TextView txtLocation;
	static TextView txtEmail;

	static Activity mLiveThis;

	RadarView	mRadarView;
	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //setContentView(R.layout.progressbar_4);
        
        setContentView(R.layout.emergency_activity_layout);

		mRadarView = findViewById(R.id.radarView);

		mRadarView.setShowCircles(true);

		Button btnOk = findViewById(R.id.btnOkDone);
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				EmergencyActivity.this.finish();
			}
		});

		Button btnConfigure =  findViewById(R.id.btnConfigure);

		btnConfigure.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

                    // TODO Auto-generated method stub
                stopService(new Intent(getApplicationContext(), MyService.class));


				EmergencyButtonActivity.releaseCamera();
//               EmergencyButtonActivity.createCamera();


				Intent myIntent = new Intent(EmergencyActivity.this, EmergencyButtonActivity.class);
				EmergencyActivity.this.startActivity(myIntent);
			}
		});

		//updateGUI();
	}
	
	
	private void updateTextField(TextView txt, String text) {
		txt.setText(text);
	}
	
	private void updateXV(ImageView img, int state) {
		switch (state) {
		case STATE_X:
			img.setImageResource(R.drawable.x);
			break;
		case STATE_V:
			img.setImageResource(R.drawable.v);
			break;
		case STATE_X_OR_V:
			img.setImageResource(R.drawable.x_or_v);
			break;
		}
	}
	
	protected void updateGUI() {
		runOnUiThread(new Runnable() {
		    public void run() {
		    	updateTextField(txtLocation, EmergencyActivity.locationString);
				updateTextField(txtSms, EmergencyActivity.smsString);
				updateTextField(txtEmail, EmergencyActivity.emailString);
				
				updateXV(imgLocation, EmergencyActivity.locationState);
				updateXV(imgSms, EmergencyActivity.smsState);
				updateXV(imgEmail, EmergencyActivity.emailState);
				
				mLiveThis.setProgressBarIndeterminateVisibility(getIsSendingState());
		    }
		});
	}
	
	protected void resetUIState() {
		if (mRadarView != null) mRadarView.startAnimation();
		setLocationState("Waiting For Location", STATE_X_OR_V);
//		setRadaronState(mRadarView);

		setEmailState("...", STATE_X_OR_V);
		setSmsState("...", STATE_X_OR_V);
		setIsSendingState(true);
	}
	
	public static void armEmergencyActivity(Context context) {
		EmergencyData emer = new EmergencyData(context);
		emer.setArmEmergency(true);
	}
	
	private synchronized void resumeEmergency() {
		EmergencyData emer = new EmergencyData(this);
		if(emer.getArmEmergency()) {
			emer.setArmEmergency(false);
			
			this.startDistressSignal();

		} else {
			Log.v("Emergency", "iictbuet resumed but unarmed.");
		}
		this.updateGUI();
		
		requestGPSDialog();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mLiveThis = this;
		
		txtEmail = findViewById(R.id.txtEmail);
		txtLocation = findViewById(R.id.txtLocation);
		txtSms = findViewById(R.id.txtSMS);
		mRadarView =  findViewById(R.id.radarView);

		imgEmail = findViewById(R.id.imgEmail);
		imgLocation = findViewById(R.id.imgLocation);
		imgSms = findViewById(R.id.imgSMS);

		resumeEmergency();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	
	@Override
	protected void onDestroy() {
		Log.v("Emergency", "destroy iictbuet send activity " + isFinishing());
		super.onDestroy();
	}

	
	private void startDistressSignal() {
		// NOTE: this could have been a check for isSending
		//		but I think if the process is alive yet "isSending"
		//		somehow wasn't reset, this is the better option.
		
		long now = SystemClock.elapsedRealtime();
		if (now - signalStartedTime < COOLDOWN_TIME_MS) {
			Toast.makeText(this, "Already sending a message.",
					Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		signalStartedTime = now;
		this.resetUIState();
		startLocator();
	}
	
	private void startLocator() {
		final Context context = this;
		if (this.locator != null) {
			Log.e("EmergencyActivity", "locator exists while lock is open");
		}
		this.locator = new Locator(context,	new EmergencyLocator());
	}

	private void requestGPSDialog() {
		final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps();
		}
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Your GPS seems to be disabled, do you want to enable it?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(
									final DialogInterface dialog,
									final int id) {
								
								showGpsOptions();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	private void showGpsOptions() {
		Intent gpsOptionsIntent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(gpsOptionsIntent);
	}


	private void setLocationState(String locationString, int locationState) {
		EmergencyActivity.locationString = locationString;
		EmergencyActivity.locationState = locationState;
	}
//	private void setRadaronState(RadarView mRadarView){
//		Log.d("RADAR", "got a location"+mRadarView);
// 		mRadarView.setShowCircles(true);
//		if (mRadarView != null) mRadarView.startAnimation();
//	}

	private void setSmsState(String smsString, int smsState) {
		EmergencyActivity.smsString = smsString;
		EmergencyActivity.smsState = smsState;
	}
	
	private void setEmailState(String emailString, int emailState) {
		EmergencyActivity.emailString = emailString;
		EmergencyActivity.emailState = emailState;
		
	}
	
	private void setIsSendingState(boolean isSending) {
		EmergencyActivity.isSending.set(isSending);
		
	}
	
	private boolean getIsSendingState() {
		return EmergencyActivity.isSending.get();
	}
	
	private class EmergencyLocator implements Locator.BetterLocationListener {
		public void onGoodLocation(Location location) {
			Log.v("Emergency", "got a location");
			if (location != null) {
				setLocationState("Location found", STATE_V);
				if (mRadarView != null) mRadarView.stopAnimation();
			} else {
				setLocationState("No location info, sending anyway.", STATE_X);
				if (mRadarView != null) mRadarView.stopAnimation();
			}
			EmergencyActivity.this.updateGUI();
			EmergencyActivity.this.location = location;

			// messageSentTime is either 0 or the last time a
			// message was sent.
			// so if the pgd0413311001 was pressed more recently, fire a
			// message.
			try {
				EmergencyActivity.this.startMessagesThread();
			} finally {
				EmergencyActivity.this.locator = null;
				// let the messages thread do this: EmergencyActivity.this.isSending.set(false);
			}
		}
	}
	
	private void startMessagesThread() {

		// these operations are going to block a bit so run them on another
		// thread that won't interfere with the GUI. That way the user
		// can see the toasts. (this was a really hard bug to find btw)
		Thread t = new EmergencyThread();
		t.start();
	}

	private class EmergencyThread extends Thread {
		
		public void run() {
			try {
				this.sendMessages();
			} finally {
				EmergencyActivity.this.setIsSendingState(false);
				// reset timer so we can immediately send another message if need be.
				signalStartedTime = 0;
				updateGUI();
			}
		}
		
		private void sendSMS(Context context, List<String> phoneNoList, String textMessage,String imagePath) {
			for(String phoneNo: phoneNoList) {
				if (phoneNo.length() > 0) {
					setSmsState("Sending sms", STATE_X_OR_V);
	
					SMSListener smsListener = new SMSListener() {
						public void onStatusUpdate(int resultCode,
								String resultString) {
							
							if (resultCode != Activity.RESULT_OK) {
								setSmsState(resultString, STATE_X);
							}
							if (resultString.equals(SMSSender.FINAL_GOOD_RESULT)) {
								setSmsState(resultString, STATE_V);
							}
							
							updateGUI();
						}
					};
					
					SMSSender.safeSendSMS(context, phoneNo, textMessage,imagePath,
							smsListener);
				} else {
					setSmsState("No phone number configured, not sending SMS.", STATE_X);
				}
				
				updateGUI();
			}
		}
		
		private void sendEmail(Context context,String emailAddress, String emailMessage,String imagePath,String videoPath) {
			if (emailAddress.length() > 0) {
				setEmailState("Sending email", STATE_X_OR_V);
				updateGUI();
				String LOG_MESSAGE="Hello";
				boolean success = EmailSender.send(emailAddress,
						emailMessage,LOG_MESSAGE,imagePath,videoPath,context);
				if (success) {
					setEmailState("Email sent", STATE_V);
				} else {
					setEmailState("Failed sending email", STATE_X);
				}
			} else {
				setEmailState("No email configured, not sending email.", STATE_X);
			}
			
			updateGUI();
			
		}
		
		private String mapsUrl(Location location) {
			String locString = (location.getLatitude())
			+ ","
			+ (location.getLongitude());
	
			String mapsUrl = "http://maps.google.com/maps?q=" + locString;
			return mapsUrl;
		}
		

		private String isotime(long time) {
			Date date = new Date(time);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			return sdf.format(date);
		}
		
		private String locationDescription(Location location) {
			String desc =  mapsUrl(location);
			desc += "\n"; 
			desc += "\n" + "Location provider: " + location.getProvider();
			desc += "\n" + "Location timestamp (UTC): " + isotime(location.getTime());
			if (location.hasAltitude()) {
				desc += "\n" + "Altitude: " + location.getAltitude();
			}
			if (location.hasAccuracy()) {
				desc += "\n" + "Accuracy: " + location.getAccuracy() + " meters";
			}
			
			return desc;
		}
		
		private void sendMessages() {

			final Context context = EmergencyActivity.this;
			// make sure all the fields are fresh and not null
			final EmergencyData emergency = new EmergencyData(context);


			// mResults = doSomethingExpensive();
			// mHandler.post(mUpdateResults);
			// Do it, send all the stuff!
			String message = emergency.getMessage();
			if (message.length() == 0) {
				message = "No iictbuet message specified.";
			}
			String textMessage = message;
			String emailMessage = message;
			if (location != null) {
				textMessage += "\n" + mapsUrl(EmergencyActivity.this.location);
				emailMessage += "\n" + locationDescription(EmergencyActivity.this.location);
			} else {
				textMessage += "\nNo location info.";
				emailMessage += "\nNo location info.";
			}

			final String sms = textMessage;
			final String finalMessage = message;
			Thread smsThread = new Thread() {


				public void run() {
					 String videodata;
				Bitmap bitmap;
//				ServiceHandler sh = new ServiceHandler();
					RequestHandler rh = new RequestHandler();
					HashMap<String, String> param = new HashMap<String, String>();
//				//----------------------------------------------------------------
					if(emergency.getImagePath() != null && !emergency.getImagePath().isEmpty()) {
						bitmap = BitmapFactory.decodeFile(emergency.getImagePath());
						bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
						ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream1);
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
						byte[] byteFormat = stream1.toByteArray();
						final String image = Base64.encodeToString(byteFormat, Base64.DEFAULT);
//		final String text = editText.getText().toString().trim();
//		final String image = getStringImage(bitmap);
						param.put(KEY_IMAGE, image);
					}
					final String text = finalMessage;

				FileInputStream objFileIS = null;
					System.out.println("powercrash1" + emergency.getVideoPath());
					if(emergency.getVideoPath() != null && !emergency.getVideoPath().isEmpty()) {
						try {
							System.out.println("powercrash2" + emergency.getVideoPath());
							objFileIS = new FileInputStream(emergency.getVideoPath());
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
						ByteArrayOutputStream objByteArrayOS = new ByteArrayOutputStream();
						byte[] byteBufferString = new byte[1024000];
						try {
							for (int readNum; (readNum = objFileIS.read(byteBufferString)) != -1; ) {
								objByteArrayOS.write(byteBufferString, 0, readNum);
								System.out.println("read " + readNum + " bytes,");
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

						videodata = Base64.encodeToString(byteBufferString, Base64.DEFAULT);
						param.put("videofile",videodata);
					}


				param.put(KEY_TEXT, text);


//radhason.000webhostapp.com
				String result=rh.sendPostRequest("http://radhason.000webhostapp.com/upload.php", param);


//				try {
					JSONObject myJson = null;
					String name=null;
					String videoLink=null;
					try {
						myJson = new JSONObject(result);
						name = myJson.optString("Data");
						videoLink = myJson.optString("video");
					} catch (JSONException e) {
						e.printStackTrace();
					}

					EmergencyThread.this.sendSMS(context, emergency.getPhones(), sms+" Image Link: "+name+"  Video Link: "+videoLink,emergency.getImagePath());
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
// 				EmergencyThread.this.sendSMS(context, emergency.getPhones(), sms+" Image Link:",emergency.getImagePath());
				}
			};
			smsThread.start();

			for (String email : emergency.getEmails()) {
				this.sendEmail(context,email, emailMessage,emergency.getImagePath(),emergency.getVideoPath());
				//this.sendEmail(context,email, emailMessage,emergency.getImagePath());
			}

			try {
				smsThread.join();
			} catch (InterruptedException e) {
				Log.e(LOG_TAG, e.getMessage(), e);
				e.printStackTrace();
			}

//			UploadImage u = new UploadImage();
//			u.execute();
		}

	}
	
	// Define the Handler that receives messages from the thread and update the progress
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			String state = msg.getData().getString("state");
			int fieldId = msg.getData().getInt("fieldId");
			if (state.equals("") && (fieldId == 0)) {
				EmergencyActivity.this.updateGUI();
				return;
			}
			TextView txt = (TextView) findViewById(fieldId);
			txt.setText(state);
		}
	};
}
