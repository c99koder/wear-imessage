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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;

public class RemoteInputService extends IntentService {
    public static final String ACTION_REPLY = "org.c99.wear_imessage.ACTION_REPLY";

    public RemoteInputService() {
        super("RemoteInputService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null || intent.hasExtra("msg")) {
                    String reply = remoteInput != null?remoteInput.getCharSequence("extra_reply").toString():intent.getStringExtra("msg");
                    if (reply.length() > 0) {
                        URL url = null;
                        try {
                            url = new URL("http://" + getSharedPreferences("prefs", 0).getString("host", "") + "/send?service=" + intent.getStringExtra("service") + "&handle=" + intent.getStringExtra("handle") + "&msg=" + URLEncoder.encode(reply, "UTF-8"));
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
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        conn.disconnect();
                    }
                    NotificationManagerCompat.from(this.getApplicationContext()).cancel(intent.getIntExtra("notification_id", 0));
                }
            }
        }
    }
}
