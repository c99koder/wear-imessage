package org.c99.wear_imessage;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;


public class QuickReplyActivity extends ActionBarActivity {
    String handle;
    String service;
    String protocol;
    Uri attachment;

    private static class SyncEntry {
        public String handle;
        public String service;

        @Override
        public String toString() {
            return handle;
        }
    }

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
                i.putExtra("handle", handle);
                i.putExtra("service", service);
                i.putExtra("msg", message.getText().toString());
                if(attachment != null)
                    i.putExtra(Intent.EXTRA_STREAM, attachment);
                startService(i);
                finish();
            }
        });

        if(getIntent().hasExtra("handle") && getIntent().hasExtra("service")) {
            handle = getIntent().getStringExtra("handle");
            service = getIntent().getStringExtra("service");

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

            Bitmap b = BitmapFactory.decodeStream(ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contact, true));
            if (b != null) {
                ImageView photo = (ImageView) findViewById(R.id.photo);
                photo.setImageBitmap(b);
            }

            Cursor cursor = getContentResolver().query(contact, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                ((TextView)findViewById(R.id.name)).setText(cursor.getString(0));
                cursor.close();
            }
            ((TextView)findViewById(R.id.protocol)).setText(protocol);
        } else if(getIntent() != null && getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            findViewById(R.id.contact).setVisibility(View.GONE);
            findViewById(R.id.spinner).setVisibility(View.VISIBLE);

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
}
