package tv.newtv.cboxtv.uc.bean;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.newtv.libs.Constant;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.RxBus;
import com.newtv.libs.util.SPrefUtils;
import com.newtv.libs.util.SharePreferenceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;


import tv.newtv.cboxtv.LauncherApplication;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by simple on 16/12/20.
 * <p>
 * 下载监听
 */

public class DownloadReceiver extends BroadcastReceiver {
    private static final String TAG = DownloadReceiver.class.getSimpleName();
    private static final String UPDATA_KEY = "updata_key";
    private static long downId;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != intent) {
            Bundle bundle = intent.getExtras();
            if (null != bundle) {
                downId = bundle.getLong(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                SPrefUtils.setValue(context, UPDATA_KEY, downId);
                Log.d(TAG, "DownLoadReceiver onReceive : " + downId);
                //下载完成或点击通知栏
                if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE) ||
                        intent.getAction().equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
                    Toast.makeText(LauncherApplication.AppContext, "下载完成", Toast.LENGTH_SHORT).show();
                    sendMsgByIntentService(context);
                }
            }
        }
    }

    private void sendMsgByIntentService(Context context) {
        Log.d(TAG, "sendMsgByIntentService");
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction("startIntentService");
        context.startService(intent);
    }

    public static class MyIntentService extends IntentService {
        public MyIntentService() {
            super("MyIntentService");
            Log.d(TAG, "MyIntentService");
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            Log.d(TAG, "onHandleIntent : " + intent.getAction());
            if ("startIntentService".equals(intent.getAction())) {
                if (0 <= downId) {
                    downId = (long) SPrefUtils.getValue(LauncherApplication.AppContext, UPDATA_KEY, downId);
                }
                queryFileUri(LauncherApplication.AppContext, downId);
            }
        }
    }

    private static void queryFileUri(Context context, long downloadApkId) {
        Log.d(TAG, "downloadApkId : " + downloadApkId);
        DownloadManager dManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadApkId);
        Cursor c = dManager.query(query);
        if (c != null && c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            Log.d(TAG, "DownLoadReceiver status : " + status);
            switch (status) {
                case DownloadManager.STATUS_PENDING:

                    break;
                case DownloadManager.STATUS_PAUSED:

                    break;
                case DownloadManager.STATUS_RUNNING:
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    Log.e(TAG, "queryFileUri: STATUS_SUCCESSFUL");
                    String downloadFileUrl = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    Log.e(TAG, "downloadFileUrl=" + downloadFileUrl);
                    installApk(context, Uri.parse(downloadFileUrl));
//                    context.unregisterReceiver();
                    break;
                case DownloadManager.STATUS_FAILED:

                    Updater.showToast(context, "下载失败，开始重新下载...");
                    break;
            }
            c.close();
        }
    }

    private static void installApk(Context context, Uri uri) {
        Log.e(TAG, "----installApk: ----");
        File file = new File(uri.getPath());
        SharePreferenceUtils.saveUpdateApkPath(context, uri.getPath());
        if (!file.exists()) {
            Log.e(TAG, "----installApk: ----!file.exists()");
            return;
        }
        startInstallApk(file);
    }

    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            LogUtils.e(e.toString());
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static void startInstallApk(File apkFile) {
        //更新包文件
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 24) { // 7.0以上
            Log.d(TAG, " > 24");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(LauncherApplication.AppContext,
                    LauncherApplication.AppContext.getPackageName() + ".fileProvider", apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            Log.d(TAG, " < 24");
            Uri uri = null;
            long completeDownLoadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DownloadManager mDownloadManager = (DownloadManager) LauncherApplication.AppContext
                    .getSystemService(Context.DOWNLOAD_SERVICE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // 6.0以下
                if (downId <= 0) {
                    downId = (long) SPrefUtils.getValue(LauncherApplication.AppContext, UPDATA_KEY, 0L);
                }
                uri = mDownloadManager.getUriForDownloadedFile(downId);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // 6.0 - 7.0
                uri = Uri.fromFile(apkFile);
            }
            try {
                Runtime.getRuntime().exec("chmod 777 " + apkFile.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        }

        SharedPreferences pref;
        pref = LauncherApplication.AppContext.getSharedPreferences("VersionMd5", MODE_PRIVATE);
        String versionmd5 = pref.getString("versionmd5", null);
        Log.e(TAG, "versionmd5 : " + versionmd5 + "");
        String fileMD5 = getFileMD5(apkFile);
        Log.e(TAG, "fileMD5 : " + fileMD5 + "");
        if (!TextUtils.isEmpty(versionmd5) && !TextUtils.isEmpty(fileMD5)) {
            if (versionmd5.equals(fileMD5)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(Intent.ACTION_VIEW);
                LauncherApplication.AppContext.startActivity(intent);
            } else {
                RxBus.get().post(Constant.UP_VERSION_IS_SUCCESS, "version_up_faild");
                Toast.makeText(LauncherApplication.AppContext, "下载文件有误,请返回页面,重新下载", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
