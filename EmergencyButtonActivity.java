package com.button.emergency;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.nullwire.trace.ExceptionHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.VideoView;

import static com.android.mms.LogTag.TAG;

@SuppressWarnings("ALL")
public class  EmergencyButtonActivity extends Activity implements SensorEventListener {
    public static Camera mCamera;
	public CameraPreview mPreview;
	public SensorManager sensorManager = null;
	public int orientation;
	public ExifInterface exif;
	public int deviceHeight;
	public Button ibRetake;
	public Button ibUse;
	public Button ibCapture;
	public FrameLayout flBtnContainer;
	public static File sdRoot;
	public String dir;
	public String fileName;
	public ImageView rotatingImage;
	public int degrees = -1;
	ImageView iv;
	FrameLayout linearLayout;
	public static MediaRecorder mediaRecorder;
	static Button myButton;
	static boolean recording;
	static public MoreEditText mPhonesMoreEditText = null;
	static public MoreEditText mEmailsMoreEditText = null;

	//Radhason
	// Activity request codes
	public static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
	public static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	Button btnCapturePicture;
	// directory name to store captured images and videos
	public static final String IMAGE_DIRECTORY_NAME = "POWER";
	public static MyCameraSurfaceView myCameraSurfaceView;
	public Uri fileUri; // file url to store image/video
	public  Uri imageUri;
	public static Uri videoUri;

	public ImageView imgPreview;
	public VideoView videoPreview;
	public Bitmap bitmap;

	//Radhason
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ExceptionHandler.register(this, new StackMailer());

		String[] perms = {"android.permission.FINE_LOCATION", "android.permission.CAMERA",
				"android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_EXTERNAL_STORAGE",
				"android.permission.READ_INTERNAL_STORAGE","android.hardware.camera","android.permission.ACCESS_FINE_LOCATION",
				"android.permission.INTERNET","android.permission.WRITE_SETTINGS","android.permission.WRITE_SECURE_SETTINGS"
				,"android.permission.CHANGE_NETWORK_STATE"
		};

		int permsRequestCode = 200;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			requestPermissions(perms, permsRequestCode);
		}
        // Setting all the path for the image
        sdRoot = Environment.getExternalStorageDirectory();
        dir = "/DCIM/Camera/";

		//Intent intent_gps=new Intent("android.location.GPS_ENABLED_CHANGE");
		//intent_gps.putExtra("enabled", true);
		//sendBroadcast(intent_gps);
		//turnGpsOn(this);
		//Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
		//intent.putExtra("enabled", true);
		//this.sendBroadcast(intent);

		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if(!provider.contains("gps")){ //if gps is disabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			this.sendBroadcast(poke);


		}
        // Getting all the needed elements from the layout
//        rotatingImage = (ImageView) findViewById(R.id.imageView1);


        // Getting the sensor service.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Selecting the resolution of the Android device so we can create a
        // proportional preview
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        deviceHeight = display.getHeight();

        // Add a listener to the Capture pgd0413311001






		/*
//		 * Record video pgd0413311001 click event
//		 */
//		btnRecordVideo.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// record video
//				recordVideo();
//			}
//		});

		// Checking camera availability
		if (!isDeviceSupportCamera()) {
			Toast.makeText(getApplicationContext(),
					"Sorry! Your device doesn't support camera",
					Toast.LENGTH_LONG).show();
			// will close the app if the device does't have cameras
			finish();
		}
		Intent intent1 = new Intent(this, MyService.class);
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		//	if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				Log.d(TAG,"Permission is granted");
				releaseCamera();
				createCamera();

		//}
		//}


		//Start Service
		startService(intent1);
	}

	private void turnGpsOn (Context context) {
		/*String beforeEnable = Settings.Secure.getString(context.getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		String newSet = String.format ("%s,%s",
				beforeEnable,
				LocationManager.GPS_PROVIDER);
		try {
			Settings.Secure.putString (context.getContentResolver(),
					Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
					newSet);
		} catch(Exception e) {}*/
		try
		{

			String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);


			if(!provider.contains("gps")){ //if gps is disabled
				final Intent poke = new Intent();
				poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
				poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
				poke.setData(Uri.parse("3"));
				sendBroadcast(poke);
			}
		}
		catch (Exception e) {

		}
	}


	@Override
	public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

		switch(permsRequestCode){

			case 200:

				boolean locationAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
				boolean cameraAccepted = grantResults[1]== PackageManager.PERMISSION_GRANTED;

				break;

		}

	}

	public static Camera getCameraInstance() {
        Camera c = null;
        try {
            // attempt to get a Camera instance
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }

        // returns null if camera is unavailable
        return c;
    }

	public void createCamera() {
        // Create an instance of Camera
		try {
			releaseCamera();
			mCamera = getCameraInstance();

			// Setting the right parameters in the camera
//			Camera.Parameters params = mCamera.getParameters();
//			params.setPictureSize(1600, 1200);
//			params.setPictureFormat(PixelFormat.JPEG);
//			params.setJpegQuality(100);
//			mCamera.setDisplayOrientation(90);
//			params.setRotation(90);
//
//			mCamera.setParameters(params);
			if(mCamera == null){
				Toast.makeText(this,
						"Fail to get Camera",
						Toast.LENGTH_LONG).show();
			}
			mCamera.setDisplayOrientation(90);
			myCameraSurfaceView = new MyCameraSurfaceView(this, mCamera);
			// Create our Preview view and set it as the content of our activity.
//			mPreview = new CameraPreview(this, mCamera);
// 			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//
//			// Calculating the width of the preview so it is proportional.
//			float widthFloat = (float) (deviceHeight) * 3 / 3;
//			int width = Math.round(widthFloat);

			// Resizing the LinearLayout so we can make a proportional preview. This
			// approach is not 100% perfect because on devices with a really small
			// screen the the image will still be distorted - there is place for
			// improvment.
//			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, deviceHeight);
//
//			preview.setLayoutParams(layoutParams);

			// Adding the camera preview after the FrameLayout and before the pgd0413311001
			// as a separated element.
//			preview.addView(mPreview, 0);

			FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.camera_preview);
            myCameraPreview.addView(myCameraSurfaceView);
			myButton = (Button)findViewById(R.id.mybutton);
			Button.OnClickListener myButtonOnClickListener
					= new Button.OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub

					try{
						if(recording){
							// stop recording and release camera
							mediaRecorder.stop();  // stop the recording
							releaseMediaRecorder(); // release the MediaRecorder object

							//Exit after saved
							//finish();
							myButton.setText("Record");
							recording = false;
						}else{

							//Release Camera before MediaRecorder start
							releaseCamera();
//createCamera();
							if(!prepareMediaRecorder()){
								Toast.makeText(EmergencyButtonActivity.this,
										"Fail in prepareMediaRecorder()!\n - Ended -",
										Toast.LENGTH_LONG).show();
								finish();
							}

							mediaRecorder.start();
							recording = true;
							myButton.setText("STOP");
						}
					}catch (Exception ex){
						ex.printStackTrace();
					}
				}};
			myButton.setOnClickListener(myButtonOnClickListener);


		}catch (Exception e){
			Log.d("Crash", String.valueOf(e));

		}
    }

	private static boolean prepareMediaRecorder(){
		mCamera = getCameraInstance();
		mCamera.setDisplayOrientation(90);
		mediaRecorder = new MediaRecorder();

		mCamera.unlock();

		mediaRecorder.setCamera(mCamera);

		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));



		mediaRecorder.setOutputFile("/sdcard/" + getFileName_CustomFormat() + ".mp4");
		//mediaRecorder.setOutputFile("/sdcard/myvideo1.mp4");

		File videoFile = new File(sdRoot, getFileName_CustomFormat() + ".mp4");
 		videoUri= Uri.fromFile(videoFile.getAbsoluteFile());

		mediaRecorder.setMaxDuration(60000); // Set max duration 60 sec.
		mediaRecorder.setMaxFileSize(50000000); // Set max file size 50M

 		mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	private void popup(String title, String text) {
		AlertDialog.Builder builder = new AlertDialog.Builder(EmergencyButtonActivity.this);
		builder.setMessage(text)
				.setTitle(title)
				.setCancelable(true)
				.setPositiveButton("ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}




	private static void releaseMediaRecorder(){
		if (mediaRecorder != null) {
			mediaRecorder.reset();   // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = new MediaRecorder();
			mCamera.lock();           // lock camera for later use
		}
	}

	public static void releaseCamera(){
		if (mCamera != null){
			mCamera.release();        // release the camera for other applications
			mCamera = null;
		}
	}

	private static String getFileName_CustomFormat() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	private void initUI() {
		setContentView(R.layout.main);
		recording = false;
		this.restoreTextEdits();
	/*
		 * Capture image pgd0413311001 click event
		 */
		Intent myIntent = getIntent();


        ibRetake = (Button) findViewById(R.id.ibRetake);
        ibUse = (Button) findViewById(R.id.ibUse);
        ibCapture = (Button) findViewById(R.id.ibCapture);
        flBtnContainer = (FrameLayout) findViewById(R.id.flBtnContainer);
		//-------------------------------------------------------------27/10/2016-------------
//		imgPreview = (ImageView) findViewById(R.id.imgPreview);
//		videoPreview = (VideoView) findViewById(R.id.videoPreview);
//		btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
//		btnRecordVideo = (Button) findViewById(R.id.btnRecordVideo);




//		if(myIntent != null && myIntent.getExtras() != null) {
//			System.out.println(myIntent.getIntExtra("Fire", -1));
////			xyz(myIntent.getIntExtra("Fire", -1)); // Run the method with the ID Value
//			// passed through the Intent Extra
//		}

		// Add a listener to the Retake pgd0413311001
		ibRetake.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Deleting the image from the SD card/
				File discardedPhoto = new File(sdRoot, dir + fileName);
				discardedPhoto.delete();
				mCamera.setDisplayOrientation(90);
				// Restart the camera preview.
				mCamera.startPreview();

				// Reorganize the buttons on the screen
				flBtnContainer.setVisibility(LinearLayout.VISIBLE);
				ibRetake.setVisibility(LinearLayout.GONE);
				ibUse.setVisibility(LinearLayout.GONE);
			}
		});


		ibCapture.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				new LongOperation().execute("");
//				try {
//					mCamera.takePicture(null, null, mPicture);
////					EmergencyButtonActivity.this.redButtonPressed();
//
//				} catch (Exception e) {
//					Log.d("ash", String.valueOf(mCamera+"--"+e+"--"+mPicture));
//
//				}

// 				EmergencyButtonActivity.this.redButtonPressed();
			}
		});

//		stop.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				stopService(new Intent(getApplicationContext(), MyService.class));
//			}
//		});
		// Add a listener to the Use pgd0413311001
		ibUse.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				try {
//					File discardedPhoto = new File(sdRoot, dir + fileName);
//					discardedPhoto.delete();
//				}catch (Exception e){
//					Log.d("Photocancel","Hello cancel"+e);
//				}

				bitmap = BitmapFactory.decodeFile(imageUri.getPath());
				bitmap = Bitmap.createScaledBitmap(bitmap, 300, 200, true);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
				linearLayout = (FrameLayout) findViewById(R.id.linearl);
				Log.d("Earth", "Love" + iv);
				if (iv != null) {
					linearLayout.removeView(iv);
				}
//		final Button btn = (Button) findViewById(R.id.btn);
				iv = new ImageView(getApplicationContext());
				iv.setImageBitmap(bitmap);
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				iv.setLayoutParams(lp);
				linearLayout.addView(iv);


				// Everything is saved so we can quit the app.
//				previewCapturedImage();
//                finish();
//				startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);

			}

		});

		createCamera();
//        btnCapturePicture.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// capture picture
//                // Creating the camera
//
////				setResult(RESULT_OK);
////				finish();
////				captureImage();
//
//
//			}
//		});


		final ImageButton btnEmergency = (ImageButton) findViewById(R.id.btnEmergency);
		btnEmergency.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// try sending the message:
				EmergencyButtonActivity.this.redButtonPressed();
			}
		});

		ImageButton btnHelp = (ImageButton) findViewById(R.id.btnHelp);
		btnHelp.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				popupHelp();
			}
		});


		/*Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intent, CONTACT_PICKER_RESULT);*/
		if(myIntent != null && myIntent.getExtras() != null) {
			System.out.println("Sabuj"+myIntent.getIntExtra("Fire", -1));
//			xyz(myIntent.getIntExtra("Fire", -1)); // Run the method with the ID Value
			// passed through the Intent Extra
//   		    ibCapture.performClick();
//			System.out.println("Sabuj3"+ibCapture.performClick());



			Thread timer = new Thread(){
				public void run(){
					try{

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try {
									sleep(5000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								ibCapture.performClick();
if(ibCapture.performClick()==true){
	Toast.makeText(getApplicationContext(), "Now the ibCapture button has been clicked!"+ibCapture.performClick(),
			Toast.LENGTH_LONG).show();
//	EmergencyButtonActivity.this.redButtonPressed();
}



//								dbloadingInfo.setVisibility(View.VISIBLE);
//								bar.setVisibility(View.INVISIBLE);
//								loadingText.setVisibility(View.INVISIBLE);
							}

						});

//						sleep(5000);
//						ibCapture.performClick();
					} catch (Exception e){

						System.out.println("Sabuj3"+e);
						e.printStackTrace();
					}

				}
			};
			timer.start();
			Toast.makeText(getApplicationContext(), "First time without clicking!"+ibCapture.performClick(),
					Toast.LENGTH_LONG).show();

			Thread timer2 = new Thread(){
				public void run(){
					try{

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try {
									sleep(5000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
//								btnEmergency.performClick();


//								dbloadingInfo.setVisibility(View.VISIBLE);
//								bar.setVisibility(View.INVISIBLE);
//								loadingText.setVisibility(View.INVISIBLE);
							}

						});

//						sleep(5000);
//						ibCapture.performClick();
					} catch (Exception e){

						System.out.println("Sabuj3"+e);
						e.printStackTrace();
					}

				}
			};
			timer2.start();

		}

	}




	private class LongOperation extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				mCamera.takePicture(null, null, mPicture);

			} catch (Exception e) {
				Log.d("ash", String.valueOf(mCamera+"--"+e+"--"+mPicture));

			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
//			EmergencyButtonActivity.this.redButtonPressed();
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}



	public void popupHelp() {
		// An activity may have been overkill AND for some reason
		// it appears in the task switcher and doesn't allow returning to the
		// iictbuet configuration mode. So a dialog is better for this.
		//IntroActivity.open(EmergencyButtonActivity.this);

		final String messages [] = {
				"Welcome to Emergency Button, enter a phone number, email and message. They'll be saved for an iictbuet.",
				"When you press the iictbuet pgd0413311001, or both widget buttons within 5 seconds, the distress signal is sent.",
				"Add the widget at your home screen using:\nMenu->Add->Widgets->Emergency Button"
		};

		// inverted order - They all popup and you hit "ok" to see the next one.
		popup("3/3", messages[2]);
		popup("2/3", messages[1]);
		popup("1/3", messages[0]);
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		initUI();
	}

    @Override
    /**
     * Putting in place a listener so we can get the sensor data only when
     * something changes.
     */
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                RotateAnimation animation = null;
                if (event.values[0] < 4 && event.values[0] > -4) {
                    if (event.values[1] > 0 && orientation != ExifInterface.ORIENTATION_ROTATE_90) {
                        // UP
                        orientation = ExifInterface.ORIENTATION_ROTATE_90;
                        animation = getRotateAnimation(270);
                        degrees = 270;
                    } else if (event.values[1] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_270) {
                        // UP SIDE DOWN
                        orientation = ExifInterface.ORIENTATION_ROTATE_270;
                        animation = getRotateAnimation(90);
                        degrees = 90;
                    }
                } else if (event.values[1] < 4 && event.values[1] > -4) {
                    if (event.values[0] > 0 && orientation != ExifInterface.ORIENTATION_NORMAL) {
                        // LEFT
                        orientation = ExifInterface.ORIENTATION_NORMAL;
                        animation = getRotateAnimation(0);
                        degrees = 0;
                    } else if (event.values[0] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_180) {
                        // RIGHT
                        orientation = ExifInterface.ORIENTATION_ROTATE_180;
                        animation = getRotateAnimation(180);
                        degrees = 180;
                    }
                }
//                if (animation != null) {
//                    rotatingImage.startAnimation(animation);
//                }
            }

        }
    }
    /**
     * Calculating the degrees needed to rotate the image imposed on the pgd0413311001
     * so it is always facing the user in the right direction
     *
     * @param toDegrees
     * @return
     */
    private RotateAnimation getRotateAnimation(float toDegrees) {
        float compensation = 0;

        if (Math.abs(degrees - toDegrees) > 180) {
            compensation = 360;
        }

        // When the device is being held on the left side (default position for
        // a camera) we need to add, not subtract from the toDegrees.
        if (toDegrees == 0) {
            compensation = -compensation;
        }

        // Creating the animation and the RELATIVE_TO_SELF means that he image
        // will rotate on it center instead of a corner.
        RotateAnimation animation = new RotateAnimation(degrees, toDegrees - compensation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        // Adding the time needed to rotate the image
        animation.setDuration(250);

        // Set the animation to stop after reaching the desired position. With
        // out this it would return to the original state.
        animation.setFillAfter(true);

        return animation;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private class StackMailer implements ExceptionHandler.StackTraceHandler {
		public void onStackTrace(String stackTrace) {
			EmailSender.send("admin@andluck.com", "EmergencyButtonError", "EmergencyButtonError\n" + stackTrace);
		}
	}

	@Override
	protected void onStart()
	{


		super.onStart();
	}


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private boolean checkSDCard() {
        boolean state = false;

        String sd = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(sd)) {
			Log.d("sdfcard","jkjlkjs: "+sd);

			state = true;
        }

        return state;
    }

//private silo ami public e leksi public Camera.PictureCallback
    public Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {

            // Replacing the pgd0413311001 after a photho was taken.
            flBtnContainer.setVisibility(View.GONE);
            ibRetake.setVisibility(View.VISIBLE);
            ibUse.setVisibility(View.VISIBLE);

            // File name of the image that we just took.
            fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".png";

            // Creating the directory where to save the image. Sadly in older
            // version of Android we can not get the Media catalog name
            File mkDir = new File(sdRoot, dir);
            mkDir.mkdirs();

            // Main file where to save the data that we recive from the camera
            File pictureFile = new File(sdRoot, dir + fileName);
			imageUri= Uri.fromFile(pictureFile.getAbsoluteFile());

            try {
			//Toast.makeText(getApplicationContext(),
			//		"Radhason to send image"+imageUri+"Path"+imageUri.getPath(), Toast.LENGTH_LONG)
			//			.show();
                FileOutputStream purge = new FileOutputStream(pictureFile);
                purge.write(data);
                purge.close();
            } catch (FileNotFoundException e) {
                Log.d("DG_DEBUG", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("DG_DEBUG", "Error accessing file: " + e.getMessage());
            }

            // Adding Exif data for the orientation. For some strange reason the
            // ExifInterface class takes a string instead of a file.
            try {
                exif = new ExifInterface("/sdcard/" + dir + fileName);
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation);
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }


			bitmap = BitmapFactory.decodeFile(imageUri.getPath());
			bitmap = Bitmap.createScaledBitmap(bitmap, 300, 200, true);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			linearLayout = (FrameLayout) findViewById(R.id.linearl);
			Log.d("Earth","Love"+iv);
			if(iv!=null) {
				linearLayout.removeView(iv);
			}
//		final Button btn = (Button) findViewById(R.id.btn);
			iv = new ImageView(getApplicationContext());
			iv.setImageBitmap(bitmap);
			LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			iv.setLayoutParams(lp);
			linearLayout.addView(iv);
if(imageUri.getPath()!=null) {
	EmergencyButtonActivity.this.redButtonPressed();
}
		}

    };
	@Override
	protected void onResume()
	{
		super.onResume();

		initUI();
		//IntroActivity.openOnceAfterInstallation(this);
		helpOnceAfterInstallation();

        // Test if there is a camera on the device and if the SD card is
        // mounted.
        if (!checkCameraHardware(this)) {
            Intent i = new Intent(this, NoCamera.class);
            startActivity(i);
            finish();
        } else if (!checkSDCard()) {
            Intent i = new Intent(this, NoSDCard.class);
            startActivity(i);
            finish();
        }



        // Register this class as a listener for the accelerometer sensor
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}


	@Override
	protected void onPause() {
		super.onPause();

		this.saveTextEdits();
		try {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mPreview.getHolder().removeCallback(mPreview);
			mCamera.release();
			mCamera = null;
		}catch (Exception e){

		}
	}

	@Override
	protected void onStop() {
		super.onStop();
try {
	mCamera.stopPreview();
	mCamera.release();
	mCamera = null;
}catch (Exception e){

}
	}

	/*
	public void setPhoneNum(String phoneNumber) {
		// gui
		EditText txtPhoneNo = (EditText) findViewById(R.id.txtPhoneNo);
		txtPhoneNo.setText(phoneNumber);

		// save
		EmergencyData iictbuet = new EmergencyData(this);
		iictbuet.setPhone(txtPhoneNo.getText().toString());
	}*/

	public void helpOnceAfterInstallation() {
		// runs only on the first time opening
		final String wasOpenedName = "wasOpened";
		final String introDbName = "introActivityState";
		SharedPreferences settings = this.getSharedPreferences(introDbName, Context.MODE_PRIVATE);
		boolean wasOpened = settings.getBoolean(wasOpenedName, false);

		if (wasOpened) {
			return;
		}

		// mark that it was opened once
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(wasOpenedName, true);
		editor.commit();

		//IntroActivity.open(context);

		popupHelp();
	}

	private class EditTextRow {
		LinearLayout mLinlay;
		EditText mEditText;
		ImageButton mRemoveBtn;

		public EditTextRow(String text, EditText example) {
			mEditText = new EditText(EmergencyButtonActivity.this);
			// set weight so the pgd0413311001 is only as big as it needs to contain the image.
			//mEditText.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
			mEditText.setLayoutParams(example.getLayoutParams());
			mEditText.setText(text);
			//mEditText.setInputType(InputType.TYPE_CLASS_PHONE);
			mEditText.setInputType(example.getInputType());

			mRemoveBtn = new ImageButton(EmergencyButtonActivity.this);
			mRemoveBtn.setBackgroundResource(R.drawable.grey_x);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.CENTER;
			mRemoveBtn.setLayoutParams(params);

			mLinlay = new LinearLayout(EmergencyButtonActivity.this);
			mLinlay.setOrientation(LinearLayout.HORIZONTAL);
			mLinlay.addView(mEditText);
			mLinlay.addView(mRemoveBtn);
		}
	}

	private class MoreEditText {
		private LinearLayout mContainer;
		private ArrayList<EditText> mEditTextList = null;

		public MoreEditText(LinearLayout container, EditText textWidget, List<String> stringsList) {
			// Create the rows from scratch, this should only happen onCreate

			mContainer = container;
			mEditTextList = new ArrayList<EditText>();
			//txtPhoneNo = (EditText) findViewById(R.id.txtPhoneNo);
			EditText edit;
			//edit = getDefaultTextEdit(container);
			edit = textWidget;
			if(! stringsList.isEmpty()) {
				edit.setText(stringsList.get(0));
			}
			mEditTextList.add(edit);
			for (int i = 1; i < stringsList.size(); i++) {
				addRow(stringsList.get(i));
			}
		}

		public void restore(LinearLayout container, EditText textWidget, List<String> stringsList) {
			// Create the rows from older existing rows, this can happen on
			// changes of orientation, onResume, etc
			mContainer = container;

			for(int i = 0; i < mEditTextList.size(); i++) {
				EditText edit;
				if (i == 0) {
					// the first row is the default one (with the "+")
					//edit = getDefaultTextEdit(container);
					edit = textWidget;
					mEditTextList.set(0, edit);
					if (stringsList.size() > 0) {
						edit.setText(stringsList.get(0));
					}
				} else {
					edit = mEditTextList.get(i);
					View viewRow = (LinearLayout) edit.getParent();
					((LinearLayout)viewRow.getParent()).removeView(viewRow);
					mContainer.addView(viewRow);
				}

			}
		}

		public EditText getDefaultTextEdit(LinearLayout container) {
			// TODO: turn this into something like "getEditTextChild" rather than counting on the index "0"
			return (EditText) ((LinearLayout)container.getChildAt(0)).getChildAt(0);

		}

		public void removeRow(EditText editText) {
			mContainer.removeView((View) editText.getParent());
			mEditTextList.remove(editText);
		}

		public void addRow(String text) {
			final EditTextRow editRow = new EditTextRow(text, mEditTextList.get(0));
			editRow.mRemoveBtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					MoreEditText.this.removeRow(editRow.mEditText);
				}
			});

			mContainer.addView(editRow.mLinlay);
			mEditTextList.add(editRow.mEditText);
		}

		public List<String> GetTexts() {
			ArrayList<String> texts = new ArrayList<String>();
			for (int i = 0; i < mEditTextList.size(); i ++) {
				texts.add(mEditTextList.get(i).getText().toString());
			}

			return texts;
		}


	}

	/**
	 * Checking device has camera hardware or not
	 * */
	private boolean isDeviceSupportCamera() {
		if (getApplicationContext().getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/*
	 * Capturing Camera Image will lauch camera app requrest image capture
	 */
	private void captureImage() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

		// start the image capture Intent
		startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);

	}

	/*
	 * Here we store the file url as it will be null after returning from camera
	 * app
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// save file url in bundle as it will be null on scren orientation
		// changes
//		outState.putParcelable("file_uri", fileUri);
		outState.putParcelable("image_uri", imageUri);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// get the file url
//		fileUri = savedInstanceState.getParcelable("file_uri");
		imageUri = savedInstanceState.getParcelable("image_uri");
	}





	/*
	 * Display image from a path to ImageView
	 */
	private void previewCapturedImage() {
//		imgPreview = (ImageView) findViewById(R.id.imgPreview);
		try {

			// bimatp factory
			BitmapFactory.Options options = new BitmapFactory.Options();

			// downsizing image as it throws OutOfMemory Exception for larger
			// images
			options.inSampleSize = 4;

			final Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(),
					options);

			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
					matrix, true);

			System.out.println("Sabuj"+imageUri);
			System.out.println("Sabuj p"+imageUri.getPath());
//			Toast.makeText(getApplicationContext(),
//					"ban"+bitmap+"Path"+imageUri.getPath(), Toast.LENGTH_LONG)
//					.show();
			if(rotated!=null){
//				Toast.makeText(getApplicationContext(),
//						"ash"+imgPreview+"Path"+imageUri.getPath(), Toast.LENGTH_LONG)
//						.show();
			}
			System.out.println(" boy"+rotated+"Path"+imageUri.getPath());
//			imgPreview.setImageBitmap(rotated);
//			imgPreview.invalidate();

		} catch (NullPointerException e) {
			System.out.println("F error"+e+"Path"+imageUri.getPath());
			e.printStackTrace();
		}
	}

	/*
	 * Previewing recorded video
	 */
	private void previewVideo() {
		try {
			// hide image preview
			imgPreview.setVisibility(View.GONE);

			videoPreview.setVisibility(View.VISIBLE);
			videoPreview.setVideoPath(imageUri.getPath());
			// start playing
			videoPreview.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	/*
	 * Creating file uri to store image/video
	 */
	public Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}
	private static File getOutputMediaFile(int type) {

		// External sdcard location
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				IMAGE_DIRECTORY_NAME);

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
						+ IMAGE_DIRECTORY_NAME + " directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".png");
		}
		else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	private void addPhonesEmailsUI(List<String> phones, List<String> emails) {
		LinearLayout phoneNoLin = (LinearLayout)findViewById(R.id.linPhoneNo);
		LinearLayout emailLin = (LinearLayout)findViewById(R.id.linEmail);
		EditText txtPhoneNo = (EditText)findViewById(R.id.txtPhoneNo);
		EditText txtEmail = (EditText)findViewById(R.id.txtEmail);
		// NOTE: we don't always create from scratch so that empty textboxes
		//		aren't erased on changes of orientation.
		if (mPhonesMoreEditText == null) {
			mPhonesMoreEditText = new MoreEditText(phoneNoLin, txtPhoneNo, phones);
			mEmailsMoreEditText = new MoreEditText(emailLin, txtEmail, emails);
		} else {
			mPhonesMoreEditText.restore(phoneNoLin, txtPhoneNo, phones);
			mEmailsMoreEditText.restore(emailLin, txtEmail, emails);
		}

		// register the Plus buttons
//		if (Bitmap.Config.manyContacts) {

			ImageButton btnPhoneNoPlus = (ImageButton) findViewById(R.id.btnPhoneNoPlus);
			btnPhoneNoPlus.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mPhonesMoreEditText.addRow("");
				}
			});

			ImageButton btnEmailPlus = (ImageButton) findViewById(R.id.btnEmailPlus);
			btnEmailPlus.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mEmailsMoreEditText.addRow("");
				}
			});
//		}
	}



	public void restoreTextEdits() {

		EmergencyData emergencyData = new EmergencyData(this);

		addPhonesEmailsUI(emergencyData.getPhones(), emergencyData.getEmails());
		EditText txtMessage = (EditText) findViewById(R.id.txtMessage);
 		txtMessage.setText(emergencyData.getMessage());
// 		txtMessage.setText(emergencyData.getImagePath()+" Video Path:"+emergencyData.getVideoPath());
//		imgPreview = (ImageView) findViewById(R.id.imgPreview);
		if(emergencyData.getImagePath()!=null){
			try {
				// hide video preview
//			videoPreview.setVisibility(View.GONE);
				bitmap = BitmapFactory.decodeFile(emergencyData.getImagePath());
				if(bitmap!=null) {
					bitmap = Bitmap.createScaledBitmap(bitmap, 300, 200, true);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
					linearLayout = (FrameLayout) findViewById(R.id.linearl);
//		final Button btn = (Button) findViewById(R.id.btn);
					iv = new ImageView(getApplicationContext());
					iv.setImageBitmap(bitmap);
					LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					iv.setLayoutParams(lp);
					linearLayout.addView(iv);
				}

// 					Toast.makeText(getApplicationContext(),
// 						"ashWAET"+imgPreview+"Path"+emergencyData.getImagePath(), Toast.LENGTH_LONG)
// 						.show();

			} catch (NullPointerException e) {

				e.printStackTrace();
			}
		}else {

			try {

				bitmap = BitmapFactory.decodeFile(emergencyData.getImagePath());
				bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
				linearLayout = (FrameLayout) findViewById(R.id.linearl);
//		final Button btn = (Button) findViewById(R.id.btn);
				iv = new ImageView(getApplicationContext());
				iv.setImageBitmap(bitmap);
				LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				iv.setLayoutParams(lp);
				linearLayout.addView(iv);

//					Toast.makeText(getApplicationContext(),
//							"king ash" + imgPreview + "Path" + emergencyData.getImagePath(), Toast.LENGTH_LONG)
//							.show();


			} catch (NullPointerException e) {

				e.printStackTrace();
			}
		}
	}

	public void saveTextEdits() {
		EditText txtPhoneNo = (EditText) findViewById(R.id.txtPhoneNo);
		EditText txtMessage = (EditText) findViewById(R.id.txtMessage);
		EditText txtEmail = (EditText) findViewById(R.id.txtEmail);

		EmergencyData emergencyData = new EmergencyData(this);
		List<String> emailsList = mEmailsMoreEditText.GetTexts();

		emergencyData.setPhones(mPhonesMoreEditText.GetTexts());
		emergencyData.setEmails(emailsList);

		emergencyData.setMessage(txtMessage.getText().toString());
		try {
			emergencyData.setImage(imageUri);
//			Toast.makeText(this, "Saved"+imageUri,
//					Toast.LENGTH_LONG).show();
		}catch (Exception e){

		}

		try {
			emergencyData.setVideoPath(videoUri);
//			Toast.makeText(this, "Saved"+videoUri,
//					Toast.LENGTH_LONG).show();
		}catch (Exception e){

		}

		for (int i = 0; i < emailsList.size(); i++) {
			if (!EmailSender.isValidEmail(emailsList.get(i))) {
				Toast.makeText(this, "Invalid email " + emailsList.get(i),
						Toast.LENGTH_SHORT).show();
			}
		}


		Toast.makeText(this, "Saved iictbuet data",
				Toast.LENGTH_SHORT).show();
	}

	public void redButtonPressed() {
		this.saveTextEdits();
		EmergencyData emergency = new EmergencyData(this);

		if ((emergency.getPhones().size() == 0) && (emergency.getEmails().size() == 0)) {
			Toast.makeText(this, "Enter a phone number or email.",
					Toast.LENGTH_SHORT).show();
			return;
		}

		EmergencyActivity.armEmergencyActivity(this);
		Intent myIntent = new Intent(EmergencyButtonActivity.this, EmergencyActivity.class);
		EmergencyButtonActivity.this.startActivity(myIntent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ebutton_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return true;
	}
	public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

		private SurfaceHolder mHolder;
		private Camera mCamera;

		public MyCameraSurfaceView(Context context, Camera camera) {
			super(context);
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int weight,
								   int height) {
			// If your preview can change or rotate, take care of those events here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null){
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e){
				// ignore: tried to stop a non-existent preview
			}

			// make any resize, rotate or reformatting changes here

			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e){
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			// The Surface has been created, now tell the camera where to draw the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}
	}
}
