package com.avtobudilka;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Zaslon, ki zvoni in vibrira. Po 2 min se sam ugasne (takrat pride na vrsto rezervni alarm). */
public class Ring extends Activity {

    private Vibrator vib;
    private Ringtone tone;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AudioAttributes alarm = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
        vib = getSystemService(Vibrator.class);
        vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 800, 400}, 0), alarm);
        tone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        if (tone != null) {
            tone.setAudioAttributes(alarm);
            tone.setLooping(true);
            tone.play();
        }

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);

        TextView t = new TextView(this);
        t.setText("Dobro jutro!");
        t.setTextSize(22);
        l.addView(t);

        Button stop = new Button(this);
        stop.setText("Ustavi");
        stop.setOnClickListener(v -> finish());
        l.addView(stop);

        // Pri glavnem alarmu lahko preklices se rezervnega
        if (getIntent().getIntExtra("id", 0) == 0 && Alarm.prefs(this).getInt("extra", 0) > 0) {
            Button all = new Button(this);
            all.setText("Ustavi vse");
            all.setOnClickListener(v -> { Alarm.cancel(this, 1); finish(); });
            l.addView(all);
        }
        setContentView(l);

        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2 * 60_000L);
    }

    @Override
    protected void onDestroy() {
        vib.cancel();
        if (tone != null) tone.stop();
        super.onDestroy();
    }
}
