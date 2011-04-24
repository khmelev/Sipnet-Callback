package com.khmelev.sipnetcallback;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Utils {
	public static void alertDialog(Context context, String title, String message) {
		AlertDialog alertDialog;

		alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		alertDialog.show();
	}

	public static void debug(String message) {
		Log.d("SipnetCallback", message);
	}

	public static void error(String message) {
		Log.e("SipnetCallback", message);
	}

	public static void notice(String message) {
		Log.i("SipnetCallback", message);
	}

	public static String getVersion(Context context) throws RuntimeException {
		try {
			return context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException ex) {
			throw new RuntimeException("Failed to get app version");
		}
	}
	public static boolean CheckNumber(String number) {
		if (number == null || number.length()<11){
			return false;
		}
		if(number.matches("^[0-9]++$")){
			return true;
		}
		return false;
	}

}
