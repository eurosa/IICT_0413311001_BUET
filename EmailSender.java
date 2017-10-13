package com.iictbuet.pgd0413311001;

import java.io.BufferedReader;

import java.io.ByteArrayOutputStream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Pattern;


import android.app.ProgressDialog;
import android.util.Base64;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONObject;

import static com.iictbuet.pgd0413311001.EmergencyActivity.KEY_IMAGE;
import static com.iictbuet.pgd0413311001.EmergencyActivity.KEY_TEXT;

public class EmailSender {
	private static String LOG_TAG = "EmailSender";
	
	public static boolean send(String to, String message,String log, String imagePath,String videoPath,Context context)  {
//		Log.d("ASHVIDEO",""+videoPath);
		return EmailSender.sendWithEmailbyweb(to, "Emergency", message,log,imagePath,videoPath,context);
	}
	
	public static boolean send(String to, String subject, String message) {
        Context context = null;
		return EmailSender.sendWithEmailbyweb(to, subject, message,"Hello","ImagePath","VideoPath",context);
	}
	
	public static boolean sendWithReplTo(String to, String subject, String message, Context context) {
		Account[] accounts = AccountManager.get(context).getAccounts();
		String possibleEmail = "";
		for (Account account : accounts) {
		  // Check possibleEmail against an email regex or treat
		  // account.name as an email address only for certain account.type values.
		  possibleEmail = account.name;
		  if (isValidEmail(possibleEmail)) {
			  break;
		  }
		}
		
		// TODO: make something happen here.
		return true;
	}
	
	public static boolean sendWithEmailbyweb(String to, String subject, String message, String log, String imagePath, String videoPath, Context context) {
		if (!isValidEmail(to)) {
			return false;
		}

		// NOTE: the "from" is ignored in the php version currently.http://radhason.000webhostapp.com/
        //boolean res = postToUrl("http://radhason.comxa.com/insert.php", "radhasonk.phy@gmail.com", to, subject,message,imagePath,videoPath,context);
		boolean res = postToUrl("http://radhason.000webhostapp.com/insert.php", "radhasonk.phy@gmail.com", to, subject,message,imagePath,videoPath,context);
		if(!res) {
           	res = postToUrl("http://radhason.000webhostapp.com/insert.php", "radhasonk.phy@gmail.com", to, subject,message,imagePath,videoPath,context);
			//res = postToUrl("http://radhason.comxa.com/insert.php", "radhasonk.phy@gmail", to, subject,message,imagePath,videoPath,context);
		}

//		// NOTE: the "from" is ignored in the php version currently.
//		boolean res = postToUrl("http://toplessproductions.com/emailbyweb/", "Emergency Button <EmergencyButtonApp@gmail.com>", to, subject,message);
//		if(!res) {
//			res = postToUrl("https://emailbyweb.appspot.com/email", "Emergency Button <EmergencyButton@emailbyweb.appspotmail.com>", to, subject,message);
//		}
		return res;
	}

//	public static boolean postToUrl(String url, String from, String to, String subject, String message) {
//		String responseBody = "";
//
//		// Create a new HttpClient and Post Header
//		HttpClient httpclient = new DefaultHttpClient();
//		//HttpPost httppost = new HttpPost("https://emailbyweb.appspot.com/email");
//		//HttpPost httppost = new HttpPost("http://toplessproductions.com/emailbyweb/");
//		HttpPost httppost = new HttpPost(url);
//
//		try {
//			// Add your data
//			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
//			nameValuePairs.add(new BasicNameValuePair("to", to));
//			nameValuePairs.add(new BasicNameValuePair("from", from));
//			nameValuePairs.add(new BasicNameValuePair("subject", subject));
//			nameValuePairs.add(new BasicNameValuePair("message", message));
////			nameValuePairs.add(new BasicNameValuePair("secret", Config.secret));
//			// NOTE: UrlEncodedFormEntity has a default encoding of ISO-8859-1
//			//   which is perfect if you want your unicode to silently turn into
//			//   \x1a or empty spaces.
//			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
//
//			// Create a response handler
//	        ResponseHandler<String> responseHandler = new BasicResponseHandler();
//	        responseBody = httpclient.execute(httppost, responseHandler);
//
//		} catch (ClientProtocolException e) {
//			Log.e(LOG_TAG, e.getMessage(), e);
//		} catch (IOException e) {
//			Log.e(LOG_TAG, e.getMessage(), e);
//		}
//
//		if ("success".equals(responseBody)) {
//			Log.v(LOG_TAG, "Email sent.");
//			return true;
//		} else {
//			Log.e(LOG_TAG, "Failed sending email: response \"" + responseBody + "\"");
//			return false;
//		}
//	}
//


    public static boolean postToUrl(final String urlAddress, final String from, final String to, String subject, final String message, String imagePath, String videoPath, final Context context) {
 		String link;
		String data;
		BufferedReader bufferedReader;
		String result;
        Bitmap bitmap;

		final String videodata;

//		try {
//
//			data = "?fullname=" + URLEncoder.encode(message, "UTF-8");
//			data += "&description=" + URLEncoder.encode(from, "UTF-8");
//
////            data += "&imagePath=" + URLEncoder.encode(imagePath, "UTF-8");
//			data += "&emailaddress=" + URLEncoder.encode(to, "UTF-8");
////            data += "&spinner=" + URLEncoder.encode(spinner, "UTF-8");
////            data += "&image=" + URLEncoder.encode(imageUp, "UTF-8");
////            link = "http://radhason.comxa.com/insert.php" + data;
//			link = urlAddress+ data;
// 			Log.d("ash",""+imagePath);
//			URL url = new URL(link);
//			HttpURLConnection con = (HttpURLConnection) url.openConnection();
//
//			bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
//			result = bufferedReader.readLine();
//			Log.d("ash",""+result);
//			return true;
//		} catch (Exception e) {
//			return false;
//		}



        try {


			RequestHandler rh = new RequestHandler();
			HashMap<String, String> param = new HashMap<String, String>();
			FileInputStream objFileIS = null;
			if(videoPath != null && !videoPath.isEmpty()) {

				try {
					System.out.println("filepath" + videoPath);
					objFileIS = new FileInputStream(videoPath);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				ByteArrayOutputStream objByteArrayOS = new ByteArrayOutputStream();
				byte[] byteBufferString = new byte[1024000];
				try {
					for (int readNum; (readNum = objFileIS.read(byteBufferString)) != -1; ) {
						objByteArrayOS.write(byteBufferString, 0, readNum);
						System.out.println("read" + readNum + " bytes,");

					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				videodata = Base64.encodeToString(byteBufferString, Base64.DEFAULT);
				param.put("videofile",videodata);
				System.out.print("videofile"+videodata);
			}
			if(imagePath != null && !imagePath.isEmpty()) {
				bitmap = BitmapFactory.decodeFile(imagePath);
				bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byte[] byteFormat = stream.toByteArray();
				final String image = Base64.encodeToString(byteFormat, Base64.DEFAULT);
//		final String text = editText.getText().toString().trim();
//		final String image = getStringImage(bitmap);
				param.put(KEY_IMAGE, image);
			}
            final String text = message;
//            class UploadImage extends AsyncTask<Void, Void, String> {
                ProgressDialog loading;
                JSONObject hay;

//                @Override
//                protected void onPreExecute() {
//                    super.onPreExecute();
//                    loading = ProgressDialog.show(context, "Please wait...", "uploading", false, false);
//
//                }
//
//                @Override
//                protected void onPostExecute(String s) {
//                    super.onPostExecute(s);
//                    loading.dismiss();
//                    try {
//                        hay = new JSONObject(s);
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    try {
//                        Toast.makeText(context, hay.getString("status"), Toast.LENGTH_LONG).show();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    try {
//                        Toast.makeText(context, hay.getString("Data"), Toast.LENGTH_LONG).show();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                protected String doInBackground(Void... params) {
//			Log.d("VideoData**>  " , videodata);

                    param.put(KEY_TEXT, text);


                    param.put("emailaddress", to);
                    param.put("description", "radhasonk.phy@gmail.com");
                    param.put("fullname", message);
                    System.out.print("radhasonpower"+from);


                     result = rh.sendPostRequest(urlAddress, param);
			System.out.print("return_result"+result);
// 			         result = rh.sendPostRequest("http://radhason.comxa.com/upload.php", param);
//                    return result;
//                }

//            }
//            UploadImage u = new UploadImage();
//            u.execute();
return  true;
        }catch (Exception e) {
			return false;
		}


	}


	public final static Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
	          "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
	          "\\@" +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
	          "(" +
	          "\\." +
	          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
	          ")+"
	      );

	public static boolean isValidEmail(String email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
	}
}
