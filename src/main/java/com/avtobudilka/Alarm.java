package com.avtobudilka;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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
            m.setPassiveListenerServiceAsync(SleepService.class,
                    PassiveListenerConfig.builder().setShouldUserActivityInfoBeRequested(true).build());
        } else {
            m.clearPassiveListenerServiceAsync();
        }
    }

    /** Nastavi glavni alarm (in rezervnega, ce je vklopljen) glede na cas, ko si zaspal. */
    static void arm(Context c, long asleepAt) {
        long t = asleepAt + SLEEP;
        if (t < System.currentTimeMillis()) return;
        set(c, 0, t);
        int extra = prefs(c).getInt("extra", 0);
        if (extra > 0) set(c, 1, t + extra * 60_000L);
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
    public void onReceive(Context c, Intent i) { // BOOT_COMPLETED: alarmi se ob restartu izbrisejo
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("on", false)) return;
        listen(c, true);
        arm(c, p.getLong("last", 0));
    }
}
