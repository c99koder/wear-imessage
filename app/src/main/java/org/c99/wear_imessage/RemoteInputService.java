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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.UUID;

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

                    if(intent.hasExtra(Intent.EXTRA_STREAM)) {
                        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                                .setContentTitle("Uploading File")
                                .setProgress(0, 0, true)
                                .setLocalOnly(true)
                                .setOngoing(true)
                                .setSmallIcon(android.R.drawable.stat_sys_upload);
                        NotificationManagerCompat.from(this).notify(1337, notification.build());

                        InputStream fileIn = null;
                        InputStream responseIn = null;
                        HttpURLConnection http = null;
                        try {
                            String filename = "";
                            int total = 0;
                            String type = getContentResolver().getType((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
                            if(type == null || type.length() == 0)
                                type = "application/octet-stream";
                            fileIn = getContentResolver().openInputStream((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));

                            Cursor c = getContentResolver().query((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM), new String[]{OpenableColumns.SIZE, OpenableColumns.DISPLAY_NAME}, null, null, null);
                            if (c != null && c.moveToFirst()) {
                                total = c.getInt(0);
                                filename = c.getString(1);
                                c.close();
                            } else {
                                total = fileIn.available();
                            }

                            String boundary = UUID.randomUUID().toString();
                            http = (HttpURLConnection) new URL("http://" + getSharedPreferences("prefs", 0).getString("host", "") + "/upload").openConnection();
                            http.setReadTimeout(60000);
                            http.setConnectTimeout(60000);
                            http.setDoOutput(true);
                            http.setFixedLengthStreamingMode(total + (boundary.length() * 5) + filename.length() + type.length() + intent.getStringExtra("handle").length() + intent.getStringExtra("service").length() + reply.length() + 99 + 88 + 64);
                            http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                            OutputStream out = http.getOutputStream();
                            out.write(("--" + boundary + "\r\n").getBytes());
                            out.write(("Content-Disposition: form-data; name=\"handle\"\r\n\r\n").getBytes());
                            out.write((intent.getStringExtra("handle") + "\r\n").getBytes());
                            out.write(("--" + boundary + "\r\n").getBytes());
                            out.write(("Content-Disposition: form-data; name=\"service\"\r\n\r\n").getBytes());
                            out.write((intent.getStringExtra("service") + "\r\n").getBytes());
                            out.write(("--" + boundary + "\r\n").getBytes());
                            out.write(("Content-Disposition: form-data; name=\"msg\"\r\n\r\n").getBytes());
                            out.write((intent.getStringExtra("msg") + "\r\n").getBytes());
                            out.write(("--" + boundary + "\r\n").getBytes());
                            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes());
                            out.write(("Content-Type: " + type + "\r\n\r\n").getBytes());

                            byte[] buffer = new byte[8192];
                            int count = 0;
                            int n = 0;
                            while (-1 != (n = fileIn.read(buffer))) {
                                out.write(buffer, 0, n);
                                count += n;

                                float progress = (float)count / (float)total;
                                if(progress < 1.0f)
                                    notification.setProgress(1000, (int) (progress * 1000), false);
                                else
                                    notification.setProgress(0, 0, true);
                                NotificationManagerCompat.from(this).notify(1337, notification.build());
                            }

                            out.write(("\r\n--" + boundary + "--\r\n").getBytes());
                            out.flush();
                            out.close();
                            if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                responseIn = http.getInputStream();
                                StringBuilder sb = new StringBuilder();
                                Scanner scanner = new Scanner(responseIn).useDelimiter("\\A");
                                while (scanner.hasNext()) {
                                    sb.append(scanner.next());
                                }
                                android.util.Log.i("iMessage", "Upload result: " + sb.toString());
                            } else {
                                responseIn = http.getErrorStream();
                                StringBuilder sb = new StringBuilder();
                                Scanner scanner = new Scanner(responseIn).useDelimiter("\\A");
                                while (scanner.hasNext()) {
                                    sb.append(scanner.next());
                                }
                                android.util.Log.e("iMessage", "Upload failed: " + sb.toString());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (responseIn != null)
                                    responseIn.close();
                            } catch (Exception ignore) {
                            }
                            try {
                                if (http != null)
                                    http.disconnect();
                            } catch (Exception ignore) {
                            }
                            try {
                                fileIn.close();
                            } catch (Exception ignore) {
                            }
                        }
                        NotificationManagerCompat.from(this).cancel(1337);
                    } else if (reply.length() > 0) {
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
                }
            }
        }
    }
}
