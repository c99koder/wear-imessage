/**
 * 
 */
package org.c99.wear_imessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;

import org.json.JSONArray;
import org.json.JSONObject;

public class ContactsSyncAdapterService extends Service {
	private static SyncAdapterImpl sSyncAdapter = null;
	private static ContentResolver mContentResolver = null;
	public static String HandleColumn = RawContacts.SYNC1;
	public static String ServiceColumn = RawContacts.SYNC2;
	public static String ProtocolColumn = RawContacts.SYNC3;
	private static Integer syncSchema = 1;

	public ContactsSyncAdapterService() {
		super();
	}

	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context mContext;

		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
			try {
				ContactsSyncAdapterService.performSync(mContext, account, extras, authority, provider, syncResult);
			} catch (OperationCanceledException e) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	private SyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null)
			sSyncAdapter = new SyncAdapterImpl(this);
		return sSyncAdapter;
	}

	private static long addContact(Account account, String name, String handle, String service, String protocol) {
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
		builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
		builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
		builder.withValue(HandleColumn, handle);
		builder.withValue(ServiceColumn, service);
		builder.withValue(ProtocolColumn, protocol);
		operationList.add(builder.build());

		builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
		operationList.add(builder.build());

		builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.org.c99.wear_imessage.profile");
		builder.withValue(ContactsContract.Data.DATA1, handle);
		builder.withValue(ContactsContract.Data.DATA2, service);
		builder.withValue(ContactsContract.Data.DATA3, protocol);
		builder.withValue(ContactsContract.Data.DATA4, "iMessage");
		builder.withValue(ContactsContract.Data.DATA5, "Send message");
		operationList.add(builder.build());

		builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
		if(protocol.equals("AIM")) {
			builder.withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM);
		} else if(protocol.equals("Jabber")) {
			builder.withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER);
		} else {
			builder.withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
			builder.withValue(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, protocol);
		}
		builder.withValue(ContactsContract.CommonDataKinds.Im.DATA, handle);
		operationList.add(builder.build());

		if(protocol.equals("iMessage") && handle.contains("@")) {
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, handle);
			builder.withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE);
			operationList.add(builder.build());
		} else {
			builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
			builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, handle);
			builder.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
			operationList.add(builder.build());
		}

		try {
			mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
			Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(
					RawContacts.ACCOUNT_TYPE, account.type).build();
			Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID, HandleColumn }, HandleColumn + " = '" + handle + "'", null, null);
			if (c1.moveToNext()) {
				return c1.getLong(0);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	private static void deleteContact(Context context, long rawContactId) {
		Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId).buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
		ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
		try {
			client.delete(uri, null, null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client.release();
	}

	private static class SyncEntry {
		public Long raw_id = 0L;
		public String handle = null;
		public String service = null;
	}
	
	private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
			throws OperationCanceledException {
		HashMap<String, SyncEntry> localContacts = new HashMap<String, SyncEntry>();
		mContentResolver = context.getContentResolver();

		//If our app has requested a full sync, we're going to delete all our local contacts and start over
		boolean is_full_sync = context.getSharedPreferences("data", 0).getBoolean("do_full_sync", false);
		
		//If our schema is out-of-date, do a fresh sync
		if(context.getSharedPreferences("data", 0).getInt("sync_schema", 0) < syncSchema)
			is_full_sync = true;
		
		// Load the local contacts
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(
				RawContacts.ACCOUNT_TYPE, account.type).build();
		Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID, HandleColumn, ServiceColumn }, null, null, null);
		while (c1 != null && c1.moveToNext()) {
			if(is_full_sync) {
				deleteContact(context, c1.getLong(0));
			} else {
				SyncEntry entry = new SyncEntry();
				entry.raw_id = c1.getLong(0);
				entry.handle = c1.getString(1);
				entry.service = c1.getString(2);
				localContacts.put(c1.getString(1), entry);
			}
		}

		Editor editor = context.getSharedPreferences("data", 0).edit();
		editor.remove("do_full_sync");
		editor.putInt("sync_schema", syncSchema);
		editor.commit();

		URL url = null;
		try {
			url = new URL("http://" + context.getSharedPreferences("prefs", 0).getString("host", "") + "/buddies");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		HttpURLConnection conn;

		try {
			conn = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		conn.setUseCaches(false);

		BufferedReader reader = null;

		try {
			if (conn.getInputStream() != null) {
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
			}
		} catch (IOException e) {
			if (conn.getErrorStream() != null) {
				reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()), 512);
			}
		}

		if (reader != null) {
			try {
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append('\n');
				}
				reader.close();

				JSONArray buddies = new JSONArray(sb.toString());

				for(int i = 0; i < buddies.length(); i++) {
					JSONObject buddy = buddies.getJSONObject(i);
					if (!localContacts.containsKey(buddy.getString("handle"))) {
						long id = addContact(account, buddy.getString("name"), buddy.getString("handle"), buddy.getString("service"), buddy.getString("service_type"));
						if(id != -1) {
							SyncEntry entry = new SyncEntry();
							entry.raw_id = id;
							localContacts.put(buddy.getString("handle"), entry);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		conn.disconnect();

		mContentResolver.addPeriodicSync(account, authority, null, 60L * 60L);
	}
}