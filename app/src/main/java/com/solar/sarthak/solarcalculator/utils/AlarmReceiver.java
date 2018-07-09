package com.solar.sarthak.solarcalculator.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.solar.sarthak.solarcalculator.activities.HomeActivity;
import com.solar.sarthak.solarcalculator.R;

/**
 * Broadcast receiver which is used to send notification at the golden hour.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        sendNotification(context);
    }

    /**
     * Sends notification at the golden hour.
     */
    public void sendNotification(Context context) {

        String CHANNEL_ID = "channel_01";

        Intent intent = new Intent(context, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // get a reference to the NotificationManager
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // create a notification builder object
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        // set parameters to the builder
        builder.setContentTitle("Golden Hour");
        builder.setContentText("Golden hour has started. Get ready with your camera to shoot some amazing photographs. :)");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setStyle(new NotificationCompat.BigTextStyle());
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);

        // set channel id if android version is Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                    "Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationManager.createNotificationChannel(mChannel);

            builder.setChannelId(CHANNEL_ID);
        }

        // create notification
        notificationManager.notify(0, builder.build());
    }
}
