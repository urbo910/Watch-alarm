package com.avtobudilka;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.text.format.DateFormat;

import androidx.health.services.client.HealthServices;
import androidx.health.services.client.PassiveMonitoringClient;
import androidx.health.services.client.data.PassiveListenerConfig;

/** Pomozne funkcije + ob ponovnem zagonu ure spet vklopi vse. */
public class Alarm extends BroadcastReceiver {

    static final long SLEEP = (7 * 60 + 30) * 60_000L; // 7 h 30 min

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("p", Context.MODE_PRIVATE);
    }

    /** Vklopi/izklopi poslusanje "uporabnik spi" (Health Services). Ura sama zbudi app. */
    static void listen(Context c, boolean on) {
        PassiveMonitoringClient m = HealthServices.getClient(c).getPassiveMonitoringClient();
        if (on) {
            var f = m.setPassiveListenerServiceAsync(SleepService.class,
                    PassiveListenerConfig.builder().setShouldUserActivityInfoBeRequested(true).build());
            f.addListener(() -> { // ce prijava ne uspe, to pokazemo v aplikaciji
                try { f.get(); } catch (Exception e) { debug(c, "Napaka: " + e.getCause()); }
            }, Runnable::run);
        } else {
            m.clearPassiveListenerServiceAsync();
        }
    }

    /** Zadnje stanje, ki ga je poslala ura (za iskanje napak). */
    static void debug(Context c, String s) {
        prefs(c).edit().putString("dbg", s).apply();
    }

    /** Nastavi glavni alarm (in rezervnega, ce je vklopljen) glede na cas, ko si zaspal. */
    static void arm(Context c, long asleepAt) {
        long t = asleepAt + SLEEP;
        if (t < System.currentTimeMillis()) return;
        set(c, 0, t);
        int extra = prefs(c).getInt("extra", 0);
        if (extra > 0) set(c, 1, t + extra * 60_000L);
        notify(c, t, t + (extra + 3) * 60_000L);
    }

    /** Tiho obvestilo "Alarm ob X" z gumbom Preklici (npr. ce si samo zadremal). */
    static void notify(Context c, long t, long until) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("a", "Alarm", NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop = PendingIntent.getBroadcast(c, 0,
                new Intent(c, Alarm.class).setAction("stop"), PendingIntent.FLAG_IMMUTABLE);
        Icon x = Icon.createWithResource(c, android.R.drawable.ic_menu_close_clear_cancel);
        nm.notify(1, new Notification.Builder(c, "a")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alarm ob " + DateFormat.getTimeFormat(c).format(t))
                .setContentText("Si samo zadremal? Preklici.")
                .addAction(new Notification.Action.Builder(x, "Preklici", stop).build())
                .setTimeoutAfter(until - System.currentTimeMillis()) // samo izgine po alarmu
                .build());
    }

    /** Preklice oba alarma + obvestilo. Naslednje zaspanje spet nastavi alarm. */
    static void stop(Context c) {
        cancel(c, 0);
        cancel(c, 1);
        c.getSystemService(NotificationManager.class).cancel(1);
        prefs(c).edit().remove("last").apply();
    }

    static void set(Context c, int id, long time) {
        PendingIntent show = PendingIntent.getActivity(c, 9, new Intent(c, Main.class), PendingIntent.FLAG_IMMUTABLE);
        c.getSystemService(AlarmManager.class).setAlarmClock(new AlarmManager.AlarmClockInfo(time, show), ring(c, id));
    }

    static void cancel(Context c, int id) {
        c.getSystemService(AlarmManager.class).cancel(ring(c, id));
    }

    private static PendingIntent ring(Context c, int id) {
        Intent i = new Intent(c, Ring.class).putExtra("id", id);
        return PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if ("stop".equals(i.getAction())) { stop(c); return; } // gumb Preklici
        // BOOT_COMPLETED: alarmi se ob restartu izbrisejo, zato jih nastavimo znova
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("on", false)) return;
        listen(c, true);
        arm(c, p.getLong("last", 0));
    }
}
