package com.diamon.ttl.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogHelper {

    private final Activity activity;
    private final TextView tvLog;
    private final ScrollView scrollLog;
    private final SimpleDateFormat dateFormat;

    public LogHelper(Activity activity, TextView tvLog, ScrollView scrollLog) {
        this.activity = activity;
        this.tvLog = tvLog;
        this.scrollLog = scrollLog;
        this.dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    public void log(final String message) {
        activity.runOnUiThread(() -> {
            if (tvLog == null || scrollLog == null)
                return;
            String time = dateFormat.format(new Date());
            tvLog.append("[" + time + "] " + message + "\n");

            // Auto scroll down
            scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
        });
    }
}
