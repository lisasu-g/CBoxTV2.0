package tv.newtv.cboxtv;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.newtv.cms.CmsErrorCode;
import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.SubContent;
import com.newtv.cms.contract.ContentContract;
import com.newtv.libs.Constant;
import com.newtv.libs.MainLooper;
import com.newtv.libs.util.NetworkManager;
import com.newtv.libs.util.ToastUtil;
import com.newtv.libs.util.YSLogUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import tv.newtv.cboxtv.annotation.BuyGoodsAD;
import tv.newtv.cboxtv.player.LifeCallback;
import tv.newtv.cboxtv.player.LiveListener;
import tv.newtv.cboxtv.player.PlayerUrlConfig;
import tv.newtv.cboxtv.player.model.LiveInfo;
import tv.newtv.cboxtv.player.videoview.VideoPlayerView;
import tv.newtv.cboxtv.player.view.NewTVLauncherPlayerView;
import tv.newtv.cboxtv.player.view.NewTVLauncherPlayerViewManager;
import tv.newtv.player.R;

/**
 * Created by wangkun on 2018/1/15.
 */
@BuyGoodsAD
public class NewTVLauncherPlayerActivity extends BaseActivity implements ContentContract
        .LoadingView, LiveListener, LifeCallback {

    private static final int PLAY_TYPE_LIVE = 0;
    private static final int PLAY_TYPE_VOD = 1;
    private static final int PLAY_TYPE_ALTERNATE = 2;
    private static final int PLAY_TYPE_SINGLE = 3;
    private static final int PLAY_TYPE_UNKNOWN = -1;
    private static String TAG = "NewTVLauncherPlayerActivity";
    NewTVLauncherPlayerView.PlayerViewConfig defaultConfig;
    int playPostion = 0;
    Content mProgramSeriesInfo;
    private FrameLayout mPlayerFrameLayoutContainer;
    private String contentUUID;
    private String contentType;
    private int PLAY_TYPE = PLAY_TYPE_UNKNOWN;


    private View loadingView;
    private int mIndexPlay;
    private ContentContract.ContentPresenter mPresenter;

    public static void play(Context context, Bundle bundle) {
        Intent intent = new Intent(context, NewTVLauncherPlayerActivity.class);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public boolean isFullScreenActivity() {
        return true;
    }

    @Override
    public boolean hasPlayer() {
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();

        if (mPresenter != null) {
            mPresenter.destroy();
            mPresenter = null;
        }

        if (PlayerUrlConfig.getInstance().isFromDetailPage())
        {//如果是从小屏切到大屏的，关闭播放器时恢复DetailPage值false
            PlayerUrlConfig.getInstance().setFromDetailPage(false);
        } else {//由推荐位直接到大屏播放器的，关闭播放器时清空保存的值
            PlayerUrlConfig.getInstance().setPlayingContentId("");
            PlayerUrlConfig.getInstance().setPlayUrl("");
            YSLogUtils.getInstance(NewTVLauncherPlayerActivity.this).clearData();
        }

        mPlayerFrameLayoutContainer = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newtv_launcher_vod_activity);
        loadingView = findViewById(R.id.loading);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!NetworkManager.getInstance().isConnected()) {
            Toast.makeText(getApplicationContext(), R.string.net_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mPresenter = new ContentContract.ContentPresenter(getApplicationContext(), this);
        mPlayerFrameLayoutContainer = (FrameLayout) findViewById(R.id.player_view_container);

        Bundle extras;
        if (savedInstanceState == null) {
            extras = getIntent().getExtras();
        } else {
            extras = savedInstanceState;
        }
        if (extras != null) {
            contentUUID = (String) extras.getString(Constant.CONTENT_UUID);
            contentType = (String) extras.getString(Constant.CONTENT_TYPE);
            if (TextUtils.isEmpty(contentUUID)) {
                ToastUtil.showToast(getApplicationContext(), "节目ID为空");
                finish();
                return;
            }

            if (Constant.CONTENTTYPE_LB.equals(contentType)) {
                PLAY_TYPE = PLAY_TYPE_ALTERNATE;
            } else if (Constant.CONTENTTYPE_LV.equals(contentType)) {
                PLAY_TYPE = PLAY_TYPE_LIVE;
            } else if(Constant.CONTENTTYPE_PG.equals(contentType) || Constant.CONTENTTYPE_CP.equals(contentType)){
                PLAY_TYPE = PLAY_TYPE_SINGLE;
            } else {
                PLAY_TYPE = PLAY_TYPE_VOD;
            }

            if (PLAY_TYPE == PLAY_TYPE_VOD) {
                mPresenter.getContent(contentUUID, true, contentType);
            } else {
                mPresenter.getContent(contentUUID, false, contentType);
            }
        }
    }

    @Override
    public void prepareMediaPlayer() {
        if (mPlayerFrameLayoutContainer != null) {
            if(defaultConfig == null) {
                NewTVLauncherPlayerView newTVLauncherPlayerView = new NewTVLauncherPlayerView(this);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout
                        .LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                newTVLauncherPlayerView.setLifeCallback(this);
                newTVLauncherPlayerView.setLayoutParams(layoutParams);
                newTVLauncherPlayerView.setFromFullScreen();
                mPlayerFrameLayoutContainer.addView(newTVLauncherPlayerView, layoutParams);
            }else{
                new NewTVLauncherPlayerView(defaultConfig,this);
            }
            NewTVLauncherPlayerViewManager.getInstance().setPlayerViewContainer
                    (mPlayerFrameLayoutContainer, this, true);
        }
    }

    private void initListener() {
//        menuGroupPresenter.addSelectListener(new MenuGroup.OnSelectListener() {
//            @Override
//            public void select(Program program) {
//                // 什么时候会修改Constant.isLiving的值？
//                // 5. 小屏时 强制点播下一个文件时 将isLiving置为false
//                Constant.isLiving = false;
//            }
//        });
//
//        NewTVLauncherPlayerViewManager.getInstance().addListener(new IPlayProgramsCallBackEvent
// () {
//
//            @Override
//            public void onNext(SubContent info, int index, boolean isNext) {
//                mIndexPlay = index;
//                mPositionPlay = 0;
//                RxBus.get().post(Constant.UPDATE_VIDEO_PLAY_INFO, new VideoPlayInfo(mIndexPlay,
//                        mPositionPlay, NewTVLauncherPlayerViewManager.getInstance()
//                        .getProgramSeriesInfo().getContentID()));
//            }
//        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.e(TAG, "action:" + event.getAction() + ",keyCode=" + event.getKeyCode());
        if (NewTVLauncherPlayerViewManager.getInstance().dispatchKeyEvent(event)) {
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    releasePlayer();
                    finish();
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyDown: ");
        NewTVLauncherPlayerViewManager.getInstance().onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed");
        super.onBackPressed();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyUp: " + keyCode);
        NewTVLauncherPlayerViewManager.getInstance().onKeyUp(keyCode, event);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (mProgramSeriesInfo != null) {
            doPlay(mProgramSeriesInfo);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");



        releasePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    private void releasePlayer() {
        playPostion = NewTVLauncherPlayerViewManager.getInstance().getPlayPostion();
        mIndexPlay = NewTVLauncherPlayerViewManager.getInstance().getIndex();
        defaultConfig = NewTVLauncherPlayerViewManager.getInstance().getDefaultConfig();
        NewTVLauncherPlayerViewManager.getInstance().releasePlayer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState01: ");

        outState.putString(Constant.CONTENT_UUID, contentUUID);
        outState.putString(Constant.CONTENT_TYPE, contentType);
    }

    @Override
    public void onContentResult(@NotNull String uuid, @Nullable Content content) {
        mProgramSeriesInfo = content;
        if (content == null || (PLAY_TYPE == PLAY_TYPE_VOD &&
                (content.getData() == null || content.getData().size() == 0))) {
            Toast.makeText(this, "节目内容为空", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        MainLooper.get().postDelayed(new Runnable() {
            @Override
            public void run() {
                doPlay(content);
            }
        }, 1000);
    }

    private void doPlay(Content content) {
        initListener();
        if (!isFinishing()) {
            if (PLAY_TYPE == PLAY_TYPE_VOD || PLAY_TYPE == PLAY_TYPE_SINGLE) {
                NewTVLauncherPlayerViewManager.getInstance().playVod(this, content, mIndexPlay,
                        playPostion);
                mIndexPlay = NewTVLauncherPlayerViewManager.getInstance().getIndex();
            } else if (PLAY_TYPE == PLAY_TYPE_ALTERNATE) {
                NewTVLauncherPlayerViewManager.getInstance().changeAlternate(content.getContentID(),
                        content.getAlternateNumber(), content.getTitle());
            } else if (PLAY_TYPE == PLAY_TYPE_LIVE) {
                LiveInfo liveInfo = new LiveInfo();
                liveInfo.setLiveUrl(content.getPlayUrl());
                liveInfo.setContentUUID(content.getContentUUID());
                liveInfo.setAlwaysPlay(true);
                liveInfo.setmTitle(content.getTitle());
                NewTVLauncherPlayerViewManager.getInstance().playLive(liveInfo, this);
            } else {
                Toast.makeText(this, "不支持的节目类型", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (loadingView != null)
                loadingView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onSubContentResult(@NotNull String uuid, @Nullable ArrayList<SubContent> result) {

    }

    @Override
    public void tip(@NotNull Context context, @NotNull String message) {

    }

    @Override
    public void onError(@NotNull Context context, @NotNull String code, @Nullable String desc) {
        onLifeError(code, desc);
    }

    @Override
    public void onLoading() {

    }

    @Override
    public void loadComplete() {

    }

    @Override
    public void onTimeChange(String current, String end) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onPlayerRelease() {

    }

    @Override
    public void onLifeError(String code, String message) {
//        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        if (CmsErrorCode.ALTERNATE_ERROR_PLAYLIST_EMPTY.equals(code) || CmsErrorCode
                .CMS_NO_ONLINE_CONTENT.equals(code)) {
            if (fromOuter) {
                ToastUtil.showToast(getApplicationContext(), "节目走丢了，即将进入应用首页");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("action", "");
                intent.putExtra("params", "");
                startActivity(intent);
            } else {
                ToastUtil.showToast(getApplicationContext(), "节目走丢了，即将返回");
            }
            finish();
        }
    }
}
