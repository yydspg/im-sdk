package com.yutak.im;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;

import com.yutak.im.db.DBHelper;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakSyncMsg;
import com.yutak.im.domain.YutakSyncMsgMode;
import com.yutak.im.kit.LogKit;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.UUID;

public class YutakApplication {
    private final String sharedName = "yutak_account_config";

    //协议版本
    public final byte defaultProtocolVersion = 4;
    public byte protocolVersion = 4;
    private WeakReference<Context> mContext;
    private static final YutakApplication yutakApplication = new YutakApplication();

    //    private String tempUid;
    private String tempRSAPublicKey;
    private DBHelper dbHelper;
    public boolean isCanConnect = true;
    private String fileDir = "yutak";
    private YutakSyncMsgMode syncMsgMode;

    public static YutakApplication get() {
        return yutakApplication;
    }

    public Context getContext() {
        if (mContext == null) {
            return null;
        }
        return mContext.get();
    }

    void initContext(Context context) {
        this.mContext = new WeakReference<>(context);
    }
    public YutakSyncMsgMode getSyncMsgMode() {
        if (syncMsgMode == null) syncMsgMode = YutakSyncMsgMode.READ;
        return syncMsgMode;
    }

    public void setYutakSyncMsgMode(YutakSyncMsgMode mode) {
        this.syncMsgMode = mode;
    }

    public YutakSyncMsgMode getYutakSyncMsgMode() {
        if (syncMsgMode == null) return YutakSyncMsgMode.READ;
        return syncMsgMode;
    }

    // 将一个RSA公钥字符串保存到Android应用的私有SharedPreferences存储空间
    public void setRSAPublicKey(String k) {
        if (mContext == null) return;
        tempRSAPublicKey = k;
        SharedPreferences setting = mContext.get().getSharedPreferences(sharedName, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = setting.edit();
        edit.putString("yutak_tempRSAPublicKey", k);
        edit.apply();
    }

    public String getRSAPublicKey() {
        if (mContext == null) {
            LogKit.get().e("no context");
            return "";
        }
        if (TextUtils.isEmpty(tempRSAPublicKey)) {
            SharedPreferences setting = mContext.get().getSharedPreferences(sharedName, Context.MODE_PRIVATE);
            tempRSAPublicKey = setting.getString("yutak_tempRSAPublicKey", "");
        }
        return tempRSAPublicKey;
    }

    public void setFileCacheDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public String getToken() {
        if (mContext == null) return "";
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        return setting.getString("Yutak_Token", "");
    }

    public void setToken(String token) {
        if (mContext == null) return;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putString("Yutak_Token", token);
        editor.apply();
    }

    public synchronized DBHelper getDbHelper() {
        if (dbHelper == null) {
            String uid = getUid();
            if (!TextUtils.isEmpty(uid)) {
                dbHelper =dbHelper.getInstance(mContext.get(), uid);
            } else {
                LogKit.get().e("获取DbHelper时用户ID为null");
            }
        }
        return dbHelper;
    }

    public void closeDbHelper() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    public long getDBUpgradeIndex() {
        if (mContext == null) return 0;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        return setting.getLong(getUid() + "_db_upgrade_index", 0);
    }

    public void setDBUpgradeIndex(long index) {
        if (mContext == null) return;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putLong(getUid() + "_db_upgrade_index", index);

    }
    public String getUid() {
        if (mContext == null) {
            LogKit.get().e("传入的context为空");
            return "";
        }
        String tempUid = "";
        if (TextUtils.isEmpty(tempUid)) {
            SharedPreferences setting = mContext.get().getSharedPreferences(
                    sharedName, Context.MODE_PRIVATE);
            tempUid = setting.getString("Yutak_UID", "");
        }
        return tempUid;
    }

    public void setUid(String uid) {
        if (mContext == null) return;
        // tempUid = uid;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putString("Yutak_UID", uid);
        editor.apply();
    }
    public String getFileCacheDir() {
        if (TextUtils.isEmpty(fileDir)) {
            fileDir = "YutakIM";
        }
        return Objects.requireNonNull(getContext().getExternalFilesDir(fileDir)).getAbsolutePath();
    }
    private void setDeviceId(String deviceId) {
        if (mContext == null) return;
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        if (TextUtils.isEmpty(deviceId))
            deviceId = UUID.randomUUID().toString().replaceAll("-", "");
        editor.putString(getUid() + "_Yutak_device_id", deviceId);
        editor.apply();
    }

    public String getDeviceId() {
        if (mContext == null) return "";
        SharedPreferences setting = mContext.get().getSharedPreferences(
                sharedName, Context.MODE_PRIVATE);
        String deviceId = setting.getString(getUid() + "_Yutak_device_id", "");
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = UUID.randomUUID().toString().replaceAll("-", "");
            setDeviceId(deviceId);
        }
        return deviceId + "ad";
    }
    public boolean isNetworkConnected() {
        if (mContext == null) {
            LogKit.get().e("检测网络的context为空--->");
            return false;
        }
        ConnectivityManager manager = (ConnectivityManager) mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities networkCapabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
                if (networkCapabilities != null) {
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                }
            } else {
                NetworkInfo networkInfo = manager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
//
//        ConnectivityManager connectivity = (ConnectivityManager) mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (connectivity != null) {
//                Network networks = connectivity.getActiveNetwork();
//                NetworkCapabilities networkCapabilities = connectivity.getNetworkCapabilities(networks);
//                if (networkCapabilities != null) {
//                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                        success = true;
//                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
//                        success = true;
//                    }
//                } else {
//                    success = false;
//                }
//            }
//
//        } else {
//            NetworkInfo.State state = connectivity.getNetworkInfo(
//                    ConnectivityManager.TYPE_WIFI).getState(); // 获取网络连接状态
//            if (NetworkInfo.State.CONNECTED == state) {
//                // 判断是否正在使用WIFI网络
//                success = true;
//            } else {
//                state = connectivity.getNetworkInfo(
//                        ConnectivityManager.TYPE_MOBILE).getState(); // 获取网络连接状态
//                if (NetworkInfo.State.CONNECTED == state) { // 判断是否正在使用GPRS网络
//                    success = true;
//                }
//            }
//        }

    }
}
