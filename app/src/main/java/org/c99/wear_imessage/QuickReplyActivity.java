package org.c99.wear_imessage;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class QuickReplyActivity extends ActionBarActivity {
    String handle;
    String service;
    String protocol;
    String name = "";
    String lastMsg;
    Uri attachment;

    private class MessagesAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView sent;
            TextView received;
        }

        private JSONArray msgs;

        public void loadMessages(String service, String handle) {
            JSONObject conversations;

            try {
                conversations = new JSONObject(getSharedPreferences("data", 0).getString("conversations", "{}"));
            } catch (JSONException e) {
                conversations = new JSONObject();
            }

            try {
                JSONObject conversation;
                String key = service + ":" + handle;
                if (conversations.has(key)) {
                    conversation = conversations.getJSONObject(key);

                    msgs = conversation.getJSONArray("msgs");

                    for(int i = 0; i < msgs.length(); i++) {
                        try {
                            JSONObject o = msgs.getJSONObject(i);
                            if(!o.has("type") || o.getString("type").equals("msg"))
                                lastMsg = o.getString("msg");
                        } catch (JSONException e) {
                            lastMsg = msgs.getString(i);
                        }
                    }
                }
            } catch (JSONException e) {
                msgs = new JSONArray();
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return msgs.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return msgs.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

       @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = view;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row_msg, viewGroup, false);

                holder = new ViewHolder();
                holder.received = (TextView)row.findViewById(R.id.recv);
                holder.sent = (TextView)row.findViewById(R.id.sent);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

           try {
               JSONObject msg = msgs.getJSONObject(i);
               if(msg.has("type") && msg.getString("type").equals("sent")) {
                   holder.received.setVisibility(View.GONE);
                   holder.sent.setVisibility(View.VISIBLE);
                   holder.sent.setText(msg.getString("msg"));
               } else {
                   holder.received.setVisibility(View.VISIBLE);
                   holder.sent.setVisibility(View.GONE);
                   holder.received.setText(msg.getString("msg"));
               }
           } catch (JSONException e) {
               try {
                   String msg = msgs.getString(i);
                   holder.received.setVisibility(View.VISIBLE);
                   holder.sent.setVisibility(View.GONE);
                   holder.received.setText(msg);
               } catch (JSONException e1) {
                   e1.printStackTrace();
               }
           }
            return row;
        }
    }

    private static class SyncEntry {
        public String handle;
        public String service;

        @Override
        public String toString() {
            return handle;
        }
    }

    private MessagesAdapter adapter = new MessagesAdapter();

    private SharedPreferences.OnSharedPreferenceChangeListener prefslistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            adapter.loadMessages(service, handle);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Uri contact = null;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_reply);
        getSupportActionBar().hide();

        final EditText message = (EditText)findViewById(R.id.message);
        ImageButton send = (ImageButton)findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                i.setComponent(new ComponentName(getPackageName(), RemoteInputService.class.getName()));
                i.putExtra("handle", handle);
                i.putExtra("service", service);
                i.putExtra("name", name);
                i.putExtra("msg", lastMsg);
                i.putExtra("notification_id", getIntent().getIntExtra("notification_id", 0));
                i.putExtra("reply", message.getText().toString());
                if(attachment != null) {
                    i.putExtra(Intent.EXTRA_STREAM, attachment);
                    finish();
                }
                startService(i);
                message.setText("");
            }
        });

        if(getIntent().hasExtra("handle") && getIntent().hasExtra("service")) {
            handle = getIntent().getStringExtra("handle");
            service = getIntent().getStringExtra("service");
            lastMsg = getIntent().getStringExtra("msg");

            Cursor c = getContentResolver().query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[] { ContactsContract.RawContacts.CONTACT_ID, ContactsSyncAdapterService.ProtocolColumn },
                    ContactsSyncAdapterService.HandleColumn + " = ? AND " + ContactsSyncAdapterService.ServiceColumn + " = ?",
                    new String[] { getIntent().getStringExtra("handle"), getIntent().getStringExtra("service") },
                    null
                    );
            if(c != null && c.moveToFirst()) {
                contact = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.getLong(0));
                protocol = c.getString(1);
                c.close();
            }
        } else if(getIntent() != null && getIntent().getData() != null) {
            Cursor cursor = getContentResolver().query(getIntent().getData(), null, null, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                handle = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));
                service = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA2));
                protocol = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA3));
                contact = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cursor.getLong(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)));
                cursor.close();
            }
        }

        if(contact != null) {
            findViewById(R.id.contact).setVisibility(View.VISIBLE);
            findViewById(R.id.spinner).setVisibility(View.GONE);

            adapter.loadMessages(service, handle);
            ListView listView = (ListView) findViewById(R.id.conversation);
            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(adapter);

            Bitmap b = BitmapFactory.decodeStream(ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contact, true));
            if (b != null) {
                ImageView photo = (ImageView) findViewById(R.id.photo);
                photo.setImageBitmap(b);
            }

            Cursor cursor = getContentResolver().query(contact, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
                ((TextView)findViewById(R.id.name)).setText(name);
                cursor.close();
            }
            ((TextView)findViewById(R.id.protocol)).setText(protocol);
        } else if(getIntent() != null && getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            findViewById(R.id.contact).setVisibility(View.GONE);
            findViewById(R.id.spinner).setVisibility(View.VISIBLE);
            findViewById(R.id.thumbnail).setVisibility(View.VISIBLE);

            Spinner s = (Spinner)findViewById(R.id.spinner);
            ArrayList<SyncEntry> contacts = new ArrayList();
            Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, getResources().getString(R.string.app_name)).appendQueryParameter(
                    ContactsContract.RawContacts.ACCOUNT_TYPE, "org.c99.wear_imessage.account").build();
            Cursor c1 = getContentResolver().query(rawContactUri, new String[]{BaseColumns._ID, ContactsSyncAdapterService.HandleColumn, ContactsSyncAdapterService.ServiceColumn}, null, null, null);
            while (c1 != null && c1.moveToNext()) {
                SyncEntry e = new SyncEntry();
                e.handle = c1.getString(1);
                e.service = c1.getString(2);
                contacts.add(e);
            }
            if(c1 != null)
                c1.close();

            s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, contacts));
            s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    SyncEntry e = (SyncEntry)adapterView.getItemAtPosition(i);
                    handle = e.handle;
                    service = e.service;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            attachment = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            String type = getContentResolver().getType(attachment);
            if(type.startsWith("image/")) {
                try {
                    android.util.Log.e("iMessage", "Image data: " + attachment);

                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(getContentResolver().openInputStream(attachment), null, o);
                    int scale = 1;

                    if (o.outWidth >= 640 || o.outHeight >= 640) {
                        if (o.outWidth > o.outHeight) {
                            if (o.outWidth > 640)
                                scale = o.outWidth / 640;
                        } else {
                            if (o.outHeight > 640)
                                scale = o.outHeight / 640;
                        }
                    }

                    o = new BitmapFactory.Options();
                    o.inSampleSize = scale;
                    Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(attachment), null, o);

                    ((ImageView) findViewById(R.id.thumbnail)).setImageBitmap(bmp);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences("data", 0).registerOnSharedPreferenceChangeListener(prefslistener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSharedPreferences("data", 0).unregisterOnSharedPreferenceChangeListener(prefslistener);
    }
}
