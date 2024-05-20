package com.yutak.im.kit;

import android.annotation.SuppressLint;
import android.util.Log;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class LogKit {
    private final String TAG = "YutakLogger" + YutakIM.get().getVersion();
    //Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    private final String FILE_NAME = "wkLogger_" + YutakIM.get().getVersion() + ".log";

    private final static LogKit logKit = new LogKit();
    private LogKit() {}

    public static LogKit get(){return logKit;}
    public void e(String msg) {
        error(msg);
    }

    public void e(Exception e) {
        error(e);
    }
    public void error(Exception e) {
        StringBuilder sb = new StringBuilder();
        String name = getFunctionName();
        StackTraceElement[] sts = e.getStackTrace();

        if (name != null) {
            sb.append(name).append(" - ").append(e).append("\r\n");
        } else {
            sb.append(e).append("\r\n");
        }
        if (sts.length > 0) {
            for (StackTraceElement st : sts) {
                if (st != null) {
                    sb.append("[ ").append(st.getFileName()).append(":").append(st.getLineNumber()).append(" ]\r\n");
                }
            }
        }
        if (YutakIM.get().isDebug()) {
            Log.e(TAG, sb.toString());
        }
        if (YutakIM.get().isDebug()) {
            writeLog(sb.toString());
        }
    }
    private String createMessage(String msg) {
        String functionName = getFunctionName();
        return (functionName == null ? msg : (functionName + " - " + msg));
    }
    public void error(String msg) {
        String message = createMessage(msg);
        if (YutakIM.get().isDebug()) {
            Log.e(TAG, message);
        }
        if (YutakIM.get().isDebug()) {
            writeLog(message);
        }
    }
    private String getFunctionName() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();

        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }

            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }

            if (st.getClassName().equals(this.getClass().getName())) {
                continue;
            }

            return "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId()
                    + "): " + st.getFileName() + ":" + st.getLineNumber() + "]";
        }

        return null;
    }
    private String getLogFilePath() {
        final String ROOT = Objects.requireNonNull(YutakApplication.get().getContext().getExternalFilesDir(null)).getAbsolutePath() + "/";
        return ROOT + FILE_NAME;
    }
    @SuppressLint("SimpleDateFormat")
    private void writeLog(String content) {
        try {
            if (YutakApplication.get().getContext() == null || !YutakIM.get().isWriteLog()) {
                return;
            }
            File file = new File(getLogFilePath());
            if (!file.exists()) {
                file.createNewFile();
            }
//			DateFormat formate = SimpleDateFormat.getDateTimeInstance();
            SimpleDateFormat formate = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            FileWriter write = new FileWriter(file, true);
            write.write(formate.format(new Date()) + "   " +
                    content + "\n");
            write.flush();
            write.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
