package com.newtv.libs.ad;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.util.RxBus;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import tv.icntv.adsdk.AdSDK;

/**
 * Created by Administrator on 2018/4/28.
 */

public class ADPresenter implements IAdConstract.IADPresenter, ADConfig.ColumnListener {
    private static final String TAG = "ADPresenter";

    private IAdConstract.IADConstractView adConstractView;
    private ADHelper.AD mAD;
    private boolean columnIsGet = false;
    private String adType;
    private String adLoc;
    private String flag;


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            JsonObject jsonObject = (JsonObject) msg.obj;
            excute(jsonObject.get("firstChannel").getAsString(),
                    jsonObject.get("secondChannel").getAsString(),
                    jsonObject.get("topicId").getAsString());
        }
    };

    public ADPresenter(IAdConstract.IADConstractView adConstractView) {
        this.adConstractView = adConstractView;
        ADConfig.getInstance().setListener(this);
    }

    public void cancel() {
        if (mAD != null) {
            mAD.cancel();
        }
    }

    @Override
    public void destroy() {
        cancel();
        mAD = null;
        adConstractView = null;
        ADConfig.getInstance().setColumnId("");
        ADConfig.getInstance().setSecondColumnId("");
        ADConfig.getInstance().removeListener(this);
    }

    @SuppressLint("CheckResult")
    @Override
    public void getAD(final String adType, final String adLoc, final String flag, final String
            firstChannel, final String secondChannel, final String topicId) {
        this.adType = adType;
        this.adLoc = adLoc;
        this.flag = flag;

        if (columnIsGet || !TextUtils.isEmpty(ADConfig.getInstance().getColumnId())) {
            excute(firstChannel, secondChannel, topicId);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    excute(firstChannel, secondChannel, topicId);
                }
            },5000);
        }
    }

    @SuppressLint("CheckResult")
    private void excute(final String firstChannel, final String secondChannel, final String
            topicId) {
        Log.i(TAG, "getAD: " + ADConfig.getInstance().toString());
        RxBus.get().post(Constant.INIT_SDK, Constant.INIT_ADSDK);

        final StringBuffer sb = new StringBuffer();

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                ADConfig config = ADConfig.getInstance();
                StringBuilder stringBuilder = new StringBuilder();
                addExtend(stringBuilder, "panel", firstChannel);
                addExtend(stringBuilder, "secondpanel", secondChannel);
                addExtend(stringBuilder, "topic", topicId);
                addExtend(stringBuilder, "secondcolumn", config.getSecondColumnId());
                e.onNext(AdSDK.getInstance().getAD(adType, config.getColumnId(), config
                        .getSeriesID(), adLoc, null, stringBuilder.toString(), sb));
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) throws Exception {
                        Log.i(TAG, "accept: " + sb.toString());
                        mAD = ADHelper.getInstance().parseADString
                                (Libs.get().getContext(), sb.toString());

                        if (adConstractView == null) {
                            return;
                        }
                        if (mAD == null) {
                            ADHelper.AD.ADItem adItem = new ADHelper.AD.ADItem(adType);
                            adConstractView.showAd(adItem);
                            return;
                        }
                        Log.e("AdHelper", "显示:" + mAD.toString());
                        mAD.setCallback(new ADHelper.ADCallback() {
                            @Override
                            public void showAd(String type, String url) {
//                                if(adConstractView == null) return;
//                                adConstractView.showAd(url, type);
                            }

                            @Override
                            public void showAdItem(ADHelper.AD.ADItem adItem) {
                                if (adConstractView != null) {
                                    adConstractView.showAd(adItem);
                                }
                            }

                            @Override
                            public void updateTime(int total, int left) {

                            }

                            @Override
                            public void complete() {
                                if (Constant.AD_DETAILPAGE_BANNER.equals(flag)) {
                                    if (mAD != null) {
                                        mAD.checkStart(false);
                                    }
                                }
                            }
                        }).start();

                    }
                });
    }

    private void addExtend(StringBuilder result, String key, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        if (TextUtils.isEmpty(result)) {
            result.append(key).append("=").append(value);
        } else {
            result.append("&").append(key).append("=").append(value);
        }
    }

    @Override
    public void receive() {
        columnIsGet = true;
    }
}