package tv.newtv.cboxtv.player.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.SubContent;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.util.DeviceUtil;
import com.newtv.libs.util.KeyEventUtils;
import com.newtv.libs.util.LiveTimingUtil;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.RxBus;
import com.newtv.libs.util.ScreenUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tv.newtv.cboxtv.menu.IMenuGroupPresenter;
import tv.newtv.cboxtv.menu.MenuGroupPresenter;
import tv.newtv.cboxtv.menu.MenuPopupWindow;
import tv.newtv.cboxtv.player.ActivityStacks;
import tv.newtv.cboxtv.player.ChkPlayResult;
import tv.newtv.cboxtv.player.FocusWidget;
import tv.newtv.cboxtv.player.IFocusWidget;
import tv.newtv.cboxtv.player.IPlayProgramsCallBackEvent;
import tv.newtv.cboxtv.player.NewTVLauncherPlayer;
import tv.newtv.cboxtv.player.Player;
import tv.newtv.cboxtv.player.PlayerConfig;
import tv.newtv.cboxtv.player.PlayerConstants;
import tv.newtv.cboxtv.player.contract.LiveContract;
import tv.newtv.cboxtv.player.contract.VodContract;
import tv.newtv.cboxtv.player.iPlayCallBackEvent;
import tv.newtv.cboxtv.player.model.LiveInfo;
import tv.newtv.cboxtv.player.model.VideoDataStruct;
import tv.newtv.cboxtv.player.model.VideoPlayInfo;
import tv.newtv.cboxtv.player.videoview.PlayerCallback;
import tv.newtv.cboxtv.player.videoview.VPlayCenter;
import tv.newtv.player.R;

//import tv.newtv.cboxtv.cms.details.PushManager;

/**
 * Created by wangkun on 2018/1/16.
 */

public class NewTVLauncherPlayerView extends FrameLayout implements LiveContract.View, VodContract
        .View {

    public static final int SHOWING_NO_VIEW = 0;
    public static final int SHOWING_SEEKBAR_VIEW = 2;
    public static final int SHOWING_NUMBER_PROGRAM_SELECTOR = 3;
    public static final int SHOWING_NAME_PROGRAM_SELECTOR = 4;
    public static final int SHOWING_SETTING_VIEW = 5;
    public static final int SHOWING_EXIT_VIEW = 6;
    public static final int SHOWING_PROGRAM_TREE = 7;
    private static final String TAG = NewTVLauncherPlayerView.class.getName();


    private static final int PROGRAM_SELECTOR_TYPE_NONE = 0; //不显示选集
    private static final int PROGRAM_SELECTOR_TYPE_NUMBER = 1; //显示数字选集
    private static final int PROGRAM_SELECTOR_TYPE_NAME = 2; //显示名称选集


    private static final int PLAY_TYPE_SINGLE = 0;
    private static final int PLAY_TYPE_SERIES = 1;
    private static final int PLAY_TYPE_LIVE = 2;


    private static int defaultWidth;
    private static int defaultHeight;
    private static boolean enterFullScreen = false;
    protected PlayerViewConfig defaultConfig;
    protected boolean startIsFullScreen = true;
    protected boolean ProgramIsChange = false;
    private long PLAYER_ID = 0;
    private VideoFrameLayout mPlayerFrameLayout;
    private NewTVLauncherPlayerLoading mLoading;
    private NewTVLauncherPlayerSeekbar mNewTVLauncherPlayerSeekbar;
    private int mShowingChildView = SHOWING_NO_VIEW;
    private boolean mIsLoading; //是否在缓冲
    private boolean mIsPause; //是否在暂停
    private boolean mIsPrepared;
    //    private ImageView mPauseImageView;
    private boolean mIsNeedPause;
    private Content mProgramSeriesInfo; //当前播放的节目集信息
    private ChkPlayResult mProgramDetailInfo; //当前播放的节目信息
    private int mPlayingIndex; //当前播放第几集
    private String mContentUUid; //当前播放第几集
    private int mPlayType;
    private int mHistoryPostion;
    private String mPlayUrl; //进入播放指定清晰度的url
    private boolean isLiving = false;
    private LiveInfo mLiveInfo;
    private boolean isReleased = false;
    private MenuPopupWindow menuPopupWindow;
    private IMenuGroupPresenter menuGroupPresenter;
    private NewTVLauncherPlayer mNewTVLauncherPlayer;
    private List<IPlayProgramsCallBackEvent> listener = new ArrayList<>();
    private boolean NeedJumpAd = false;
    private boolean unshowLoadBack = false;
    private Map<Integer, FocusWidget> widgetMap;

    private LiveContract.Presenter mLivePresenter;
    private VodContract.Presenter mVodPresenter;


    private iPlayCallBackEvent mCallBackEvent = new iPlayCallBackEvent() {
        @Override
        public void onPrepared(LinkedHashMap<String, String> definitionDatas) {
            LogUtils.i(TAG, "onPrepared: ");
            mIsPrepared = true;
            stopLoading();
            mNewTVLauncherPlayerSeekbar.setDuration();
            if (mHistoryPostion > 0 && mHistoryPostion < mNewTVLauncherPlayer.getDuration() - 30
                    * 1000) {
                mNewTVLauncherPlayer.seekTo(mHistoryPostion);
            }
            mHistoryPostion = 0;
        }

        @Override
        public void onCompletion() {
            LogUtils.i(TAG, "onCompletion: ");
            /*
             *  大屏点播完成后，
             *  判断是否符合直播条件，如果符合则直播。 不符合则播放下一级
             */
            // 什么时候会修改Constant.isLiving的值？
            // 3. 大屏加载完一个点播文件，播放下一个之前，需要判断当前时间是否满足直播
            Constant.isLiving = false;

            playVodNext();
        }

        @Override
        public void onVideoBufferStart(String typeString) {
            LogUtils.i(TAG, "onVideoBufferStart: typeString=" + typeString);
            startLoading();
        }

        @Override
        public void onVideoBufferEnd(String typeString) {
            Log.i(TAG, "onVideoBufferEnd: typeString=" + typeString);
            if ("702".equals(typeString)) {
                unshowLoadBack = true;
            }
            if (!TextUtils.isEmpty(typeString) && (typeString.equals("702") || "ad_onPrepared"
                    .equals(typeString))) {
                stopLoading();
                hidePauseImage();
            }
        }

        @Override
        public void onTimeout() {
            LogUtils.i(TAG, "onTimeout: ");
        }

        @Override
        public void changePlayWithDelay(int delay, String url) {

        }

        @Override
        public void onError(int what, int extra, String msg) {
            LogUtils.i(TAG, "onError: ");
        }
    };


    private iPlayCallBackEvent mLiveCallBackEvent = new iPlayCallBackEvent() {
        @Override
        public void onPrepared(LinkedHashMap<String, String> definitionDatas) {
            LogUtils.i(TAG, "live onPrepared: ");
            mIsPrepared = true;
            stopLoading();

            if (mShowingChildView == SHOWING_PROGRAM_TREE) {
                menuGroupPresenter.gone();
            }

            if (!(isLiving() && mLiveInfo != null && !mLiveInfo.isTimeShift())) {
                mNewTVLauncherPlayerSeekbar.setDuration();
            }

            mHistoryPostion = 0;
        }

        @Override
        public void onCompletion() {
            LogUtils.i(TAG, "live onCompletion: ");
        }

        @Override
        public void onVideoBufferStart(String typeString) {

            LogUtils.i(TAG, "live onVideoBufferStart: typeString=" + typeString);
            startLoading();
        }

        @Override
        public void onVideoBufferEnd(String typeString) {
            Log.i(TAG, "live onVideoBufferEnd: typeString=" + typeString);
            if ("702".equals(typeString)) {
                unshowLoadBack = true;
            }
            if (!TextUtils.isEmpty(typeString) && (typeString.equals("702") ||
                    "ad_onPrepared".equals(typeString))) {
                stopLoading();
                hidePauseImage();
            }
        }

        @Override
        public void onTimeout() {
            LogUtils.i(TAG, "live onTimeout: ");
        }

        @Override
        public void changePlayWithDelay(int delay, String liveUrl) {

//            if (mProgramSeriesInfo != null) {
//                String playUrl = translateUrl(liveUrl, delay);
//                LogUtils.d(TAG, "changePlayWithDelay video delay=" + delay + " url=" + playUrl);
//                if (mLiveInfo.getmLiveUrl().equals(playUrl)) {
//                    return;
//                }
//                mNewTVLauncherPlayer.release();
//                mLiveInfo.setLiveUrl(playUrl);
//                PlayerConfig.getInstance().setScreenChange(true);
//                PlayerConfig.getInstance().setJumpAD(true);
//                playLive(playUrl, mProgramSeriesInfo, false, 0, 0);
//            }
        }

        @Override
        public void onError(int what, int extra, String msg) {
            LogUtils.i(TAG, "live onError: ");
        }
    };
    private PlayerLocation mPlayerLocation;

    public NewTVLauncherPlayerView(PlayerViewConfig config, @NonNull Context context) {
        this(context, null, 0, config);
    }

    public NewTVLauncherPlayerView(@NonNull Context context) {
        this(context, null);
    }


    public NewTVLauncherPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NewTVLauncherPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int
            defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public NewTVLauncherPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int
            defStyleAttr, PlayerViewConfig config) {
        super(context, attrs, defStyleAttr);
        updateDefaultConfig(config);
        initView(context);
    }

    @SuppressLint("UseSparseArrays")
    public int registerWidget(int id, IFocusWidget widget) {
        if (id != 0) {
            unregisterWidget(id);
        }
        FocusWidget focusWidget = new FocusWidget(widget);
        if (widgetMap == null) {
            widgetMap = new HashMap<>();
        }
        widgetMap.put(focusWidget.getId(), focusWidget);
        return focusWidget.getId();
    }

    /**
     * 接触外部控件注册
     *
     * @param id
     */
    public void unregisterWidget(int id) {
        if (widgetMap != null && widgetMap.containsKey(id)) {
            FocusWidget focusWidget = widgetMap.get(id);
            if (focusWidget != null && focusWidget.isShowing()) {
                focusWidget.onBackPressed();
            }
            widgetMap.remove(id);
        }
    }

    public void updateDefaultConfig(PlayerViewConfig config) {
        defaultConfig = config;
        if (config != null) {
            startIsFullScreen = config.startIsFullScreen;
            setLayoutParams(config.layoutParams);
            ((ViewGroup) config.parentViewGroup).addView(this, config.layoutParams);
        }
    }

    public void buildPlayerViewConfig() {
        if (defaultConfig == null) {
            defaultConfig = new PlayerViewConfig();
        }

        defaultConfig.layoutParams = getLayoutParams();
        defaultConfig.prepared = mIsPrepared;
        defaultConfig.playPosition = getCurrentPosition();
        defaultConfig.isFullScreen = isFullScreen();
        defaultConfig.startIsFullScreen = startIsFullScreen;
        defaultConfig.parentViewGroup = getParent();
    }

    public PlayerViewConfig getDefaultConfig() {
        if (defaultConfig == null) {
            buildPlayerViewConfig();
        }
        return defaultConfig;
    }

    protected void onError(String code, String messgae) {
        stopLoading();
    }

    protected boolean NeedRepeat() {
        return false;
    }

    public void ExitFullScreen() {
        if (!enterFullScreen) return;
        enterFullScreen = false;

        if (mPlayerLocation != null) {
            mPlayerLocation.destroy();
            mPlayerLocation = null;
        }

        Activity activity = ActivityStacks.get().getCurrentActivity();
        dismissChildView();

        final int screenWidth = activity.getWindow().getDecorView().getMeasuredWidth();
        final int screenHeight = activity.getWindow().getDecorView().getMeasuredHeight();

        ViewGroup.LayoutParams container = getLayoutParams();
        container.width = defaultWidth;
        container.height = defaultHeight;
        setLayoutParams(container);
        FrameLayout frameLayout = activity.getWindow().getDecorView().findViewById(android.R.id
                .content);
        View rootView = frameLayout.getChildAt(0);

        setParentWidth(this, rootView, screenWidth - defaultWidth, screenHeight - defaultHeight,
                screenWidth, screenHeight, false, true);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) frameLayout
                .getLayoutParams();
        layoutParams.leftMargin = 0;
        layoutParams.topMargin = 0;
        frameLayout.setLayoutParams(layoutParams);

        updateUIPropertys(false);
        if (menuGroupPresenter != null && menuGroupPresenter instanceof MenuGroupPresenter) {
            ((MenuGroupPresenter) menuGroupPresenter).exitFullScreen();
        }

        NeedJumpAd = ProgramIsChange;

        if (mIsPause && mNewTVLauncherPlayer != null) {
            start();
        }
    }

    public void setFromFullScreen() {
        startIsFullScreen = true;
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(layoutParams);
        }
    }

    public boolean onBackPressed() {
        if (!startIsFullScreen) {
            if (isFullScreen()) {
                ExitFullScreen();
            }
        } else {
            NewTVLauncherPlayerViewManager.getInstance().release();
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!enterFullScreen) {
            defaultWidth = getLayoutParams().width;
            defaultHeight = getLayoutParams().height;
        }
    }

    private void setParentWidth(View view, View rootView, int changeWidth, int changeHeight, int
            maxWidth, int maxHeight, boolean bringFront, boolean isExit) {
        if (view.getParent() != null && view.getParent() instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view.getParent();

            if (bringFront) {
                view.bringToFront();
            }
            ViewGroup.LayoutParams layoutParams = viewGroup.getLayoutParams();
            boolean isSame = (!isExit && viewGroup.getWidth() == defaultWidth && viewGroup
                    .getHeight() == defaultHeight) || (isExit && viewGroup.getWidth() ==
                    maxWidth &&
                    viewGroup.getHeight() == maxHeight);
            if (isSame) {
                layoutParams.width = isExit ? defaultWidth : maxWidth;
                layoutParams.height = isExit ? defaultHeight : maxHeight;
            } else {
                layoutParams.width = viewGroup.getWidth() + (isExit ? -changeWidth : changeWidth);
                layoutParams.height = viewGroup.getHeight() + (isExit ? -changeHeight :
                        changeHeight);
            }
            viewGroup.setLayoutParams(layoutParams);
            if (viewGroup == rootView) return;
            setParentWidth(viewGroup, rootView, changeWidth, changeHeight, maxWidth, maxHeight,
                    bringFront, isExit);
        }
    }

    public void delayEnterFullScreen(final Activity activity, final boolean bringFront, int delay) {
        if (enterFullScreen) return;
        enterFullScreen = true;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                enterFullScreen = false;
                EnterFullScreen(activity, bringFront);
            }
        }, delay);
    }

    public void EnterFullScreen(Activity activity, final boolean bringFront) {
        if (enterFullScreen) return;
        enterFullScreen = true;

        if (mPlayerLocation != null) {
            mPlayerLocation.destroy();
            mPlayerLocation = null;
        }

        final int screenWidth = activity.getWindow().getDecorView().getMeasuredWidth();
        final int screenHeight = activity.getWindow().getDecorView().getMeasuredHeight();

        final FrameLayout frameLayout = activity.getWindow().getDecorView().findViewById(android
                .R.id.content);
        View rootView = frameLayout.getChildAt(0);

        setParentWidth(this, rootView, screenWidth - defaultWidth, screenHeight - defaultHeight,
                screenWidth, screenHeight, bringFront, false);

        ViewGroup.LayoutParams container = getLayoutParams();
        container.width = screenWidth;
        container.height = screenHeight;
        setLayoutParams(container);

        mPlayerLocation = PlayerLocation.build(this, bringFront);

        ProgramIsChange = false;

        createMenuGroup();

        if (mNewTVLauncherPlayer != null && !mNewTVLauncherPlayer.isADPlaying()) {
            if (menuGroupPresenter != null && menuGroupPresenter instanceof MenuGroupPresenter) {
                ((MenuGroupPresenter) menuGroupPresenter).showHinter();
            }
            showSeekBar(mIsPause);
        }

        updateUIPropertys(true);
    }

    private void createMenuGroup() {
        if (menuGroupPresenter == null) {
            menuPopupWindow = new MenuPopupWindow();
            menuGroupPresenter = menuPopupWindow.show(getContext(), this);
        }
    }

    public void updateUIPropertys(boolean isFullScreen) {
        if (isFullScreen && menuGroupPresenter != null && menuGroupPresenter instanceof
                MenuGroupPresenter) {
            ((MenuGroupPresenter) menuGroupPresenter).enterFullScreen();
        }
        if (mPlayerFrameLayout != null) {
            mPlayerFrameLayout.updateTimeTextView(getResources().getDimensionPixelSize
                    (isFullScreen ? R.dimen.height_22px : R.dimen.height_12px));
        }
        if (mLoading != null) {
            mLoading.updatePropertys(getResources().getDimensionPixelSize(isFullScreen ? R.dimen
                    .height_22px : R.dimen.height_11px), isFullScreen);
        }
    }

    public boolean isFullScreen() {
        return this.getWidth() == ScreenUtils.getScreenW() && this.getHeight() == ScreenUtils
                .getScreenH();
    }

    public void destroy() {

        if (listener != null) {
            listener.clear();
        }

        if (menuPopupWindow != null) {
            menuPopupWindow.dismiss();
            menuPopupWindow = null;
        }

        if (menuGroupPresenter != null) {
            menuGroupPresenter.release();
            menuGroupPresenter = null;
        }

        mLiveCallBackEvent = null;
        mCallBackEvent = null;

        mNewTVLauncherPlayer = null;
        mNewTVLauncherPlayerSeekbar = null;

        mProgramSeriesInfo = null;
        mProgramDetailInfo = null;
        onDetachedFromWindow();
    }

    public void release() {
        addHistory();
        Log.i(TAG, "release: ");
        if (listener != null) {
            listener.clear();
        }

        if (mNewTVLauncherPlayer != null) {
            mNewTVLauncherPlayer.release();
        }
        if (mNewTVLauncherPlayerSeekbar != null) {
            mNewTVLauncherPlayerSeekbar.release();
        }

        stopLoading();
        if (mLoading != null) {
            mLoading.release();
            mLoading = null;
        }

        hidePauseImage();

        isReleased = true;

        destroy();
    }

    public boolean isReleased() {
        return isReleased;
    }

    private String translateUrl(String url, int delay) {
        return mLiveInfo.setTimeDelay(delay);
    }

    protected void initView(Context context) {
        mNewTVLauncherPlayer = new NewTVLauncherPlayer();

        View view = LayoutInflater.from(getContext().getApplicationContext()).inflate(R.layout
                .newtv_launcher_player_view, this);
        mPlayerFrameLayout = (VideoFrameLayout) view.findViewById(R.id.player_view_framelayout);
        mNewTVLauncherPlayerSeekbar = (NewTVLauncherPlayerSeekbar) view.findViewById(R.id
                .player_seekbar_area);

        mLoading = (NewTVLauncherPlayerLoading) view.findViewById(R.id.player_loading);
        mNewTVLauncherPlayerSeekbar.setmNewTVLauncherPlayer(mNewTVLauncherPlayer);

        updateUIPropertys(defaultConfig != null ? defaultConfig.isFullScreen : startIsFullScreen);

        PLAYER_ID = NewTVLauncherPlayerViewManager.getInstance().setPlayerView(this);


        mLivePresenter = new LiveContract.LivePresenter(getContext(), this);
        mVodPresenter = new VodContract.VodPresenter(getContext(), this);
    }

    private boolean equalsInfo(Content AInfo, Content BInfo) {
        if (AInfo == null || BInfo == null) return false;
        if (TextUtils.isEmpty(AInfo.getContentUUID()) || TextUtils.isEmpty(BInfo.getContentUUID()
        )) {
            return false;
        }
        Log.e(TAG, "AInfo Id=" + AInfo.getContentUUID() + " BInfo Id=" + BInfo.getContentUUID());
        return AInfo.getContentUUID().equals(BInfo.getContentUUID());
    }

    /*
     * 播放节目集
     * programSeriesInfo 节目集信息
     * isNeedStartActivity 是否需要启动新的activity（播放器内选集，切换清晰度时为false）
     * index 播放第几集
     * position 从什么位置开始播放
     * */
    public void playProgramSeries(Content programSeriesInfo, boolean
            isNeedStartActivity, int index, int position) {
        unshowLoadBack = false;
        LogUtils.i(TAG, "playVideo: index=" + index + " position=" + position);
        updatePlayStatus(2, index, position);
        if (isFullScreen() && !equalsInfo(mProgramSeriesInfo, programSeriesInfo)) {
            ProgramIsChange = true;
        }
        mProgramSeriesInfo = programSeriesInfo;

        List<SubContent> programsInfos = programSeriesInfo.getData();
        if (programsInfos != null && programsInfos.size() > index) {
            SubContent program = programsInfos.get(index);
            if (mNewTVLauncherPlayerSeekbar != null) {
                boolean hasMutipleProgram = programsInfos.size() > 1;
                mNewTVLauncherPlayerSeekbar.setProgramName(program.getTitle(),
                        hasMutipleProgram);
            }

            if (mNewTVLauncherPlayerSeekbar != null) {
                mNewTVLauncherPlayerSeekbar.setProgramName(program.getTitle(),
                        false);
            }

            if (mLoading != null) {
                mLoading.setProgramName(program.getTitle());
            }

            playIndex(index);

            String seriesUUID = programSeriesInfo.getContentUUID();

//            if (programsInfos.get(index).isMenuGroupHistory()) {
//                seriesUUID = programsInfos.get(index).getSeriesSubUUID();
//            } else {
//                seriesUUID = programSeriesInfo.getContentUUID();
//            }

            mVodPresenter.checkVod(program.getContentUUID(), seriesUUID);

            startLoading();
            isNeedStartActivity(isNeedStartActivity, programSeriesInfo, index);
        } else {
            LogUtils.i(TAG, "playVideo: programsInfos == null || programsInfos.size() <= index");
            onError("-8", "播放信息为空");
        }

    }

    /*
     * add by lxf
     * 播放直播流
     * programSeriesInfo 节目集信息
     * isNeedStartActivity 是否需要启动新的activity（播放器内选集，切换清晰度时为false）
     * index 播放第几集
     * position 从什么位置开始播放
     * */
    public void playLive(LiveInfo liveInfo, boolean isNeedStartActivity) {
        unshowLoadBack = false;
        LogUtils.i(TAG, "playlive playVideo");
        updatePlayStatus(3, 0, 0);
        mLivePresenter.checkLive(liveInfo);
        startLoading();
        isNeedStartActivity(isNeedStartActivity, null, 0);
    }


    private void playAlive(LiveInfo liveInfo) {
        VideoDataStruct videoDataStruct = new VideoDataStruct();
        videoDataStruct.setPlayType(PlayerConstants.PLAYTYPE_LIVE);
        if (liveInfo.isTimeShift()) {
            videoDataStruct.setPlayUrl(liveInfo.getmLiveUrl());
        } else {
            videoDataStruct.setPlayUrl(liveInfo.getDefaultLiveUrl());
        }

        videoDataStruct.setDataSource(PlayerConstants.DATASOURCE_ICNTV);
        videoDataStruct.setDeviceID(Constant.UUID);
        videoDataStruct.setKey(liveInfo.getKey());
        videoDataStruct.setContentUUID(liveInfo.getContentUUID());
        mNewTVLauncherPlayer.playAlive(getContext(), mPlayerFrameLayout, liveInfo,
                mLiveCallBackEvent,
                videoDataStruct);
    }

    /*
     * 播放节目
     * programDetailInfo 节目信息
     * position 从什么位置开始播放
     * */
    public void playProgramSingle(Content programDetailInfo, int position, boolean
            openActivity) {
        unshowLoadBack = false;
        LogUtils.i(TAG, "playProgram: ");
        if (programDetailInfo == null) {
            return;
        }

        updatePlayStatus(1, 0, position);
        mProgramSeriesInfo = programDetailInfo;

        if (mNewTVLauncherPlayerSeekbar != null) {
            mNewTVLauncherPlayerSeekbar.setProgramName(programDetailInfo.getTitle(), false);
        }

        if (mLoading != null) {
            mLoading.setProgramName(programDetailInfo.getTitle());
        }

        mVodPresenter.checkVod(programDetailInfo.getContentUUID(),
                programDetailInfo.getCgContentIDs());

        startLoading();

        isNeedStartActivity(openActivity, programDetailInfo, 0);
    }

    /**
     * 开始播放时进行状态和行为变更
     * type 1为单节目 2为节目集 3为直播
     */
    private void updatePlayStatus(int type, int index, int position) {
        setHintTextVisible(GONE);
        mIsPrepared = false;
        dismissChildView();

        switch (type) {
            case 1:
                mPlayType = PLAY_TYPE_SINGLE;
                isLiving = false;
                break;
            case 2:
                mPlayType = PLAY_TYPE_SERIES;
                isLiving = false;
                break;
            case 3:
                mPlayType = PLAY_TYPE_LIVE;
                isLiving = true;
                break;
        }

        if (!isLiving) {
            addHistory();
            PlayerConfig.getInstance().setJumpAD(NeedJumpAd);
            NeedJumpAd = false;
            if (enterFullScreen) {
                createMenuGroup();
            }
        }

        mPlayingIndex = index;
        mHistoryPostion = position;
    }

    private void isNeedStartActivity(boolean isNeedStartActivity, Content
            programDetailInfo, int index) {
        if (isNeedStartActivity) {
            Intent intent = new Intent(getContext(), NewTVLauncherPlayerActivity.class);
            Bundle bundle = new Bundle();
            if (programDetailInfo != null) {
                bundle.putSerializable("programSeriesInfo", programDetailInfo);
            }
            bundle.putInt("index", index);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtils.i(TAG, "onKeyDown: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (mIsLoading) {
                    return true;
                }

                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                LogUtils.i(TAG, "onKeyDown: KEYCODE_DPAD_LEFT");
                if (!mIsPrepared) {
                    return true;
                }
                if (mShowingChildView == SHOWING_NO_VIEW) {
                    showSeekBar(mIsPause);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                LogUtils.i(TAG, "onKeyDown: KEYCODE_DPAD_RIGHT");
                if (!mIsPrepared) {
                    return true;
                }
                if (mShowingChildView == SHOWING_NO_VIEW) {
                    showSeekBar(mIsPause);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!mIsPrepared || mPlayType == PLAY_TYPE_LIVE) {
                    LogUtils.i(TAG, "onKeyDown: mIsPrepared is false");
                    return true;
                }
                if (mPlayType == PLAY_TYPE_SERIES) {
                    if (mProgramSeriesInfo == null || mProgramSeriesInfo.getData() == null ||
                            mProgramSeriesInfo.getData().size() <= 1) {
                        LogUtils.i(TAG, "onKeyDown: mProgramSeriesInfo.getData()==null");
                        return true;
                    }
                } else if (mPlayType == PLAY_TYPE_SINGLE) {
                    return true;
                }

                if (mShowingChildView == SHOWING_SEEKBAR_VIEW) {
                    dismissChildView();
                    return true;
                } else if (mShowingChildView == SHOWING_EXIT_VIEW) {

                } else if (mShowingChildView == SHOWING_NUMBER_PROGRAM_SELECTOR) {

                } else if (mShowingChildView == SHOWING_NO_VIEW) {
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showSeekBar(boolean isPause) {
        if (isLiving() && mLiveInfo != null && !mLiveInfo.isTimeShift()) {
            return;
        }

        if (mShowingChildView != SHOWING_NO_VIEW) {
            return;
        }
        if (mNewTVLauncherPlayerSeekbar != null) {
            if (isPause) {
                mNewTVLauncherPlayerSeekbar.showPauseIcon();
            } else {
                mNewTVLauncherPlayerSeekbar.show();
            }
        }
    }

    private void dismissChildView() {
        if (mShowingChildView == SHOWING_NO_VIEW) return;
        LogUtils.i(TAG, "dismissChildView: " + mShowingChildView);
        int current = mShowingChildView;
        mShowingChildView = SHOWING_NO_VIEW;
        boolean interrupt = false;
        if (widgetMap != null) {
            for (FocusWidget focusWidget : widgetMap.values()) {
                if (current == focusWidget.getId()) {
                    focusWidget.onBackPressed();
                    interrupt = true;
                    break;
                }
            }
        }
        if (!interrupt) {
            switch (current) {
                case SHOWING_SEEKBAR_VIEW:
                    if (mNewTVLauncherPlayerSeekbar != null) {
                        mNewTVLauncherPlayerSeekbar.dismiss();
                    }
                    break;
                case SHOWING_PROGRAM_TREE:
                    if (menuGroupPresenter != null && menuGroupPresenter.isShow() &&
                            menuGroupPresenter instanceof MenuGroupPresenter) {
                        menuGroupPresenter.gone();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        /**
         * 适配讯码盒子
         * 正常盒子按返回键返回KeyEvent.KEYCODE_BACK
         * 讯码盒子非长按返回KeyEvent.KEYCODE_ESCAPE  长按返回KeyEvent.KEYCODE_ESCAPE KeyEvent.KEYCODE_BACK
         */
        if (Libs.get().getFlavor().equals(DeviceUtil.XUN_MA)) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_ESCAPE:
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (mShowingChildView == SHOWING_SEEKBAR_VIEW
                                || mShowingChildView == SHOWING_PROGRAM_TREE) {
                            dismissChildView();
                        } else {
                            onBackPressed();
                        }
                    }
                    return true;
            }
        }

        //有限处理BACK按键事件，关闭进度条显示
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (mShowingChildView != SHOWING_NO_VIEW) {
                    dismissChildView();
                } else {
                    onBackPressed();
                }
            }
            return true;
        }

        if (widgetMap != null) {
            Collection<FocusWidget> widgets = widgetMap.values();
            for (FocusWidget widget : widgets) {
                if (widget.isOverride(event.getKeyCode())) {
                    if (widget.isRegisterKey(event)) {
                        if (!widget.isShowing()) {
                            widget.show(this, Gravity.LEFT);
                            widget.requestDefaultFocus();
                            NewTVLauncherPlayerViewManager.getInstance().setShowingView(widget
                                    .getId());
                        } else {
                            if (widget.isToggleKey(event.getKeyCode())) {
                                dismissChildView();
                            } else {
                                widget.dispatchKeyEvent(event);
                            }
                        }
                    }
                    return true;
                }
            }
        }

        if (mNewTVLauncherPlayer != null && mNewTVLauncherPlayer.isADPlaying()
                && KeyEventUtils.FullScreenAllowKey(event)) {
            return true;
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent
                .KEYCODE_DPAD_DOWN) {
            if (mShowingChildView == SHOWING_SEEKBAR_VIEW) {
                dismissChildView();
            }
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent
                .KEYCODE_ENTER) {
            if (mShowingChildView == SHOWING_NO_VIEW || mShowingChildView == SHOWING_SEEKBAR_VIEW) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    onKeyUp(event.getKeyCode(), event);
                }
                return true;
            }
        }

        if (mShowingChildView != SHOWING_NO_VIEW) {
            if (widgetMap != null && widgetMap.containsKey(mShowingChildView)) {
                FocusWidget focusWidget = widgetMap.get(mShowingChildView);
                if (focusWidget != null) {
                    focusWidget.dispatchKeyEvent(event);
                    return true;
                }
            }
        }

        if (mShowingChildView == SHOWING_SEEKBAR_VIEW &&
                (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT
                        || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            mNewTVLauncherPlayerSeekbar.dispatchKeyEvent(event);
            return true;
        }

        if (menuGroupPresenter != null && menuGroupPresenter.dispatchKeyEvent(event)) {
            return true;
        }

        if (isFullScreen()) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    if (onBackPressed()) {
                        return true;
                    }
                }
                if (NewTVLauncherPlayerViewManager.getInstance().onKeyUp(event.getKeyCode(),
                        event)) {
                    return true;
                }
            } else {
                if (NewTVLauncherPlayerViewManager.getInstance().onKeyDown(event.getKeyCode(),
                        event)) {
                    return true;
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtils.i(TAG, "onKeyUp: " + keyCode);

        if (!mIsPrepared) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                NewTVLauncherPlayerViewManager.getInstance().release();
            }
            return true;
        }
        switch (keyCode) {

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                LogUtils.i(TAG, "onKeyUp: KEYCODE_DPAD_CENTER");
                if (mIsLoading) {
                    return true;
                }
                if (mShowingChildView == SHOWING_SEEKBAR_VIEW || mShowingChildView ==
                        SHOWING_NO_VIEW) {
                    if (mNewTVLauncherPlayer == null) {
                        return true;
                    }

                    if (isLiving() && mLiveInfo != null && !mLiveInfo.isTimeShift()) {
                        return true;
                    }

                    if (mNewTVLauncherPlayer.isPlaying()) {
                        pause();
                    } else {
                        start();
                    }
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_BACK:
                LogUtils.i(TAG, "onKeyDown: KEYCODE_BACK");
                if (seekVisibleExit()) return true;
                break;
            case KeyEvent.KEYCODE_MENU:
                dismissChildView();
                break;
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * 进度条显示时返回键触发
     *
     * @return
     */
    private boolean seekVisibleExit() {
        if (mNewTVLauncherPlayerSeekbar.getVisibility() == VISIBLE) {
            mNewTVLauncherPlayerSeekbar.dismiss();
            return true;
        }
        if (mShowingChildView != SHOWING_NO_VIEW) {
            dismissChildView();
            return true;
        } else {
            NewTVLauncherPlayerViewManager.getInstance().release();
        }
        return false;
    }

    public boolean isLiving() {
        return isLiving;
    }

    // add by lxf
    private void playVodNext() {
        if (mPlayType == PLAY_TYPE_SINGLE || mPlayType == PLAY_TYPE_LIVE) {
            if (mPlayType != PLAY_TYPE_LIVE) {
                addHistory();
            }
            Toast.makeText(getContext(), getContext().getResources().getString(R.string
                    .play_complete), Toast.LENGTH_SHORT).show();
            AllComplete(false, "播放结束");

            if (startIsFullScreen) {
                NewTVLauncherPlayerViewManager.getInstance().release();
            }
        } else if (mPlayType == PLAY_TYPE_SERIES) {
            int next = mPlayingIndex + 1;
            if (mProgramSeriesInfo.getData().size() > next) {
                playProgramSeries(mProgramSeriesInfo, false, next, 0);
                if (listener != null && listener.size() > 0) {
                    for (IPlayProgramsCallBackEvent l : listener) {
                        l.onNext(mProgramSeriesInfo.getData().get(next), next, true);
                    }
                }
            } else {
                addHistory();
                if (listener != null && listener.size() > 0) {
                    for (IPlayProgramsCallBackEvent l : listener) {
                        l.onNext(null, next, false);
                    }
                }
                Toast.makeText(getContext(), getContext().getResources().getString(R.string
                        .play_complete), Toast.LENGTH_SHORT).show();
                if (startIsFullScreen) {
                    NewTVLauncherPlayerViewManager.getInstance().release();
                }
                AllComplete(false, "播放结束");
            }
        }
    }

    protected void playIndex(int index) {
    }

    protected void AllComplete(boolean isError, String info) {

        if (mNewTVLauncherPlayer != null) {
            mNewTVLauncherPlayer.release();
            mNewTVLauncherPlayer = null;
        }

    }

    public int getShowingView() {
        return mShowingChildView;
    }

    public void setShowingView(int showingView) {
        if (mShowingChildView == showingView) return;
        LogUtils.i(TAG, "setShowingView: showingView=" + showingView);
        if (mShowingChildView != SHOWING_NO_VIEW) {
            dismissChildView();
        }
        mShowingChildView = showingView;
    }

    private void startLoading() {
        if (mLoading != null) {
            mLoading.show(unshowLoadBack, NewTVLauncherPlayerLoading.allScrFlag);
            bringChildToFront(mLoading);
            mIsLoading = true;
        }
    }

    private void stopLoading() {
        if (mLoading != null) {
            mLoading.dismiss();
            mIsLoading = false;
        }
    }

    private void showPauseImage() {
        if (mNewTVLauncherPlayerSeekbar != null) {
            mNewTVLauncherPlayerSeekbar.setSeekPlayIcon();
        }
    }

    private void hidePauseImage() {
        if (mNewTVLauncherPlayerSeekbar != null) {
            mNewTVLauncherPlayerSeekbar.setSeekPauseIcon();
        }
    }

    public void start() {
        if (mNewTVLauncherPlayer != null)
            mNewTVLauncherPlayer.start();

        mIsPause = false;
        hidePauseImage();
        if (mIsPause && mNewTVLauncherPlayerSeekbar != null &&
                mNewTVLauncherPlayerSeekbar.getVisibility() == VISIBLE) {
            mNewTVLauncherPlayerSeekbar.hidePauseIcon();
        }
    }

    public void pause() {
        if (mNewTVLauncherPlayer != null)
            mNewTVLauncherPlayer.pause();

        mIsPause = true;
        showPauseImage();
        showSeekBar(true);
    }

    public void stop() {
        if (mNewTVLauncherPlayer != null) {
            mNewTVLauncherPlayer.stop();
        }
    }

    public int getIndex() {
        return mPlayingIndex;
    }

    public int getDuration() {
        if (mNewTVLauncherPlayer != null)
            return mNewTVLauncherPlayer.getDuration();

        return 0;
    }

    public int getCurrentPosition() {
        if (mNewTVLauncherPlayer != null)
            return mNewTVLauncherPlayer.getCurrentPosition();

        return 0;
    }

    public void addListener(IPlayProgramsCallBackEvent l) {
        if (listener == null) {
            listener = new ArrayList<>();
        }
        listener.add(l);
    }

    /**
     * 直播有结束时间，并且没有时移功能，就启动计时在结束时关闭直播
     */
    private void timer(Content mProgramSeriesInfo) {
        if (mProgramSeriesInfo != null && !TextUtils.isEmpty(mProgramSeriesInfo.getPlayEndTime())
                && !"1".equals(mProgramSeriesInfo.getIsTimeShift()))
            LiveTimingUtil.endTime(mProgramSeriesInfo.getPlayEndTime(), new LiveTimingUtil
                    .LiveEndListener() {
                @Override
                public void end() {
                    if (isLiving) {
                        mLiveCallBackEvent.onCompletion();
                    }
                }
            });
    }

    /**
     * 保存播放记录  在播放单节目和节目集的时候调用
     */
    private void addHistory() {
        if (mProgramSeriesInfo == null) {
            return;
        }
        RxBus.get().post(Constant.UPDATE_VIDEO_PLAY_INFO, new VideoPlayInfo(getIndex(),
                getCurrentPosition(), mProgramSeriesInfo.getContentUUID()));


        Player.get().onFinish(mProgramSeriesInfo, getCurrentPosition(), getDuration());

//        Log.i(TAG, "addHistory: " + getIndex() + ",position:" + getCurrentPosition());
//        DBUtil.addHistory(mProgramSeriesInfo, getIndex(), getCurrentPosition(), new
//                DBCallback<String>() {
//                    @Override
//                    public void onResult(int code, String result) {
//                        if (code == 0) {
//                            if (mProgramSeriesInfo != null) {
//                                LogUploadUtils.uploadLog(Constant.LOG_NODE_HISTORY, "0," +
//                                        mProgramSeriesInfo.getContentUUID());//添加历史记录
//                            }
//                            RxBus.get().post(Constant.UPDATE_UC_DATA, true);
//                        }
//                    }
//                });
    }

    public boolean isADPlaying() {
        return mNewTVLauncherPlayer != null && mNewTVLauncherPlayer.isADPlaying();
    }

    public boolean isPlaying() {
        return mNewTVLauncherPlayer != null && mNewTVLauncherPlayer.isPlaying();
    }

    public void setHintTextVisible(int visible) {
    }

    public void setVideoSilent(boolean isSilent) {
        if (mNewTVLauncherPlayer != null) {
            mNewTVLauncherPlayer.setVideoSilent(isSilent);
        }
    }

    @Override
    public void onVodchkResult(VideoDataStruct videoDataStruct, String contentUUID) {
        mContentUUid = contentUUID;
        if (mNewTVLauncherPlayer == null) {
            mNewTVLauncherPlayer = new NewTVLauncherPlayer();
        }
        if (mNewTVLauncherPlayerSeekbar != null) {
            mNewTVLauncherPlayerSeekbar.setmNewTVLauncherPlayer(mNewTVLauncherPlayer);
        }
        mNewTVLauncherPlayer.play(getContext(), mPlayerFrameLayout, mCallBackEvent,
                videoDataStruct);
    }

    @Override
    public void liveChkResult(LiveInfo liveInfo) {
        playAlive(mLiveInfo);
    }

    @Override
    public void onChkError(String code, String desc) {
        onError(code, desc);
    }

    @Override
    public void tip(@NotNull Context context, @NotNull String message) {

    }

    @Override
    public void onError(@NotNull Context context, @NotNull String desc) {

    }

    public static class PlayerViewConfig {
        public boolean prepared = false;
        public ViewGroup.LayoutParams layoutParams;     //布局属性
        public boolean isFullScreen;            //当前是否为全屏状态
        public ViewParent parentViewGroup;      //父级容器
        public boolean startIsFullScreen;       //开始时候是不是全屏状态
        public View defaultFocusView;           //进入全屏时候的默认焦点位置
        public PlayerCallback playerCallback;
        public int playPosition;
        public VPlayCenter playCenter;
    }
}