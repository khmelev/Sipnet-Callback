package com.khmelev.sipnetcallback;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class AcSettings extends PreferenceActivity implements OnPreferenceClickListener {
	private static final String CLEARCALL= "clearcalls";
	private static final String LOGIN= "login";
	private static final String PASSWORD= "password";
	private static final String CALLBACKNUMBER= "callback_number";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		Preference clearcalls = findPreference(CLEARCALL);
		clearcalls.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference pref) {
		if(pref.compareTo(findPreference(CLEARCALL))==0){
			DatabaseAdapter mDbHelper = new DatabaseAdapter(this);
			mDbHelper.open();
			mDbHelper.truncateCallTable();
			finish();
			return true;
		}
		return false;
	}
	
	public static String getLogin(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(LOGIN, null);
	}
	public static String getPassword(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PASSWORD, null);
	}
	public static String getCallbackNumber(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(CALLBACKNUMBER, null);
	}
	
}
