package com.iictbuet.pgd0413311001;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

public class EmergencyData {
	
	Context mContext;
	SharedPreferences mSettings;

	public static final String PREFS_NAME = "EmergencyPrefsFile";
	public static final String EMAIL = "emailAddress";
	public static final String PHONE = "phoneNo";
	public static final String MESSAGE = "message";
	public static final String SEND_EMERGENCY = "sendEmergency";

	String nameImage="name";
	String video="video";
	String IMAGE="image";
	Bitmap bitmap;
	ImageView imgPreview;

	String selectedImagePath;
	String getSelectedVideoPath;
	private static final String COUNT_SUFFIX = "Count";
	private String name;

	public EmergencyData(Context context) {
		this.mContext = context;
		this.mSettings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	private int getCount(String id) {
		return mSettings.getInt(id + COUNT_SUFFIX, 1);
	}
	
	private void setCount(SharedPreferences.Editor editor, String id, int count) {
		editor.putInt(id + COUNT_SUFFIX, count);
	}
	
	private List<String> getList(String id) {
		// NOTE: this function should always return a list with at least one
		//		string.
		List<String> stringsList = new ArrayList<String>();
		String lastString;
		lastString = mSettings.getString(id, "");
		stringsList.add(lastString);
		
		int stringsCount = getCount(id);
		for(int i = 1; i < stringsCount; i++) {
			// NOTE: the first item is just "id" and then next is "id1"
			//		this is for backwards compatibility, oh well...
			lastString = mSettings.getString(id + i, "");
			stringsList.add(lastString);
		}
		
		return stringsList;
	}
	
	private void commitList(String id, List<String> stringList) {
		SharedPreferences.Editor editor = mSettings.edit();
		int previousCount = getCount(id);
		int i = 0;
		for (; i < stringList.size(); i++) {
			String idName;
			if (i > 0) {
				idName = id + i;
			} else {
				idName = id;
			}
			editor.putString(idName, stringList.get(i));
		}
		
		for(; i < previousCount; i++) {
			// remove older irrelevant strings
			editor.remove(id + i);
		}
		
		setCount(editor, id, stringList.size());
		editor.commit();
	}
	public String encodeToString(){
		selectedImagePath=mSettings.getString(nameImage, "");

		bitmap = BitmapFactory.decodeFile(selectedImagePath);
		bitmap=Bitmap.createScaledBitmap(bitmap, 100, 100, true);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteFormat = stream.toByteArray();
		String imgString = Base64.encodeToString(byteFormat, Base64.DEFAULT);
		return imgString;
	}
	public List<String> getEmails() {
		return getList(EMAIL);
	}
	public List<String> getPhones() {
		return getList(PHONE);
	}
	public String getMessage() {
		return mSettings.getString(MESSAGE, "");
	}
	public boolean getArmEmergency() {
		return mSettings.getBoolean(SEND_EMERGENCY, false);
	}
	public void getImage(){


		selectedImagePath=mSettings.getString(nameImage, "");
		bitmap = BitmapFactory.decodeFile(selectedImagePath);
		imgPreview.setImageBitmap(bitmap);

	}

    public String getImagePath(){


        selectedImagePath=mSettings.getString(nameImage, "");
//        bitmap = BitmapFactory.decodeFile(selectedImagePath);
//        imgPreview.setImageBitmap(bitmap);
        return selectedImagePath;

    }
	public  String getVideoPath(){
		getSelectedVideoPath=mSettings.getString(video, "");
//        bitmap = BitmapFactory.decodeFile(selectedImagePath);
//        imgPreview.setImageBitmap(bitmap);
		return getSelectedVideoPath;
	}
	
	private void commitString(String id, String value) {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putString(id, value);
		editor.commit();
	}		
	
	public void setEmails(List<String> emails) {
		commitList(EMAIL, emails);
	}
	public void setPhones(List<String> phones) {
		commitList(PHONE, phones);
	}
	public void setMessage(String message) {
		commitString(MESSAGE, message);
	}
	
	public void setArmEmergency(boolean sendMessage) {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putBoolean(SEND_EMERGENCY, sendMessage);
		editor.commit();
	}
public void setImage(Uri imaPath){

	SharedPreferences.Editor editor = mSettings.edit();
 	editor.putString(nameImage, imaPath.getPath());
// 	editor.putString(nameImage, imaPath.toString());
//	editor.putString(IMAGE, encodeTobase64(yourbitmap));
	editor.commit();
}

	public void setVideoPath(Uri videoPath){

		SharedPreferences.Editor editor = mSettings.edit();
		editor.putString(video, videoPath.getPath());

// 	editor.putString(nameImage, imaPath.toString());
//	editor.putString(IMAGE, encodeTobase64(yourbitmap));
		editor.commit();
	}

	public static String encodeTobase64(Bitmap image) {

			String imageEncoded = null;
		if (image != null) {
			System.out.println(image + "Radhason");
			Bitmap immage = image;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			immage.compress(Bitmap.CompressFormat.PNG, 100, baos);
			byte[] b = baos.toByteArray();
			imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

			Log.d("Radhason", imageEncoded);
		}
			return imageEncoded;
		}



	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
