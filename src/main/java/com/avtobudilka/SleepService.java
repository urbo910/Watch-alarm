package com.avtobudilka;

import android.content.SharedPreferences;

import androidx.health.services.client.PassiveListenerService;
import androidx.health.services.client.data.UserActivityInfo;
import androidx.health.services.client.data.UserActivityState;

/** Ura poklice to, ko se spremeni stanje (spi / buden / vadba). App ves cas ne tece. */
public class SleepService extends PassiveListenerService {

    static final long NIGHT = 12 * 60 * 60_000L; // v 12 h uposteva samo prvo zaspanje

    @Override
    public void onUserActivityInfoReceived(UserActivityInfo info) {
        if (info.getUserActivityState() != UserActivityState.USER_ACTIVITY_ASLEEP) return;

        SharedPreferences p = Alarm.prefs(this);
        long now = System.currentTimeMillis();
        if (!p.getBoolean("on", false) || now - p.getLong("last", 0) < NIGHT) return;

        p.edit().putLong("last", now).apply();
        Alarm.arm(this, now);
    }
}
