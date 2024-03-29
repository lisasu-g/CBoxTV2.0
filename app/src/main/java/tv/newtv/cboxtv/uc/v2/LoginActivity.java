package tv.newtv.cboxtv.uc.v2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.uc.pay.ExterPayBean;
import com.newtv.libs.util.LogUploadUtils;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.QrcodeUtil;
import com.newtv.libs.util.ScaleUtils;
import com.newtv.libs.util.SharePreferenceUtils;
import com.newtv.libs.util.Utils;

import org.json.JSONObject;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import tv.newtv.cboxtv.ActivityStacks;
import tv.newtv.cboxtv.BaseActivity;
import tv.newtv.cboxtv.MainActivity;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.net.NetClient;
import tv.newtv.cboxtv.uc.v2.Pay.PayChannelActivity;
import tv.newtv.cboxtv.uc.v2.Pay.PayOrderActivity;
import tv.newtv.cboxtv.uc.v2.manager.UserCenterRecordManager;
import tv.newtv.cboxtv.uc.v2.member.MemberCenterActivity;
import tv.newtv.cboxtv.utils.UserCenterUtils;


/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv
 * 创建事件:         17:03
 * 创建人:           weihaichao
 * 创建日期:          2018/8/24
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener, View
        .OnFocusChangeListener {

    private final String TAG = "LoginActivity";
    private ImageView img_login;
    private Button mButton;
    private FrameLayout frameLayout_qrcode;
    private QrcodeUtil mQrcodeUtil;
    private String mDeviceCode;
    private String Authorization;
    private final int MSG_RESULT_TOKEN = 1;
    private final int MSG_RESULT_INVALID = 2;
    private final int MSG_AUTH = 3;
    private Disposable disposable_Qrcode, disposable_token, disposable_buyFlag;
    private PopupWindow mPopupWindow;
    private View mPopupView;
    private TextView tv_title_full;
    private ImageView img_qrcode_full;
    private Bitmap mBitmap;
    private String mQRcode;
    private int expires;
    private boolean mFlagPay;
    private boolean mFlagAuth;
    private ExterPayBean mExterPayBean;
    private String mVipFlag;
    private int location = -1;
    private String mContentUUID;
    private String mExternalAction;
    private String mExternalParams;
    private int loginType; // 登陆方式 0:newtv;1:cctv，此参数只在手机验证码这种登录方式的情况下会用到, M站登录不需要考虑这个变量
    private String page;   // member ,  orders  ,

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
        location = getIntent().getIntExtra("location", -1);  //登录页面跳转
        page = getIntent().getStringExtra("page");
        mFlagPay = getIntent().getBooleanExtra("ispay", false);
        mFlagAuth = getIntent().getBooleanExtra("isAuth", false);
        mExternalAction = getIntent().getStringExtra("action");
        mExternalParams = getIntent().getStringExtra("params");
        mExterPayBean = (ExterPayBean) getIntent().getSerializableExtra("payBean");
        if (mExterPayBean != null) {
            Log.i(TAG, "mExterPayBean: " + mExterPayBean);
            mVipFlag = mExterPayBean.getVipFlag();
            Log.i(TAG, mExterPayBean.toString());
            mContentUUID = mExterPayBean.getContentUUID();
        }
        Log.i(TAG, "mFlagPay: " + mFlagPay);
        Log.i(TAG, "mFlagAuth: " + mFlagAuth);
        if (TextUtils.isEmpty(Authorization)) {
            Authorization = Utils.getAuthorization(LoginActivity.this);
            Constant.Authorization = Authorization;
        }
        if (location == 1) {
            Log.i(TAG, "location: " + location);
            mButton.requestFocus();
        }

    }

    private void init() {
        mBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        img_login = findViewById(R.id.login_imageview);
        mButton = findViewById(R.id.mobile_login_btn);
        frameLayout_qrcode = findViewById(R.id.login_frame_qrcode_root);
        mButton.setOnClickListener(this);
        frameLayout_qrcode.setOnClickListener(this);
        mButton.setOnFocusChangeListener(this);
        frameLayout_qrcode.setOnFocusChangeListener(this);
        mQrcodeUtil = new QrcodeUtil();
        mPopupView = LayoutInflater.from(this).inflate(R.layout.layout_usercenter_qr_code_full, null);
        img_qrcode_full = mPopupView.findViewById(R.id.layout_usercenter_img_qrcode);
        tv_title_full = mPopupView.findViewById(R.id.usercenter_full_screen_qr_top_title);
        tv_title_full.setText(getResources().getString(R.string.usercenter_login_scan_phone));
        mPopupWindow = new PopupWindow(mPopupView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());// 响应返回键必须的语句。
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (TextUtils.isEmpty(Authorization)) {
            Authorization = Utils.getAuthorization(LoginActivity.this);
            Constant.Authorization = Authorization;
        }

        getQrcode(Authorization, Constant.RESPONSE_TYPE, Constant.CLIENT_ID);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mobile_login_btn:
                Intent intent = new Intent(LoginActivity.this, PhoneLoginActivity.class);
                intent.putExtra("ispay", mFlagPay);
                intent.putExtra("isAuth", mFlagAuth);
                intent.putExtra("payBean", mExterPayBean);
                intent.putExtra("action", mExternalAction);
                intent.putExtra("params", mExternalParams);
                intent.putExtra("loginType", loginType);
                intent.putExtra("page", page);
                Log.e(TAG, "onClick: mobile_login_btn------loginType=" + loginType);
                startActivity(intent);
                finish();
                break;
            case R.id.login_frame_qrcode_root:
                mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {

        if (hasFocus) {
            onItemGetFocus(v);
        } else {
            onItemLoseFocus(v);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.removeMessages(MSG_RESULT_TOKEN);
        }
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESULT_TOKEN:
                    getToken(Authorization,
                            Constant.RESPONSE_TYPE,
                            mDeviceCode,
                            Constant.CLIENT_ID);
                    if (mHandler != null) {
                        mHandler.sendEmptyMessageDelayed(MSG_RESULT_TOKEN, 2000);
                    }
                    break;
                case MSG_RESULT_INVALID:
                    getQrcode(Authorization, Constant.RESPONSE_TYPE, Constant.CLIENT_ID);
                    break;
            }
            return false;
        }
    });


    private void getQrcode(String Authorization, String response_type, String client_id) {
        try {
            NetClient.INSTANCE.getUserCenterLoginApi()
                    .getLoginQRCode(Authorization, response_type, client_id, Libs.get().getChannelId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {

                        @Override
                        public void onSubscribe(Disposable d) {
                            disposable_Qrcode = d;
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String data = responseBody.string();
                                Log.i(TAG, "Login Qrcode :" + data.toString());
                                JSONObject mJsonObject = new JSONObject(data);
                                mDeviceCode = mJsonObject.optString("device_code");
                                mQRcode = mJsonObject.optString("veriﬁcation_uri_complete");
                                expires = mJsonObject.optInt("expires_in");
                                loginType = mJsonObject.optInt("login_type");
                                mQrcodeUtil.createQRImage(mQRcode, getResources().getDimensionPixelOffset(R.dimen.width_448px),
                                        getResources().getDimensionPixelOffset(R.dimen.height_448px), mBitmap, img_login);

                                mQrcodeUtil.createQRImage(mQRcode, getResources().getDimensionPixelOffset(R.dimen.height_607px),
                                        getResources().getDimensionPixelOffset(R.dimen.height_607px), mBitmap, img_qrcode_full);
                                if (mHandler != null) {
                                    mHandler.sendEmptyMessageDelayed(MSG_RESULT_INVALID, expires * 1000);
                                    mHandler.sendEmptyMessage(MSG_RESULT_TOKEN);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.i(TAG, "GetToken  onError" + e);
                            if (disposable_Qrcode != null) {
                                disposable_Qrcode.dispose();
                                disposable_Qrcode = null;
                            }
                            String error = getResources().getString(R.string.phone_login_err);
                            if (e instanceof HttpException) {
                                HttpException httpException = (HttpException) e;
                                try {
                                    String responseString = httpException.response().errorBody().string();
                                    JSONObject jsonObject = new JSONObject(responseString);
                                    error = jsonObject.optString("msg");
                                    Log.i(TAG, "error: " + responseString);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onComplete() {
                            if (disposable_Qrcode != null) {
                                disposable_Qrcode.dispose();
                                disposable_Qrcode = null;
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getToken(String Authorization, String response_type, String mDeviceCode, String client_id) {
        try {
            NetClient.INSTANCE.getUserCenterLoginApi()
                    .getAccessToken(Authorization, response_type, mDeviceCode, client_id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {

                        @Override
                        public void onSubscribe(Disposable d) {
                            disposable_token = d;
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            try {
                                String data = responseBody.string();
                                Log.i(TAG, "Login responseBody :" + data);
                                JSONObject mJsonObject = new JSONObject(data);
                                String mAccessToken = mJsonObject.optString("access_token");
                                String RefreshToken = mJsonObject.optString("refresh_token");
                                Log.i(TAG, "Login access_token :" + mAccessToken);
                                Log.i(TAG, "Login RefreshToken :" + RefreshToken);
                                SharePreferenceUtils.saveToken(LoginActivity.this, mAccessToken, RefreshToken);

                                UserCenterRecordManager.getInstance().synchronizationUserBehavior(getApplicationContext());

                                if (mHandler != null) {
                                    mHandler.removeMessages(MSG_RESULT_TOKEN);
                                }
                                //微信登录上报日志
                                LogUploadUtils.uploadLog(Constant.LOG_NODE_USER_CENTER, "8,0,1");
                                // LogUploadUtils.uploadKey(Constant.USER_ID, SharePreferenceUtils.getUserId(LoginActivity.this));
                                LogUploadUtils.setLogFileds(Constant.USER_ID, SharePreferenceUtils.getUserId(LoginActivity.this));
                                UserCenterUtils.setLogin(true);
                                if (mFlagAuth) {
                                    isBuy("", mContentUUID);
                                } else {
                                    if (mFlagPay) {
                                        if (mVipFlag != null) {
                                            Intent mIntent = new Intent();
                                            if (mVipFlag.equals(Constant.BUY_ONLY)) {
                                                mIntent.setClass(LoginActivity.this, PayOrderActivity.class);
                                            } else {
                                                mIntent.setClass(LoginActivity.this, PayChannelActivity.class);
                                            }
                                            mIntent.putExtra("payBean", mExterPayBean);
                                            startActivity(mIntent);
                                        }
                                    }

                                    if (!TextUtils.isEmpty(mExternalAction) && !TextUtils.isEmpty(mExternalParams)) {
                                        jumpActivity();
                                    }
                                    if (!TextUtils.isEmpty(page)) {
                                        Intent intent = new Intent();
                                        if (TextUtils.equals("member", page)) {
                                            intent.setClass(LoginActivity.this, MemberCenterActivity.class);
                                        } else if (TextUtils.equals("orders", page)) {
                                            intent.setClass(LoginActivity.this, MyOrderActivity.class);
                                        }
                                        startActivity(intent);
                                    }
                                    finish();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onError(Throwable e) {
                            if (disposable_token != null) {
                                disposable_token.dispose();
                                disposable_token = null;
                            }
                        }

                        @Override
                        public void onComplete() {
                            if (disposable_token != null) {
                                disposable_token.dispose();
                                disposable_token = null;
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jumpActivity() {
        Class clazz = MainActivity.class;
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("action", mExternalAction);
        intent.putExtra("params", mExternalParams);
        startActivity(intent);
        boolean isBackground = ActivityStacks.get().isBackGround();
        if (!isBackground && clazz == MainActivity.class) {
            ActivityStacks.get().finishAllActivity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        if (disposable_Qrcode != null) {
            disposable_Qrcode.dispose();
            disposable_Qrcode = null;
        }
        if (disposable_token != null) {
            disposable_token.dispose();
            disposable_token = null;
        }
    }

    private void onItemGetFocus(View view) {
        //直接放大view
//        ScaleAnimation sa = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//        sa.setFillAfter(true);
//        sa.setDuration(150);
//        view.startAnimation(sa);
        ScaleUtils.getInstance().onItemGetFocus(view,400,1.1f);
    }

    private void onItemLoseFocus(View view) {
        // 直接缩小view
//        ScaleAnimation sa = new ScaleAnimation(1.1f, 1.0f, 1.1f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//        sa.setFillAfter(true);
//        sa.setDuration(150);
//        view.startAnimation(sa);
        ScaleUtils.getInstance().onItemLoseFocus(view,400,1.1f);
    }

    public void isBuy(String productIds, String contentUUID) {
        String token = SharePreferenceUtils.getToken(LoginActivity.this);

        NetClient.INSTANCE.getUserCenterLoginApi()
                .getBuyFlag("Bearer " + token, productIds, Libs.get().getAppKey(),
                        Libs.get().getChannelId(), contentUUID, "3.1")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable_buyFlag = d;
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String result = responseBody.string();
                            LogUtils.i(TAG, result);
                            JSONObject jsonObject = new JSONObject(result);
                            boolean buyFlag = jsonObject.optBoolean("buyFlag");
                            Log.i(TAG, "buyFlag :" + buyFlag);

                            if (!buyFlag) {
                                if (mFlagPay) {
                                    if (mVipFlag != null) {
                                        Intent mIntent = new Intent();
                                        if (mVipFlag.equals(Constant.BUY_ONLY)) {
                                            mIntent.setClass(LoginActivity.this, PayOrderActivity.class);
                                        } else {
                                            mIntent.setClass(LoginActivity.this, PayChannelActivity.class);
                                        }
                                        mIntent.putExtra("payBean", mExterPayBean);
                                        startActivity(mIntent);
                                    }
                                }
                            }
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (disposable_buyFlag != null) {
                            disposable_buyFlag.dispose();
                            disposable_buyFlag = null;
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (disposable_buyFlag != null) {
                            disposable_buyFlag.dispose();
                            disposable_buyFlag = null;
                        }
                    }
                });

    }
}
