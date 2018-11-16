package tv.newtv.cboxtv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ViewTarget;
import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.SubContent;
import com.newtv.cms.contract.ContentContract;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.db.DBCallback;
import com.newtv.libs.db.DBConfig;
import com.newtv.libs.db.DataSupport;
import com.newtv.libs.util.DeviceUtil;
import com.newtv.libs.util.DisplayUtils;
import com.newtv.libs.util.FileUtil;
import com.newtv.libs.util.LogUploadUtils;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.PicassoBuilder;
import com.newtv.libs.util.RxBus;
import com.newtv.libs.util.SharePreferenceUtils;
import com.newtv.libs.util.SystemUtils;
import com.newtv.libs.util.Utils;
import com.newtv.libs.util.YSLogUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import tv.icntv.adsdk.AdSDK;
import tv.newtv.cboxtv.player.Player;
import tv.newtv.cboxtv.player.PlayerObserver;
import tv.newtv.cboxtv.uc.v2.listener.INotifyLoginStatusCallback;
import tv.newtv.cboxtv.uc.v2.manager.UserCenterRecordManager;
import tv.newtv.cboxtv.uc.v2.sub.QueryUserStatusUtil;
import tv.newtv.cboxtv.utils.DBUtil;
import tv.newtv.cboxtv.utils.UserCenterUtils;
import tv.newtv.ottlauncher.db.History;

//import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by lixin on 2018/1/11.
 */

public class LauncherApplication extends MultiDexApplication implements PlayerObserver {

    public static Context AppContext = null;
    private ContentContract.Presenter mContentPresenter;

    private boolean isAdSDKInit = false;//广告初始化结果

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (BuildConfig.DEBUG) {
//            MultiDex.install(this);
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate() {
        super.onCreate();

        ViewTarget.setTagId(R.id.tag_glide_id);

        //解决Rxjava的onError()异常
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e("TAG", "throw test");
            }
        });


        if (BuildConfig.DEBUG) {
//            LeakCanary.install(this);
        }

        AppContext = this.getApplicationContext();
        mContentPresenter = new ContentContract.ContentPresenter(this, null);
        Libs.init(this, BuildConfig.APP_KEY, BuildConfig.CHANNEL_ID, BuildConfig.FLAVOR,
                BuildConfig.DEBUG, BuildConfig.CLIENT_ID);

        Player.get().attachObserver(this);
        DataSupport.init(getApplicationContext());

        //KeyHelper.init(getApplicationContext());
        initADsdk();

        initBugly();
        Observable<String> mBackNavObservable = RxBus.get().register(Constant.INIT_SDK);
        mBackNavObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String value) throws Exception {
                        if (Constant.INIT_ADSDK.equals(value)) {
                            initADsdk();
                        } else if (Constant.INIT_LOGSDK.equals(value)) {
                            LogUploadUtils.initSDk();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        LogUtils.e(throwable);
                    }
                });
        PicassoBuilder.init(this);

        Log.e(Constant.TAG, "Application onCreate : ");
        DisplayUtils.init(this);

        YSLogUtils.getInstance(getApplicationContext()).initTracker();//央视网日志初始化
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.i(Constant.TAG, "accept: " + throwable);
            }
        });
        UserCenterUtils.init();
    }

    /**
     * 初始化bugly
     */
    private void initBugly() {

//        CrashReport.initCrashReport(getApplicationContext(), "e8a44dd463", false);

    }

    @SuppressLint("CheckResult")
    private void initADsdk() {
        //日志上传sdk初始化
        if (isAdSDKInit) {
            return;
        }
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                e.onNext(AdSDK.getInstance().init(Constant.BASE_URL_AD,
                        SystemUtils.getMac(AppContext),
                        BuildConfig.APP_KEY, BuildConfig.CHANNEL_ID,
                        FileUtil.getCacheDirectory(getApplicationContext(), "ad_cache")
                                .getAbsolutePath()));//广告初始化
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean value) throws Exception {
                        isAdSDKInit = value;
                        Log.e("adASD", "广告初始化结果=" + value);
                    }
                });
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Glide.get(this).clearMemory();
            PicassoBuilder.getBuilder().clear();
        }
        Glide.get(this).trimMemory(level);

        LogUtils.d(String.format(Locale.getDefault(), "trimmemory level=%d", level));
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Glide.get(this).clearMemory();
        PicassoBuilder.getBuilder().clear();
    }

    @Override
    public void onFinish(final Content playInfo, final int index, final int position) {
        if (Constant.CONTENTTYPE_CP.equals(playInfo.getContentType())) {
            if (TextUtils.isEmpty(playInfo.getCsContentIDs())) {
                addHistory(playInfo, index, position);
                return;
            }
            String csId = playInfo.getCsContentIDs().split("\\|")[0];
            mContentPresenter.getContent(csId, true, new
                    ContentContract.View() {
                        @Override
                        public void onContentResult(@NotNull String uuid, @Nullable Content content) {
                            if (content != null) {
                                playInfo.setVImage(content.getVImage());
                                playInfo.setTitle(content.getTitle());
                            }
                            addHistory(playInfo, index, position);
                        }

                        @Override
                        public void onSubContentResult(@NotNull String uuid, @Nullable ArrayList<SubContent> result) {

                        }

                        @Override
                        public void tip(@NotNull Context context, @NotNull String message) {

                        }

                        @Override
                        public void onError(@NotNull Context context, @Nullable String desc) {
                            addHistory(playInfo, index, position);
                        }
                    });
        } else {
            addHistory(playInfo, index, position);
        }
    }

    private void addHistory(final Content playInfo, final int index, final int position) {
        try {
            QueryUserStatusUtil.getInstance().getLoginStatus(this, new INotifyLoginStatusCallback() {
                @Override
                public void notifyLoginStatusCallback(boolean login) {
                    String userId;
                    String tableName;
                    if (login) {
                        userId = SharePreferenceUtils.getUserId(AppContext);
                        tableName = DBConfig.REMOTE_HISTORY_TABLE_NAME;
                    } else {
                        userId = SystemUtils.getDeviceMac(getApplicationContext());
                        tableName = DBConfig.HISTORY_TABLE_NAME;
                    }

                    Bundle bundle = new Bundle();
                    bundle.putString(DBConfig.PLAYINDEX, String.valueOf(index));
                    bundle.putString(DBConfig.PLAYPOSITION, String.valueOf(position));

                    DBUtil.addHistory(userId, playInfo, bundle, new DBCallback<String>() {
                        @Override
                        public void onResult(int code, String result) {
                            if (code == 0) {
                                LogUtils.e("写入历史记录成功");
                            }
                        }
                    }, tableName);


                }
            });

            History mHistory = new History(playInfo.getContentID(), playInfo.getContentType(),
                    playInfo.getTitle(), playInfo.getVImage(), "com.newtv.cboxtv",
                    "tv.newtv.cboxtv.SplashActivity", "", "", System.currentTimeMillis());
            Intent mIntent = new Intent();
            mIntent.setAction("android.intent.action.CONTENT_HISTORY");
            mIntent.putExtra("history", mHistory);
            sendBroadcast(mIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onExitApp() {

    }

    @Override
    public Activity getCurrentActivity() {
        return ActivityStacks.get().getCurrentActivity();
    }

    @Override
    public Intent getPlayerActivityIntent() {
        return new Intent(LauncherApplication.this, NewTVLauncherPlayerActivity.class);
    }

    @Override
    public boolean isVip() {
        return false;
    }

}
