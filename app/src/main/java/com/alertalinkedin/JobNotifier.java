package com.alertalinkedin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.concurrent.atomic.AtomicInteger;

public class JobNotifier {
    private static final String CHANNEL_ID = "vagas_linkedin";
    private static final AtomicInteger NOTIF_ID = new AtomicInteger(0);

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Vagas LinkedIn",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alertas de novas vagas no LinkedIn");
            NotificationManager manager = ctx.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void notify(Context ctx, Job job) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(job.getUrl()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int id = NOTIF_ID.getAndIncrement();
        PendingIntent pendingIntent = PendingIntent.getActivity(
            ctx, id, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String time = (job.getTimeAgo() != null && !job.getTimeAgo().isEmpty()) ? " • " + job.getTimeAgo() : "";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nova vaga: " + job.getTitle())
            .setContentText(job.getCompany() + " • " + job.getLocation() + time)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(job.getCompany() + "\n" + job.getLocation() + time + "\nBusca: " + job.getKeyword()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
