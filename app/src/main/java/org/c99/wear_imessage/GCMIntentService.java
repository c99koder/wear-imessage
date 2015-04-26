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
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.text.Spanned;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class GCMIntentService extends IntentService {
    public static final String GCM_ID = "YOUR-GCM-ID-HERE";
    public static final boolean ENABLE_REPLIES = false;

    private JSONObject conversations;

    public GCMIntentService() {
        super("GcmIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            conversations = new JSONObject(getSharedPreferences("data", 0).getString("conversations", "{}"));
        } catch (JSONException e) {
            conversations = new JSONObject();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        save();
    }

    private void save() {
        SharedPreferences.Editor e = getSharedPreferences("data", 0).edit();
        e.putString("conversations", conversations.toString());
        e.apply();
    }

    public static void notify(Context context, int notification_id, JSONArray msgs, Intent intent, boolean updateOnly) throws JSONException {
        String lastMsg = intent.getStringExtra("msg");
        Spanned msg;
        String name = intent.getStringExtra("name");
        if(name.contains(" "))
            name = name.substring(0, name.indexOf(" "));

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < msgs.length(); i++) {
            if(sb.length() > 0)
                sb.append("<br/><br/>");
            String body;
            String from = name;

            try {
                JSONObject o = msgs.getJSONObject(i);
                body = o.getString("msg");
                if(o.has("type")) {
                    if (o.getString("type").equals("sent") || o.getString("type").equals("sent_file"))
                        from = "Me";
                    else if (o.getString("type").equals("file") || o.getString("type").equals("sent_file"))
                        body = "[File: " + o.getString("msg") + "]";
                    else
                        lastMsg = body;
                } else {
                    from = name;
                    lastMsg = body;
                }
            } catch (JSONException e1) {
                from = name;
                body = msgs.getString(i);
                lastMsg = body;
            }

            sb.append("<b>").append(Html.escapeHtml(from)).append(":</b> ").append(Html.escapeHtml(body));
        }

        msg = Html.fromHtml(sb.toString());
        int msg_count = msgs.length();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(intent.getStringExtra("name"))
                .setContentText(lastMsg)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setNumber(msg_count);

        if(!updateOnly) {
            builder.setTicker(Html.fromHtml("<b>" + Html.escapeHtml(intent.getStringExtra("name")) + ":</b> " + Html.escapeHtml(intent.getStringExtra("msg"))))
                    .setDefaults(Notification.DEFAULT_ALL);
        }

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText(intent.getStringExtra("msg"));
        builder.setStyle(style);

        NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();

        if(ENABLE_REPLIES) {
            Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
            replyIntent.setComponent(new ComponentName(context.getPackageName(), RemoteInputService.class.getName()));
            replyIntent.putExtras(intent.getExtras());
            replyIntent.putExtra("notification_id", notification_id);
            PendingIntent replyPendingIntent = PendingIntent.getService(context, notification_id + 1, replyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            extender.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_reply,"Reply", replyPendingIntent).addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply").build()).build());

            Intent launchIntent = new Intent(context, QuickReplyActivity.class);
            launchIntent.putExtras(intent.getExtras());
            launchIntent.putExtra("notification_id", notification_id);
            builder.setContentIntent(PendingIntent.getActivity(context, notification_id + 2, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT));
            builder.addAction(R.drawable.ic_action_reply, "Reply", PendingIntent.getActivity(context, notification_id + 3, launchIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT));
        }

        if(msg_count > 1)
            extender.addPage(new NotificationCompat.Builder(context).setContentText(msg).extend(new NotificationCompat.WearableExtender().setStartScrollBottom(true)).build());

        Cursor c = null;
        if(intent.getStringExtra("handle").contains("@"))
        c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[] {ContactsContract.Data.CONTACT_ID}, ContactsContract.CommonDataKinds.Email.ADDRESS + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", new String[] {intent.getStringExtra("handle"), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE}, null);

        if(c != null && c.moveToFirst()) {
            long contactId = c.getLong(0);
            c.close();

            Bitmap b = BitmapFactory.decodeStream(ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId), true));
            if(b != null) {
                builder.setLargeIcon(b);
                extender.setBackground(b);
            }
        }

        NotificationManagerCompat m = NotificationManagerCompat.from(context);
        m.notify(notification_id, builder.extend(extender).build());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            String messageType = gcm.getMessageType(intent);

            if (extras != null && !extras.isEmpty()) {
                if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    int notificationId;
                    JSONObject conversation;

                    try {
                        String key = intent.getStringExtra("service") + ":" + intent.getStringExtra("handle");
                        if (conversations.has(key)) {
                            conversation = conversations.getJSONObject(key);
                        } else {
                            conversation = new JSONObject();
                            conversations.put(key, conversation);

                            long time = new Date().getTime();
                            String tmpStr = String.valueOf(time);
                            String last4Str = tmpStr.substring(tmpStr.length() - 5);
                            conversation.put("notification_id", Integer.valueOf(last4Str));
                            conversation.put("msgs", new JSONArray());
                        }

                        notificationId = conversation.getInt("notification_id");

                        JSONArray msgs = conversation.getJSONArray("msgs");
                        JSONObject m = new JSONObject();
                        m.put("msg", intent.getStringExtra("msg"));
                        m.put("service", intent.getStringExtra("service"));
                        m.put("handle", intent.getStringExtra("handle"));
                        m.put("type", intent.getStringExtra("type"));
                        msgs.put(m);

                        while (msgs.length() > 10) {
                            msgs.remove(0);
                        }

                        save();

                        notify(getApplicationContext(), notificationId, msgs, intent, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            GCMBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private static final Timer GCMTimer = new Timer("GCM-Registration-Timer");

    public static void scheduleRegisterTimer(Context ctx, int delay) {
        final Context context = ctx.getApplicationContext();
        final int retrydelay = (delay < 500) ? 500 : delay;

        GCMTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getRegistrationId(context).length() == 0) {
                    try {
                        String regId = GoogleCloudMessaging.getInstance(context).register(GCM_ID);
                        int appVersion = getAppVersion(context);
                        SharedPreferences.Editor editor = context.getSharedPreferences("prefs", 0).edit();
                        editor.putString("gcm_reg_id", regId);
                        editor.putInt("gcm_app_version", appVersion);
                        editor.putString("gcm_app_build", Build.FINGERPRINT);
                        editor.remove("gcm_registered");
                        editor.commit();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        scheduleRegisterTimer(context, retrydelay * 2);
                    }
                }
            }

        }, delay);

    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences("prefs", 0);
        String registrationId = prefs.getString("gcm_reg_id", "");
        if (registrationId != null && registrationId.length() == 0) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt("gcm_app_version", Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        String build = prefs.getString("gcm_app_build", "");
        if (!Build.FINGERPRINT.equals(build)) {
            return "";
        }
        return registrationId;
    }
}
