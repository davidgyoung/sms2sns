package com.davidgyoungtech.sms2sns;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by dyoung on 5/1/18.
 */

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = SmsReceiver.class.getSimpleName();
    private static final String AMAZON_URL = /* ####  YOU MUST PASTE YOUR URL HERE, SEE README.MD #### */ ;
    private static final String MESSAGE_PREFIX_FOR_FORWARDING = "device_uuid:";
    private boolean mForwardingInProgress = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive called");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            // get sms objects
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus.length == 0) {
                Log.w(TAG, "onReceive called with no pdus");
                return;
            }
            // large message might be broken into many
            SmsMessage[] messages = new SmsMessage[pdus.length];
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pdus.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                sb.append(messages[i].getMessageBody());
            }
            String sender = messages[0].getOriginatingAddress();
            String message = sb.toString();
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(context);
            Intent notificationIntent = new Intent(context, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context,0,notificationIntent,0);

            //set
            builder.setContentIntent(contentIntent);
            builder.setSmallIcon(R.drawable.ic_launcher_background);
            builder.setContentText(message);

            boolean forward = true;
            if (message.startsWith(MESSAGE_PREFIX_FOR_FORWARDING)) {
                Log.w(TAG, "SMS message has the prefix we care about.  Processing.");
                builder.setContentTitle("Forwarding SMS received");
            }
            else {
                Log.w(TAG, "SMS message does not have the prefix we care about.  Ignoring.");
                builder.setContentTitle("Non-Forwarding SMS received");
                forward = false;
            }
            builder.setAutoCancel(true);
            builder.setDefaults(Notification.DEFAULT_ALL);

            Notification notification = builder.build();
            notificationManager.notify((int)System.currentTimeMillis(),notification);

            if (forward) {
                String uuid = UUID.randomUUID().toString();
                String messageJson = "{\\\"originationNumber\\\":\\\""+sender+"\\\",\\\"messageBody\\\":\\\""+message+"\\\",\\\"inboundMessageId\\\":\\\""+uuid+"\\\",\\\"previousPublishedMessageId\\\":null,\\\"messageKeyword\\\":\\\""+MESSAGE_PREFIX_FOR_FORWARDING+"\\\",\\\"destinationNumber\\\":null}";
                String body = "{\"sns_message\":\""+messageJson+"\"}";

                mForwardingInProgress = true;
                RestRequest apiClient = new RestRequest();
                apiClient.makeRequest(
                        AMAZON_URL,
                        "PUT",
                        body,
                        apiClient.getHeadersForJsonRequestWithBody(),
                        new RestRequest.RestResponseHandler() {
                            @Override
                            public void onFail(Exception e) {
                                mForwardingInProgress = false;
                                Log.w(TAG, "Failed to send message to SNS using URL "+AMAZON_URL, e);
                            }

                            @Override
                            public void onResponse(int httpStatus, Map<String, List<String>> headers, String body) {
                                mForwardingInProgress = false;
                                if (httpStatus != 200) {
                                    Log.w(TAG, "Bad response code trying to send message to SNS using URL "+
                                            AMAZON_URL+": "+httpStatus+" response body: "+body);
                                }
                                else {
                                    Log.w(TAG, "Message SMS message successfuly forwarded to Amazon SNS");
                                }
                            }
                        }
                );
            }

        }
        else {
            Log.w(TAG, "onReceive called with nill bundle");
        }
    }
}
