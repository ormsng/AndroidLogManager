package com.example.logviewer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

public class LogService extends Service {
    private static final String TAG = "LogService";
    public static final String ACTION_LOG_UPDATED = "com.example.logviewer.LOG_UPDATED";
    public static final String EXTRA_LOG = "extra_log";

    private List<LogEntry> logs = new ArrayList<>();
    private BroadcastReceiver logReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        logReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String type = intent.getStringExtra("type");
                String tag = intent.getStringExtra("tag");
                String message = intent.getStringExtra("message");
                String timestamp = intent.getStringExtra("timestamp");
                String fileName = intent.getStringExtra("fileName");
                int lineNumber = intent.getIntExtra("lineNumber", 0);

                message = stripAnsiEscapeCodes(message);

                // Format the log entry with all information in a single line
                String formattedMessage = String.format("%s: [%s] %s - %s\nFile: %s, Line: %d",
                        tag, type, timestamp, message,
                        (fileName != null) ? fileName : "Unknown", lineNumber);

                Log.d(TAG, "Broadcast received: " + formattedMessage);

                LogEntry logEntry = new LogEntry(LogType.valueOf(type), tag, formattedMessage, timestamp);
                logs.add(logEntry);

                Intent logIntent = new Intent(ACTION_LOG_UPDATED);
                logIntent.putExtra(EXTRA_LOG, logEntry);
                LocalBroadcastManager.getInstance(context).sendBroadcast(logIntent);
            }
        };

        IntentFilter filter = new IntentFilter("com.example.logviewer.LOG_BROADCAST");
        registerReceiver(logReceiver, filter);
        Log.d(TAG, "Broadcast receiver registered in service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(logReceiver);
        Log.d(TAG, "Broadcast receiver unregistered in service");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String stripAnsiEscapeCodes(String input) {
        return input.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}