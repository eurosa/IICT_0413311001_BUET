/**
 * Locator is a class which allows getting a location with minimum requirements
 * of age and accuracy. There is also a timer which fires after 20 seconds and
 * uses the best location that was found until now. After a location is sent
 * the locator class auto unregisters.
 *
 */

package com.iictbuet.pgd0413311001;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


/**
 * @author Yuv
 *
 */
/**
 * @author Yuv
 *
 */
public class Locator {
	private static final int SECOND_MS = 1000;
	private static final int MINUTE_MS = 60 * SECOND_MS;


	private static final int MAX_WAITING_TIME_MS = 40 * SECOND_MS;
	private static final float REQUIRED_ACCURACY_METERS = 50;
	private static final int LARGE_LOCATION_AGE_MS = 2 * MINUTE_MS;


	public Location location = null;

	private LocationManager locationManager = null;
	private LocationListener locationListener;
	private BetterLocationListener blListener;
	private Timer waitForGoodLocationTimer;

    public interface BetterLocationListener {
        void onGoodLocation(Location location);
    }

    /**
      * Locator
      *
      * remember to call locator.unregister() when you're done.
    */
    public Locator(final Context context, final BetterLocationListener blListener) {
		if (this.locationManager != null) {
			Log.e("Locator", "registered twice!");
			return;
		}

		this.blListener = blListener;

		// Acquire a reference to the system Location Manager
		this.locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		//Toast.makeText(act.getBaseContext(), "Locator register", Toast.LENGTH_SHORT).show();

		this.location = getBestLastKnownLocation(locationManager);

		// Define a listener that responds to location updates
		this.locationListener = new GoodLocationListener();

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this.locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.locationListener);
		
		this.waitForGoodLocationTimer = new Timer();
		this.waitForGoodLocationTimer.schedule(new GetLastLocation(), MAX_WAITING_TIME_MS);
    }
    
    /**	
	 Gets the best already known location, if none exists, null is returned.
	 
	 @param locationManager
	            The location manager you're using.
	            
	 @return a location or null.
	*/    
    public static Location getBestLastKnownLocation(LocationManager locationManager) {

		Location satelliteLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location networkLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (null == networkLoc) {
			return satelliteLoc;
		} 
		if (null == satelliteLoc) {
			return networkLoc;
		}
		
		// both locations aren't null
		if (Locator.isBetterLocation(networkLoc, satelliteLoc)) {
			return networkLoc;
		} else {
			return satelliteLoc;
		}
    }
    
    private class GoodLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the network location
			// or GPS provider.
			if (Locator.this.location == null) {
				// never use the first location, always compare because
				// that's the only way to find the age of the location.
				Locator.this.location = location;
				return;
			}
			
			if (Locator.isBetterLocation(location, Locator.this.location)) {
				Locator.this.location = location;
				
				if (Locator.isGoodLocation(Locator.this.location)) {
					Locator.this.waitForGoodLocationTimer.cancel();
					Locator.this.emitLocation();
				}
			}

			
			//Toast.makeText(act.getBaseContext(), "New location: " + Locator.location.toString(), Toast.LENGTH_SHORT).show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}
		public void onProviderEnabled(String provider) {}
		public void onProviderDisabled(String provider) {}
	};
    
    protected static boolean isGoodLocation(Location location) {
    	// NOTE: You can't know the exact age of each location because
    	//		the system time is set by the user and the location time is set 
    	//		by the satellites. You can only compare locations by age,
    	//		you can't measure the absolute age of a location.
    	if (! location.hasAccuracy()) {
    		return false;
    	}
    	    	
    	if (location.getAccuracy() > REQUIRED_ACCURACY_METERS ) {
    		return false;
    	}
    	
    	return true;
    }
    
    
    /**	
	 Determines whether one Location reading is better than the current
	 Location fix. DON'T PASS NULL AS A PARAMETER.
	 
	 @param location
	            The new Location that you want to evaluate
	 @param currentBestLocation
	            The current Location fix, to which you want to compare the new
	            one
	            
	 @return true if the first location is better.
	*/
	protected static boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > LARGE_LOCATION_AGE_MS;
		boolean isSignificantlyOlder = timeDelta < -LARGE_LOCATION_AGE_MS;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());
		
		if (location.hasAccuracy() && currentBestLocation.hasAccuracy()) {
			// Check whether the new location fix is more or less accurate
			int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
					.getAccuracy());
			boolean isLessAccurate = accuracyDelta > 0;
			boolean isMoreAccurate = accuracyDelta < 0;
			boolean isSignificantlyLessAccurate = accuracyDelta > 200;
	
	
			// Determine location quality using a combination of timeliness and
			// accuracy
			if (isMoreAccurate) {
				return true;
			} else if (isNewer && !isLessAccurate) {
				return true;
			} else if (isNewer && !isSignificantlyLessAccurate
					&& isFromSameProvider) {
				return true;
			}
			return false;
		}
		
		// one or both locations don't have accuracy information
		
		// prefer GPS
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			return true;
		}
		
		// TODO: maybe prefer locations with hasAccuracy() = true?
		
		if ( ! currentBestLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			// the currentBestLocation isn't GPS, so go with the newer
			if (location.getTime() > currentBestLocation.getTime()) {
				return true;
			}
		}
		
		return false;
	}

	private void emitLocation() {
		Locator.this.unregister();
		Locator.this.blListener.onGoodLocation(Locator.this.location);
	}
	
	// Checks whether two providers are the same
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
	
	class GetLastLocation extends TimerTask {
		@Override
		public void run() {
			// ran out of time to get a good location, go go go
			Locator.this.emitLocation();
		}
	}
    
	public void unregister() {
		if (this.locationManager == null) {
			return;
		}
		
		this.locationManager.removeUpdates(this.locationListener);
		this.locationManager = null;
	}
	
	protected void finalize() throws Throwable
	{
		super.finalize();
		
		unregister(); 
	}

	public static class DgCamActivity extends Activity implements SensorEventListener {
        private Camera mCamera;
        private CameraPreview mPreview;
        private SensorManager sensorManager = null;
        private int orientation;
        private ExifInterface exif;
        private int deviceHeight;
        private Button ibRetake;
        private Button ibUse;
        private Button ibCapture;
        private FrameLayout flBtnContainer;
        private File sdRoot;
        private String dir;
        private String fileName;
        private ImageView rotatingImage;
        private int degrees = -1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main);

            // Setting all the path for the image
            sdRoot = Environment.getExternalStorageDirectory();
            dir = "/DCIM/Camera/";

            // Getting all the needed elements from the layout
    //		rotatingImage = (ImageView) findViewById(R.id.imageView1);
            ibRetake = (Button) findViewById(R.id.ibRetake);
            ibUse = (Button) findViewById(R.id.ibUse);
            ibCapture = (Button) findViewById(R.id.ibCapture);
            flBtnContainer = (FrameLayout) findViewById(R.id.flBtnContainer);

            // Getting the sensor service.
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            // Selecting the resolution of the Android device so we can create a
            // proportional preview
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            deviceHeight = display.getHeight();

            // Add a listener to the Capture pgd0413311001
            ibCapture.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mCamera.takePicture(null, null, mPicture);
                }
            });

            // Add a listener to the Retake pgd0413311001
            ibRetake.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Deleting the image from the SD card/
                    File discardedPhoto = new File(sdRoot, dir + fileName);
                    discardedPhoto.delete();

                    // Restart the camera preview.
                    mCamera.startPreview();

                    // Reorganize the buttons on the screen
                    flBtnContainer.setVisibility(LinearLayout.VISIBLE);
                    ibRetake.setVisibility(LinearLayout.GONE);
                    ibUse.setVisibility(LinearLayout.GONE);
                }
            });

            // Add a listener to the Use pgd0413311001
            ibUse.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Everything is saved so we can quit the app.
                    finish();
                }
            });
        }

        private void createCamera() {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Setting the right parameters in the camera
            Camera.Parameters params = mCamera.getParameters();
            params.setPictureSize(1600, 1200);
            params.setPictureFormat(PixelFormat.JPEG);
            params.setJpegQuality(85);
            mCamera.setParameters(params);

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

            // Calculating the width of the preview so it is proportional.
            float widthFloat = (float) (deviceHeight) * 3 / 3;
            int width = Math.round(widthFloat);

            // Resizing the LinearLayout so we can make a proportional preview. This
            // approach is not 100% perfect because on devices with a really small
            // screen the the image will still be distorted - there is place for
            // improvment.
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, deviceHeight);
            preview.setLayoutParams(layoutParams);

            // Adding the camera preview after the FrameLayout and before the pgd0413311001
            // as a separated element.
            preview.addView(mPreview, 0);
        }

        @Override
        protected void onResume() {
            super.onResume();

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

            // Creating the camera
            createCamera();

            // Register this class as a listener for the accelerometer sensor
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        protected void onPause() {
            super.onPause();
            // release the camera immediately on pause event
            releaseCamera();

            // removing the inserted view - so when we come back to the app we
            // won't have the views on top of each other.
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.removeViewAt(0);
        }

        private void releaseCamera() {
            if (mCamera != null) {
                mCamera.release(); // release the camera for other applications
                mCamera = null;
            }
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
                state = true;
            }

            return state;
        }

        /**
         * A safe way to get an instance of the Camera object.
         */
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

        private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

            public void onPictureTaken(byte[] data, Camera camera) {

                // Replacing the pgd0413311001 after a photho was taken.
                flBtnContainer.setVisibility(View.GONE);
                ibRetake.setVisibility(View.VISIBLE);
                ibUse.setVisibility(View.VISIBLE);

                // File name of the image that we just took.
                fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".jpg";

                // Creating the directory where to save the image. Sadly in older
                // version of Android we can not get the Media catalog name
                File mkDir = new File(sdRoot, dir);
                mkDir.mkdirs();

                // Main file where to save the data that we recive from the camera
                File pictureFile = new File(sdRoot, dir + fileName);

                try {
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

            }
        };

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
                    if (animation != null) {
                        rotatingImage.startAnimation(animation);
                    }
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

        /**
         * STUFF THAT WE DON'T NEED BUT MUST BE HEAR FOR THE COMPILER TO BE HAPPY.
         */
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
