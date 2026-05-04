package com.alertalinkedin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.alertalinkedin.db.AppDatabase;
import com.alertalinkedin.db.JobEntity;
import com.alertalinkedin.db.KeywordEntity;

import java.util.List;

public class MonitoringService extends Service {

    private static final int    FOREGROUND_NOTIF_ID = 1;
    private static final String FOREGROUND_CHANNEL  = "monitor_running";
    private static final String ACTION_REPOST       = "com.alertalinkedin.ACTION_REPOST_FOREGROUND";

    private volatile boolean running = false;
    private Thread monitorThread;

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, MonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
    }

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, MonitoringService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createForegroundChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FOREGROUND_NOTIF_ID, buildForegroundNotification());

        if (ACTION_REPOST.equals(intent != null ? intent.getAction() : null)) {
            return START_STICKY;
        }

        if (!running) {
            running = true;
            monitorThread = new Thread(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    checkJobs();
                    try {
                        long ms = PrefsManager.getIntervalMinutes(this) * 60_000L;
                        Thread.sleep(ms);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            monitorThread.start();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (monitorThread != null) monitorThread.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkJobs() {
        AppDatabase db = AppDatabase.getInstance(this);
        
        // Limpeza semanal: remove registros com mais de 7 dias
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        db.jobDao().deleteOlderThan(sevenDaysAgo);

        List<KeywordEntity> keywords = db.keywordDao().getAllSync();
        if (keywords.isEmpty()) return;

        String location  = PrefsManager.getLocation(this);
        String timeRange = PrefsManager.getTimeRange(this);
        LinkedInScraper scraper = new LinkedInScraper();

        for (KeywordEntity kw : keywords) {
            if (!running) break;
            List<Job> jobs = scraper.searchJobs(kw.value, location, timeRange);
            for (Job job : jobs) {
                if (isJobTooOld(job.getTimeAgo(), timeRange)) continue;

                if (!db.jobDao().exists(job.getId())) {
                    db.jobDao().insert(new JobEntity(
                        job.getId(), job.getTitle(), job.getCompany(),
                        job.getLocation(), job.getUrl(), job.getKeyword()
                    ));
                    JobNotifier.notify(this, job);
                }
            }
        }
    }

    private boolean isJobTooOld(String timeAgo, String timeRange) {
        if (timeAgo == null || timeAgo.isEmpty()) return false;
        String lower = timeAgo.toLowerCase();

        if (timeRange.equals("r3600")) { // 1 hora
            if (lower.contains("hora") || lower.contains("hour")) {
                // "2 horas" ou mais é antigo
                return !lower.startsWith("1") && !lower.contains(" 1 ");
            }
            return lower.contains("dia") || lower.contains("day") ||
                   lower.contains("semana") || lower.contains("week") ||
                   lower.contains("mês") || lower.contains("mes") || lower.contains("month") ||
                   lower.contains("ano") || lower.contains("year");
        }
        if (timeRange.equals("r86400")) { // 24 horas
            return lower.contains("dia") || lower.contains("day") ||
                   lower.contains("semana") || lower.contains("week") ||
                   lower.contains("mês") || lower.contains("mes") || lower.contains("month") ||
                   lower.contains("ano") || lower.contains("year");
        }
        if (timeRange.equals("r604800")) { // 1 semana
            if (lower.contains("semana") || lower.contains("week")) {
                // "2 semanas" ou mais é antigo
                return !lower.startsWith("1") && !lower.contains(" 1 ");
            }
            return lower.contains("mês") || lower.contains("mes") || lower.contains("month") ||
                   lower.contains("ano") || lower.contains("year");
        }
        return false;
    }

    private void createForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                FOREGROUND_CHANNEL,
                "Monitoramento ativo",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Indica que o app está monitorando vagas em segundo plano");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildForegroundNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, open, PendingIntent.FLAG_IMMUTABLE);

        Intent repost = new Intent(this, MonitoringService.class);
        repost.setAction(ACTION_REPOST);
        PendingIntent deletePi = PendingIntent.getService(
            this, 1, repost, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        long interval = PrefsManager.getIntervalMinutes(this);
        String intervalText = interval == 1 ? "1 minuto" : interval + " minutos";

        Notification n = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("Monitorando vagas no LinkedIn")
            .setContentText("Verificando a cada " + intervalText)
            .setContentIntent(pi)
            .setDeleteIntent(deletePi)
            .setOngoing(true)
            .setSilent(true)
            .build();
        n.flags |= Notification.FLAG_NO_CLEAR;
        return n;
    }
}
