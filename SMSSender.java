package com.iictbuet.pgd0413311001;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.R.attr.name;
import static com.iictbuet.pgd0413311001.EmergencyActivity.KEY_IMAGE;
import static com.iictbuet.pgd0413311001.EmergencyActivity.KEY_TEXT;

public class SMSSender {

	public static String FINAL_GOOD_RESULT = "SMS delivered";

	public interface SMSListener {
		public void onStatusUpdate(int resultCode, String resultString);
	}

	// ---sends an SMS message to another device---
	public static void sendSMS(Context context, String phoneNumber,
			String message) {
		PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(
				context, EmergencyButtonActivity.class), 0);
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, pi, null);
	}

	// sends an SMS message and reports status with toasts
	public static void safeSendSMS(final Context context, String phoneNumber,
			String message,String imagePath) {
		SMSListener toastListener = new SMSListener() {
			public void onStatusUpdate(int resultCode, String resultString) {
				Toast.makeText(context, resultString, Toast.LENGTH_SHORT)
						.show();
			}
		};

		safeSendSMS(context, phoneNumber, message,imagePath, toastListener);
	}

	private static class SMSData {
		Context context;
		SMSListener listener;
		
		int totalMessages;
		private AtomicInteger sentCount = new AtomicInteger(0);
		private AtomicInteger deliveredCount = new AtomicInteger(0);

		@SuppressWarnings("unused")
		SMSData(final Context context, final SMSListener listener) {
			this(context, listener, 1);
		}

		SMSData(final Context context, final SMSListener listener, int totalMessages) {
			this.context = context;
			this.listener = listener;
			this.totalMessages = totalMessages;
		}

		// TODO: maybe have an unregisterReceiver on a timer as well?
		private void badEvent(int resultCode, String resultString) {
			try {
				context.unregisterReceiver(sentReceiver);
				context.unregisterReceiver(deliveredReceiver);
				listener.onStatusUpdate(resultCode, resultString);
			} catch (IllegalArgumentException e) {
				// This try/catch is because sometimes this happens:
				// java.lang.IllegalArgumentException: Receiver not registered: com.iictbuet.pgd0413311001.SMSSender$SMSData$1@44957138

				// TODO: figure out when/why this happens and fix it.
				
			}
		}

		private void deliveredEvent(int resultCode, String resultString) {
			int delivered = SMSData.this.deliveredCount.incrementAndGet();
			if (delivered == this.totalMessages) {
				// done with everything
				try {
					context.unregisterReceiver(sentReceiver);
					context.unregisterReceiver(deliveredReceiver);
					listener.onStatusUpdate(resultCode, resultString);
				} catch (IllegalArgumentException e) {
					// This fixes this bug:
					// package_name=com.iictbuet.pgd0413311001, package_version=1.4, phone_model=SGH-T959,
					// android_version=2.1-update1, stacktrace=java.lang.RuntimeException: Error
					// receiving broadcast Intent { act=SMS_DELIVERED (has extras) } in
					// com.iictbuet.pgd0413311001.SMSSender$SMSData$2@47ae4d28
					// TODO: figure out why/when this happens
					
				}
			}
		}

		private void sentEvent(int resultCode, String resultString) {
			int sent = SMSData.this.sentCount.incrementAndGet();
			if (sent == this.totalMessages) {
				// done with sending.
				// NOTE: We don't do anything, there is a tradeoff here:
				//	1. wait for delievered to say we succeeded can cause false negatives.
				//	2. wait for sent to say we succeeded can cause false positives.
				//context.unregisterReceiver(sentReceiver);
				//context.unregisterReceiver(deliveredReceiver);
			}
			
			//listener.onStatusUpdate(resultCode, resultString);
			listener.onStatusUpdate(resultCode, "Sent " + sent + "/" + this.totalMessages + " parts");
		}

		public BroadcastReceiver sentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				
				int resCode = getResultCode();
				switch (resCode) {
				case Activity.RESULT_OK:
					SMSData.this.sentEvent(resCode, "SMS sent");
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					SMSData.this.badEvent(resCode, "Generic failure");
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					SMSData.this.badEvent(resCode, "No service");
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					SMSData.this.badEvent(resCode, "Null PDU");
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					SMSData.this.badEvent(resCode, "Radio off");
					break;
				}
			}
		};

		public BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				
				int resCode = getResultCode();
				switch (resCode) {
				case Activity.RESULT_OK:
					// NOTE: this result doesn't appear in the emulator, only
					// on real devices.
					SMSData.this.deliveredEvent(resCode, FINAL_GOOD_RESULT);
					break;
				case Activity.RESULT_CANCELED:
					SMSData.this.badEvent(resCode, "SMS not delivered");
					break;
				}
			}
		};
	}

	public static void safeSendSMS(final Context context, String phoneNumber,
			String message,String imagePath, final SMSListener listener) {
		if(numOfMessages(message) > 1) {
			safeSendLongSMS(context, phoneNumber, message,imagePath, listener);
		} else {
			safeSendLongSMS(context, phoneNumber, message,imagePath, listener);
			//safeSendShortSMS(context, phoneNumber, message, listener);
		}
	}
	


	public static int numOfMessages(String message) {
		SmsManager smsMan = SmsManager.getDefault();
		return smsMan.divideMessage(message).size();
	}

	public static void safeSendLongSMS(final Context context, String phoneNumber,
			String message,String imagePath, final SMSListener listener) {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";
	    System.out.println("radhasonpower"+imagePath);
		Uri imageUri= Uri.parse(imagePath);
		SmsManager smsMan = SmsManager.getDefault();
		ArrayList<String> messagesArray = smsMan.divideMessage(message);
		
		SMSData smsd = new SMSData(context, listener, messagesArray.size());

		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
				new Intent(SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
				new Intent(DELIVERED), 0);

		// ---when the SMS has been sent---
		context.registerReceiver(smsd.sentReceiver, new IntentFilter(SENT));

		// ---when the SMS has been delivered---
		context.registerReceiver(smsd.deliveredReceiver, new IntentFilter(
				DELIVERED));

		ArrayList<PendingIntent> sentPIList = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliveredPIList = new ArrayList<PendingIntent>();
		
		for (int i = 0; i < messagesArray.size() ; i++) {
			// we don't care which failed, one failure is bad enough.
			sentPIList.add(sentPI);
			deliveredPIList.add(deliveredPI);
		}
		
		try {
			ArrayList<EmergencyData> contactList;
			Bitmap mBitmap;


			//-----------------------------------------------------------------
 			smsMan.sendMultipartTextMessage(phoneNumber, null, messagesArray, sentPIList, deliveredPIList);
// 			smsMan.sendMultimediaMessage(context, imageUri, phoneNumber, Bundle configOverrides, PendingIntent sentIntent);
//			Settings sendSettings = new Settings();
//			Transaction sendTransaction = new Transaction(context, sendSettings);
//			Message mMessage = new Message(String.valueOf(messagesArray), phoneNumber);
//			mBitmap = BitmapFactory.decodeFile(imagePath);
//			mBitmap = Bitmap.createScaledBitmap(mBitmap, 200, 200, true);
//			ByteArrayOutputStream stream = new ByteArrayOutputStream();
//			mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//			mMessage.setImage(mBitmap);

//			sendTransaction.sendNewMessage(mMessage, Transaction.NO_THREAD_ID);
//			mMessage.setImage(mBitmap);



		} catch (Exception e) {

            smsd.badEvent(SmsManager.RESULT_ERROR_GENERIC_FAILURE, "SMS Error");
		}
	}
	
}
