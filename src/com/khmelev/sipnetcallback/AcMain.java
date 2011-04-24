package com.khmelev.sipnetcallback;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class AcMain extends ListActivity {
	private Context mCtx;
	
	private Button btOpenContacts;
	private Button btRequestCall;
	private TextView tvContactNumber;
	private TextView tvContactName;
	
	private DatabaseAdapter mDbHelper;
	
	private String[] NumberList;
	private String SavedName;

	private DefaultHttpClient client;
	private List<Cookie> cookies;

	static final int PICK_CONTACT_REQUEST = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCtx = this;

		mDbHelper = new DatabaseAdapter(this);
		mDbHelper.open();
		mDbHelper.verifyTable();

		tvContactNumber = (TextView) findViewById(R.id.AcMain_ContactNumber);
		tvContactName = (TextView) findViewById(R.id.AcMain_ContactName);
		
		btOpenContacts = (Button) this.findViewById(R.id.AcMain_btOpenContacts);
		btOpenContacts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_PICK,
						ContactsContract.Contacts.CONTENT_URI);
				startActivityForResult(i, PICK_CONTACT_REQUEST);
			}
		});
		btRequestCall = (Button) this.findViewById(R.id.AcMain_btConnect);
		btRequestCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				RequestCall();
			}
		});
		fillLast10Calls();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mDbHelper == null) {
			mDbHelper = new DatabaseAdapter(this);
			mDbHelper.open();
		}
		fillLast10Calls();
	}

	private void RequestCall() {
		String login = AcSettings.getLogin(this);
		String password = AcSettings.getPassword(this);
		if (login == null || password == null || login.length()==0 || password.length()==0) {
			Utils.alertDialog(this, mCtx.getString(R.string.Dialog_MakeCallFail_Title),
					mCtx.getString(R.string.Dialog_LoginOrPassNotSet));
			return;
		}
		String CallbackNumber = AcSettings.getCallbackNumber(this);
		if (!Utils.CheckNumber(CallbackNumber)) {
			Utils.alertDialog(this, mCtx.getString(R.string.Dialog_MakeCallFail_Title),
					mCtx.getString(R.string.Dialog_CallbackNumberNotSet));
			return;
		}
		String ContactNumber = tvContactNumber.getText().toString();
		if(!Utils.CheckNumber(ContactNumber)){
			Utils.alertDialog(mCtx, mCtx.getString(R.string.Dialog_MakeCallFail_Title),
					mCtx.getString(R.string.Dialog_ContactNumberNotSet));
			return;
		}
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("CabinetAction", "login"));
			nameValuePairs.add(new BasicNameValuePair("Name", login));
			nameValuePairs.add(new BasicNameValuePair("Password", password));
			HttpPost post;
			if (client == null || cookies == null || cookies.isEmpty() || cookies.size() < 3) {
				client = new DefaultHttpClient();
				post = new HttpPost("https://customer.sipnet.ru/cabinet/");
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
				new MakeAuthRequest().execute(post);
			} else {
				String getURL = "https://customer.sipnet.ru/cabinet/do_zakaz_service?PHONESRC="	+ CallbackNumber
						+ "&PHONEDST=" + ContactNumber;
				HttpGet get = new HttpGet(getURL);
				new MakeCallRequest().execute(get);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		fillLast10Calls();
	}

	private class MakeAuthRequest extends AsyncTask<HttpPost, Void, HttpResponse> {
		private ProgressDialog dialog;
		@Override
		protected HttpResponse doInBackground(HttpPost... post) {
			try {
				return client.execute(post[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(HttpResponse responsePost) {
			dialog.dismiss();
			if (responsePost!=null && responsePost.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				cookies = client.getCookieStore().getCookies();
				if (cookies.size() != 3) {
					Utils.alertDialog(mCtx, mCtx.getString(R.string.Dialog_AutentificationFailure),
							mCtx.getString(R.string.Dialog_AutentificationFailureMsg));
					return;
				}
			}
			String CallbackNumber = AcSettings.getCallbackNumber(mCtx);
			String ContactNumber = tvContactNumber.getText().toString();
			String getURL = "https://customer.sipnet.ru/cabinet/do_zakaz_service?PHONESRC="	+ CallbackNumber
					+ "&PHONEDST=" + ContactNumber;
			HttpGet get = new HttpGet(getURL);
			new MakeCallRequest().execute(get);
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(mCtx, "", mCtx.getString(R.string.Dialog_Autentificating),
					true);
		}
	}

	private class MakeCallRequest extends
			AsyncTask<HttpGet, Void, HttpResponse> {
		private ProgressDialog dialog;

		@Override
		protected HttpResponse doInBackground(HttpGet... get) {
			HttpResponse responseGet = null;
			try {
				if (!Constants.DEBUG) {
					responseGet = client.execute(get[0]);
				} else {
					Utils.debug(get[0].getURI().toString());
				}
				return responseGet;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(HttpResponse responseGet) {
			dialog.dismiss();
			if (Constants.DEBUG
					|| responseGet.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				Toast.makeText(mCtx, "«вонок заказан. ∆дите вызова.", Toast.LENGTH_LONG)
						.show();
				mDbHelper.addRec(tvContactNumber.getText().toString(),
						tvContactName.getText().toString());
				fillLast10Calls();
			}
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(mCtx, "", mCtx.getString(R.string.Dialog_Calling),
					true);
		}
	}

	private void fillLast10Calls() {
		Cursor callsCursor = mDbHelper.fetchLast10Calls();
		startManagingCursor(callsCursor);
		String[] from = new String[] { "number", "name" };
		int[] to = new int[] { R.id.text1, R.id.text2 };
		SimpleCursorAdapter last10calls = new SimpleCursorAdapter(this,
				R.layout.lastcall_raw, callsCursor, from, to);
		setListAdapter(last10calls);
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		Context mCtx = this;
		if (requestCode == PICK_CONTACT_REQUEST) {
			if (resultCode == RESULT_OK) {
				Cursor cursor = managedQuery(intent.getData(), null, null,
						null, null);
				cursor.moveToFirst();
				String contactId = cursor.getString(cursor
						.getColumnIndex(ContactsContract.Contacts._ID));
				SavedName = cursor
						.getString(cursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				Cursor phones = managedQuery(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID
								+ "=" + contactId, null, null);
				if (phones.getCount() > 0) {
					if (phones.getCount() > 1) {
						NumberList = new String[phones.getCount()];
						int i = 0;
						while (phones.moveToNext()) {
							String phoneNumber = phones
									.getString(phones
											.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							NumberList[i] = phoneNumber;
							i++;
						}
						AlertDialog.Builder builder = new AlertDialog.Builder(
								mCtx);
						builder.setTitle(R.string.AcMain_ChooseNumber);
						builder.setItems(NumberList,
								new DialogInterface.OnClickListener() {
									public void onClick(
											DialogInterface dialogInterface,
											int item) {
										String phoneNumber = NumberList[item]
												.replaceAll("[-+ ]", "");
										tvContactNumber.setText(phoneNumber);
										tvContactName.setText(SavedName);
										return;
									}
								});
						AlertDialog alertDialog = builder.create();
						alertDialog.show();
					} else {
						phones.moveToFirst();
						String phoneNumber = phones
								.getString(phones
										.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						phoneNumber = phoneNumber.replaceAll("[-+ ]", "");
						tvContactNumber.setText(phoneNumber);
						tvContactName.setText(SavedName);
					}
				} else {
					Toast.makeText(this, R.string.AcMain_NumberNotFound,
							Toast.LENGTH_LONG).show();
				}
				cursor.close();
				phones.close();

			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuItem_settings:
			startActivity(new Intent(this, AcSettings.class));
			return true;
		case R.id.menuItem_about:
			startActivity(new Intent(this, AcAbout.class));
			return true;
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String[] NameAndNumber = mDbHelper.getNameAndNumber(String.valueOf(id));
		if (NameAndNumber != null) {
			tvContactNumber.setText(NameAndNumber[0]);
			tvContactName.setText(NameAndNumber[1]);
		}
	}
}