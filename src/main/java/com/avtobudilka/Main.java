package com.avtobudilka;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** Nastavitve: vklop/izklop, rezervni alarm (brez / +5 / +10 min), zvok ali samo vibracija. */
public class Main extends Activity {

    private static final String PERM = Manifest.permission.ACTIVITY_RECOGNITION;
    private SharedPreferences p;
    private Switch onOff;
    private Button extra, sound;
    private TextView info;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Alarm.prefs(this);

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(40, 40, 40, 40);

        onOff = new Switch(this);
        onOff.setText("Avto budilka");
        onOff.setChecked(p.getBoolean("on", false));
        onOff.setOnCheckedChangeListener((v, on) -> {
            p.edit().putBoolean("on", on).apply();
            if (!on) Alarm.stop(this); // izklop: preklici vse
            sync();
        });
        l.addView(onOff);

        extra = new Button(this);
        extra.setOnClickListener(v -> {
            int e = p.getInt("extra", 0);
            p.edit().putInt("extra", e == 0 ? 5 : e == 5 ? 10 : 0).apply();
            Alarm.cancel(this, 1); // ce je alarm za nocoj ze nastavljen, posodobi rezervo
            if (p.getBoolean("on", false)) Alarm.arm(this, p.getLong("last", 0));
            show();
        });
        l.addView(extra);

        sound = new Button(this);
        sound.setOnClickListener(v -> {
            p.edit().putBoolean("sound", !p.getBoolean("sound", true)).apply();
            show();
        });
        l.addView(sound);

        info = new TextView(this);
        info.setGravity(Gravity.CENTER);
        l.addView(info);

        setContentView(l);
        sync();
    }

    private void sync() {
        boolean on = p.getBoolean("on", false);
        if (on && checkSelfPermission(PERM) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERM, Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        Alarm.listen(this, on);
        show();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED) sync();
        else onOff.setChecked(false); // brez dovoljenja ne gre
    }

    private void show() {
        int e = p.getInt("extra", 0);
        extra.setText(e == 0 ? "Rezerva: brez" : "Rezerva: +" + e + " min");
        sound.setText(p.getBoolean("sound", true) ? "Zvok + vibracija" : "Samo vibracija");

        long t = p.getLong("last", 0) + Alarm.SLEEP;
        if (!p.getBoolean("on", false)) info.setText("Izklopljeno");
        else if (t > System.currentTimeMillis()) info.setText("Alarm ob " + DateFormat.getTimeFormat(this).format(t));
        else info.setText("Cakam, da zaspis (7 h 30 min)");
        info.append("\n\nZadnje: " + p.getString("dbg", "se nic"));
    }
}
