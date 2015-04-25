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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.util.Arrays;


public class QuickReplyActivity extends ActionBarActivity {
    String handle;
    String service;
    String protocol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Uri contact = null;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_reply);

        final EditText message = (EditText)findViewById(R.id.message);
        ImageButton send = (ImageButton)findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                i.putExtra("handle", handle);
                i.putExtra("service", service);
                i.putExtra("msg", message.getText().toString());
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
            Bitmap b = BitmapFactory.decodeStream(ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contact, true));
            if (b != null) {
                ImageView photo = (ImageView) findViewById(R.id.photo);
                photo.setImageBitmap(b);
            }

            Cursor cursor = getContentResolver().query(contact, new String[] { ContactsContract.Contacts.DISPLAY_NAME }, null, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                ((TextView)findViewById(R.id.name)).setText(cursor.getString(0));
                cursor.close();
            }
            ((TextView)findViewById(R.id.protocol)).setText(protocol);
        }
        getSupportActionBar().hide();
    }
}
