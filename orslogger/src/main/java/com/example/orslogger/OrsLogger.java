package com.example.orslogger;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrsLogger {
    private static final String RESET = "\u001B[0m";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private static Context applicationContext;

    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public enum LogType {
        DEBUG("\u001B[34m"),  // Blue
        INFO("\u001B[32m"),   // Green
        ERROR("\u001B[31m"),  // Red
        WARNING("\u001B[33m"), // Yellow
        VERBOSE("\u001B[35m"); // Purple

        private final String color;

        LogType(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    public static void log(LogType type, String tag, String message, Throwable throwable) {
        String timestamp = DATE_FORMAT.format(new Date());
        StackTraceElement caller = Thread.currentThread().getStackTrace()[4];

        // Send log to LogViewer app
        if (applicationContext != null) {
            Intent intent = new Intent("com.example.logviewer.LOG_BROADCAST");
            intent.putExtra("type", type.name());
            intent.putExtra("tag", tag);
            intent.putExtra("message", message);
            intent.putExtra("timestamp", timestamp);
            intent.putExtra("fileName", caller.getFileName());
            intent.putExtra("lineNumber", caller.getLineNumber());
            applicationContext.sendBroadcast(intent);
        }

        // Also print to console for debugging
        System.out.println(String.format("%s: [%s] %s - %s", tag, type, timestamp, message));

        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    public static void d(String tag, String message) {
        log(LogType.DEBUG, tag, message, null);
    }

    public static void i(String tag, String message) {
        log(LogType.INFO, tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        log(LogType.ERROR, tag, message, throwable);
    }

    public static void w(String tag, String message) {
        log(LogType.WARNING, tag, message, null);
    }

    public static void v(String tag, String message) {
        log(LogType.VERBOSE, tag, message, null);
    }
}
