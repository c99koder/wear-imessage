/*
 * Copyright (c) 2015 Sam Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.c99.wear_imessage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class MainActivity extends ActionBarActivity {
    private TextView gcm_id;
    private TextView host;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gcm_id = (TextView) findViewById(R.id.gcm_id);
        host = (TextView) findViewById(R.id.host);

        findViewById(R.id.saveBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor e = getSharedPreferences("prefs", 0).edit();
                e.putString("host", host.getText().toString());
                e.apply();
                Toast.makeText(MainActivity.this, "Hostname saved", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.copyBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gcm_id.setText(getSharedPreferences("prefs", 0).getString("gcm_reg_id", ""));
                @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    android.content.ClipData clip = android.content.ClipData.newPlainText("GCM ID", getSharedPreferences("prefs", 0).getString("gcm_reg_id", ""));
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "GCM ID copied to clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to copy message. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            gcm_id.setText(getSharedPreferences("prefs", 0).getString("gcm_reg_id", ""));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        getSharedPreferences("prefs", 0).registerOnSharedPreferenceChangeListener(listener);

        if(checkPlayServices() && GCMIntentService.getRegistrationId(MainActivity.this).length() == 0) {
            GCMIntentService.scheduleRegisterTimer(MainActivity.this, 100);
        }

        gcm_id.setText(getSharedPreferences("prefs", 0).getString("gcm_reg_id", ""));
        host.setText(getSharedPreferences("prefs", 0).getString("host", ""));

        if(GCMIntentService.ENABLE_REPLIES) {
            AccountManager am = AccountManager.get(this);
            if (am.getAccountsByType("org.c99.wear_imessage.account").length == 0) {
                Log.e("iMessage", "Sync account not found, adding new one");
                Account account = new Account(getResources().getString(R.string.app_name), "org.c99.wear_imessage.account");
                am.addAccountExplicitly(account, null, null);
                ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        getSharedPreferences("prefs", 0).unregisterOnSharedPreferenceChangeListener(listener);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 9000).show();
            }
            return false;
        }
        return true;
    }
}
