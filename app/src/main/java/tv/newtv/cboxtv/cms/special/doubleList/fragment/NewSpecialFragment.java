package tv.newtv.cboxtv.cms.special.doubleList.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.ModelResult;
import com.newtv.cms.bean.Page;
import com.newtv.cms.bean.Program;
import com.newtv.cms.bean.SubContent;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.util.RxBus;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import tv.newtv.cboxtv.LauncherApplication;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.special.doubleList.adapter.NewSpecialCenterAdapter;
import tv.newtv.cboxtv.cms.special.doubleList.adapter.NewSpecialLeftAdapter;
import tv.newtv.cboxtv.cms.special.doubleList.view.FocusRecyclerView;
import tv.newtv.cboxtv.cms.special.fragment.BaseSpecialContentFragment;
import tv.newtv.cboxtv.player.IPlayProgramsCallBackEvent;
import tv.newtv.cboxtv.player.videoview.PlayerCallback;
import tv.newtv.cboxtv.player.videoview.VideoExitFullScreenCallBack;
import tv.newtv.cboxtv.player.videoview.VideoPlayerView;
import tv.newtv.cboxtv.player.view.NewTVLauncherPlayerViewManager;

/**
 * 双列表专题界面
 */

public class NewSpecialFragment extends BaseSpecialContentFragment implements PlayerCallback {

    private static final String TAG = NewSpecialFragment.class.getSimpleName();
    private static final String PAGEUUID = "3a6cc222-bb3f-11e8-8f40-c7d8a7a18cc4";
    private static final String UP = "up";
    private static final String DOWN = "down";
    private static final int MIN_DIS_POSTION = 0;
    private static final int MAX_DIS_POSTION = 8;
    private static final int LEFT_TO_CENTER_POSITION = 0X001;
    private static final int VIDEO_TO_CENTER_POSITION = 0X002;
    private static final int SELECT_DEFAULT_ITEM = 0X004;
    private static final int VIDEO_PLAY = 0X005;
    private static final int VIDEO_NEXT_PLAY = 0X006;
    private static final int LEFT_SCROLL_POSITION = 0X008;
    private static final int MSG_REFRESH_CENTER_DATA = 0X009;
    private static final int MSG_PAUSE_RESUME_SLECT = 0X010;
    private LinearLayout mNewSpecialLayout;
    private ImageView mLeftUp, mLeftDown, mCenterUp, mCenterDown;
    private FocusRecyclerView mLeftMenu, mCenterMenu;
    private NewSpecialLeftAdapter mNewSpecialLeftAdapter;
    private NewSpecialCenterAdapter mNewSpecialCenterAdapter;
    private LinearLayoutManager mLeftManager, mCenterManager;
    private TextView mSpecialTopicName, mSpecialTopicTitle;
    private LinearLayout mVideoLinear;
    private FrameLayout mFocusViewVideo;
    private TextView mVideoPlayerTitle;
    private ImageView mFullScreenImage;
    private List<Program> mLeftData = new ArrayList<>();
    private List<Program> mLiftListData = new ArrayList<>();
    private List<Program> mLeftFocusedData = new ArrayList<>();
    private List<SubContent> mCenterData = new ArrayList<>();
    private List<SubContent> mCenterFocusedData = new ArrayList<>();
    private boolean isFristPlay = true, isRightToLeft = false, isMoveKey = false, isPlayNextProgram = false;
    private int leftPosition = 0, oldLeftPosition = -1, centerPosition = 0, oldCenterPosition = 0;
    private String mDefaultContentUUID;
    private View focusView;
    private String mMoveTag = DOWN;
    private int currentIndex = 0;
    private ModelResult<ArrayList<Page>> mModuleInfoResult;
    private String mLeftContentId;
    private Content mProgramSeriesInfo;
    private Observable<Boolean> isVideoEndObservable;
    private Observable<Boolean> isHaveAdObservable;
    private boolean isHaveAD;
    private HashMap<String, Content> mCacheSubContents;
    private String uuidTag;
    private boolean isRefreshCenter = false;

    private SpecialHandler mSpecialHandler = new SpecialHandler(NewSpecialFragment.this);

    class SpecialHandler extends Handler {
        WeakReference<NewSpecialFragment> mWeakHandler;

        public SpecialHandler(NewSpecialFragment activity) {
            mWeakHandler = new WeakReference<NewSpecialFragment>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            NewSpecialFragment mNewSpecialFragment = mWeakHandler.get();
            if (videoPlayerView != null) {
                videoPlayerView.setVideoPlayNext(leftPosition < mLeftData.size() - 1);
            }
            if (mNewSpecialFragment != null) {
                switch (msg.what) {
                    case LEFT_TO_CENTER_POSITION:
                        if (mCenterManager.findViewByPosition(msg.arg1) != null) {
                            mCenterManager.findViewByPosition(msg.arg1).requestFocus();
                        } else {
                            printLogAndToast("Handler", "ywy left to center is null", false);
                        }
                        break;
                    case VIDEO_TO_CENTER_POSITION:
                        if (mCenterManager.findViewByPosition(centerPosition) != null) {
                            mCenterManager.findViewByPosition(centerPosition).requestFocus();
                        } else {
                            printLogAndToast("Handler", "ywy initVideo key left is null", false);
                        }
                        break;
                    case VIDEO_PLAY:
                        if (mCenterData.size() > 0) {
                            setVideoFocusedPlay(0);
                            sendEmptyMessageDelayed(SELECT_DEFAULT_ITEM, 50);
                        } else {
                            printLogAndToast("Handler", "ywy video playurl is null", false);
                        }
                        break;
                    case SELECT_DEFAULT_ITEM:
                        setSelectBg(0, false);
                        break;
                    case VIDEO_NEXT_PLAY:
                        if (mLeftFocusedData != null && mLeftFocusedData.size() > 0) {
                            setPageNameAndTitle();
                        } else {
                            if (mLeftData != null) {
                                printLogAndToast("Handler", "video next playVod data: " + mLeftData.toString(), false);
                            } else {
                                printLogAndToast("Handler", "video next playVod data is null ", false);
                            }
                        }
                        if (mCenterFocusedData != null && mCenterFocusedData.size() > 0) {
                            mVideoPlayerTitle.setText(mCenterFocusedData.get(centerPosition).getTitle());
                        }
                        setVideoFocusedPlay(centerPosition);
                        mNewSpecialCenterAdapter.setSelected(centerPosition);
                        break;
                    case LEFT_SCROLL_POSITION:
                        /*if (mLeftMenu != null && mLeftMenu.getChildAt(leftPosition) != null) {
                            mLeftMenu.getChildAt(leftPosition).setFocusable(true);
                            mLeftMenu.getChildAt(leftPosition).requestFocus();
                        } */
                        if (mLeftManager.findViewByPosition(leftPosition) != null) {
                            mLeftManager.findViewByPosition(leftPosition).requestFocus();
                        } else {
                            printLogAndToast("left_scroll_position", "position is null", false);
                        }
                        break;
                    case MSG_REFRESH_CENTER_DATA:
                        if (mCenterMenu != null) {
                            mCenterMenu.scrollToPosition(0);
                            printLogAndToast("Handler", "scrollToPosition", false);
                        } else {
                            printLogAndToast("Handler", "msg_refresh_center_data", false);
                        }
                        isRefreshCenter = true;
                        break;
                    case MSG_PAUSE_RESUME_SLECT:
                        mCenterMenu.scrollToPosition(centerPosition);
                        sendEmptyMessageDelayed(VIDEO_TO_CENTER_POSITION, 50);
                        break;
                    default:
                        break;
                }
            }
        }

    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_newspecial_layout;
    }

    @Override
    protected void onItemContentResult(String uuid, Content content, int playIndex) {
        //处理返回的数据
        printLogAndToast("OnItemContentResult", "uuidTag : " + uuidTag + "  uuid : " + uuid + " content : " + content.toString(), false);
        if (mCacheSubContents == null) {
            mCacheSubContents = new HashMap<>();
        }
        if (uuidTag.equals(uuid)) {
            mCacheSubContents.put(leftPosition + uuid, content);
            refreshCenterData(leftPosition, content);
            mProgramSeriesInfo = content;
        }
        initCenterDownStatus();

        if (isPlayNextProgram) {
            setVideoFocusedPlay(0);
            setSelectBg(0, true);
        }

    }

    @Override
    protected void setUpUI(View view) {
        initTitle(view);
        initLeftList(view);
        initCenterList(view);
        initVideo(view);
        setPlayNextVideo();
    }

    @Override
    public void setModuleInfo(ModelResult<ArrayList<Page>> infoResult) {
        mModuleInfoResult = infoResult;
        String uuid = getArguments().getString(Constant.DEFAULT_UUID);
        if (null != mModuleInfoResult) {
            mLeftData = mModuleInfoResult.getData().get(0).getPrograms();
            printLogAndToast("setModuleInfo", "leftData : " + mLeftData.toString(), false);
        } else {
            printLogAndToast("setModuleInfo", "ywy Modulenfo 1 is null :   uuid : " + uuid, false);
        }
        if (null != mLeftData) {
            for (int i = 0; i < mLeftData.size(); i++) {
                if (mLeftData.get(i).getDefaultFocus() == 1) {
                    leftPosition = i;
                    mDefaultContentUUID = mLeftData.get(i).getL_id();
                    printLogAndToast("setLeftDefaultFocusedP", "default leftPosition : " + leftPosition + "  mDefaultContentUUID : " + mDefaultContentUUID, false);
                } else {
                    //测试
                    //mLiftListData.add(mLeftData.get(i));
                }
            }
        } else {
            printLogAndToast("setModuleInfo", "ywy Modulenfo 2 : is null", false);
        }
    }

    @Override
    public void onEpisodeChange(int index, int position) {
        currentIndex = index;
    }

    @Override
    public void onPlayerClick(VideoPlayerView videoPlayerView) {
        videoPlayerView.EnterFullScreen(getActivity(), false);
        mVideoPlayerTitle.setVisibility(View.GONE);
        mFullScreenImage.setVisibility(View.GONE);
    }

    @Override
    public void AllPlayComplete(boolean isError, String info, VideoPlayerView videoPlayerView) {
        if (mCenterFocusedData.size() - 1 <= centerPosition) {
            if (mVideoPlayerTitle != null) {
                mVideoPlayerTitle.setVisibility(View.GONE);
            }
            return;
        } else {
            centerPosition++;
            mCenterMenu.scrollToPosition(centerPosition);
            mSpecialHandler.sendEmptyMessageDelayed(VIDEO_NEXT_PLAY, 50);
        }
    }

    @Override
    public void ProgramChange() {
        Content programSeriesInfo = NewTVLauncherPlayerViewManager.getInstance().getProgramSeriesInfo();
        if (programSeriesInfo != null && TextUtils.equals(programSeriesInfo.getContentID(), mProgramSeriesInfo.getContentID())) {
            currentIndex = NewTVLauncherPlayerViewManager.getInstance().getIndex();
        } else {
            videoPlayerView.setSeriesInfo(mProgramSeriesInfo);
            videoPlayerView.playSingleOrSeries(currentIndex, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        printLogAndToast("onResume","onResume left : " + leftPosition + " old left : "+ oldLeftPosition + "  center : " + centerPosition, false);
        if (oldLeftPosition != -1 && centerPosition != -1) {
            leftPosition = oldLeftPosition;
            setScrollLeftPosition();
            mSpecialHandler.sendEmptyMessageDelayed(MSG_PAUSE_RESUME_SLECT, 100);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isMoveKey = false;
        mDefaultContentUUID = null;
        oldLeftPosition = 0;
        leftPosition = -1;
        centerPosition = 0;
        /*mLeftData.clear();
        mLeftFocusedData.clear();
        mCenterData.clear();
        mCenterFocusedData.clear();
        mNewSpecialCenterAdapter.clearList();*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isFristPlay = true;
        videoPlayerView = null;
        RxBus.get().unregister(Constant.IS_VIDEO_END, isVideoEndObservable);
        RxBus.get().unregister(Constant.IS_HAVE_AD, isHaveAdObservable);
        mSpecialHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (getContentView() != null) {
                focusView = getContentView().findFocus();
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                mMoveTag = UP;
                isMoveKey = true;
                isRefreshCenter = false;
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                mMoveTag = DOWN;
                isMoveKey = true;
                isRefreshCenter = false;
            }
            if (focusView instanceof VideoPlayerView) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        mVideoPlayerTitle.setVisibility(View.GONE);
                        mFullScreenImage.setVisibility(View.GONE);
                        if (leftPosition == oldLeftPosition) {
                            centerPosition = oldCenterPosition;
                        }
                        mCenterMenu.scrollToPosition(centerPosition);
                        mSpecialHandler.sendEmptyMessageDelayed(VIDEO_TO_CENTER_POSITION, 50);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void initTitle(View view) {
        mNewSpecialLayout = view.findViewById(R.id.fragment_newspecial_layout);
        mSpecialTopicName = view.findViewById(R.id.fragment_newspecial_video_name);
        mSpecialTopicTitle = view.findViewById(R.id.fragment_newspecial_video_title);
        setPageNameAndTitle();
        if (mModuleInfoResult != null) {
            String url = mModuleInfoResult.getBackground();
            if (TextUtils.isEmpty(url)) {
                //mNewSpecialLayout.setBackgroundResource(R.drawable.new_special_bg);
            }
        }

    }

    private void setPageNameAndTitle() {
        if (mModuleInfoResult != null) {
            mSpecialTopicName.setText(mModuleInfoResult.getSubTitle());
            mSpecialTopicTitle.setText(mModuleInfoResult.getDescription());
        }
    }

    private void setLeftUpVisible(String tag, int leftFocusPt) {
        if (null != mLeftUp) {
            int first = mLeftManager.findFirstVisibleItemPosition();
            int last = mLeftManager.findLastVisibleItemPosition();
            printLogAndToast("setLeftUpVisible", " setLeftUpVisible tag : " + tag
                    + " first : " + first + " last : " + last, false);
            if (!TextUtils.isEmpty(tag) && tag.equals(UP)) {
                if (first > MIN_DIS_POSTION) {
                    mLeftUp.setVisibility(View.VISIBLE);
                } else {
                    mLeftUp.setVisibility(View.INVISIBLE);
                }
            } else if (!TextUtils.isEmpty(tag) && tag.equals(DOWN)) {
                if (first <= MIN_DIS_POSTION) {
                    mLeftUp.setVisibility(View.INVISIBLE);
                } else {
                    mLeftUp.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void setLeftDownVisible(String tag, int leftFocusPt) {
        printLogAndToast("setLeftDownVisible", "setLeftDownVisible tag : " + tag + " leftP : " + leftFocusPt, false);
        if (!TextUtils.isEmpty(tag) && tag.equals(DOWN)) {
            if ((mLeftData.size() <= MAX_DIS_POSTION + 1) || leftFocusPt == mLeftData.size() - 1) {
                mLeftDown.setVisibility(View.INVISIBLE);
            } else if ((mLeftData.size() > MAX_DIS_POSTION + 1)) {
                mLeftDown.setVisibility(View.VISIBLE);
            }
        } else if ((!TextUtils.isEmpty(tag) && tag.equals(UP)) && leftFocusPt < mLeftData.size() - 1 - MAX_DIS_POSTION) {
            mLeftDown.setVisibility(View.VISIBLE);
        } else {
            mLeftDown.setVisibility(View.INVISIBLE);
        }
    }

    private void setLeftArrowVisible() {
        if (mLeftUp != null && mLeftDown != null) {
            int first = mLeftManager.findFirstVisibleItemPosition();
            int last = mLeftManager.findLastVisibleItemPosition();
            if (first <= MIN_DIS_POSTION) {
                mLeftUp.setVisibility(View.INVISIBLE);
            } else {
                mLeftUp.setVisibility(View.VISIBLE);
            }
            if (last >= MAX_DIS_POSTION) {
                mLeftDown.setVisibility(View.VISIBLE);
            } else {
                mLeftDown.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void initLeftDownStatus() {
        if (null != mLeftData && mLeftData.size() > MAX_DIS_POSTION + 1) {
            mLeftDown.setVisibility(View.VISIBLE);
        } else {
            mLeftDown.setVisibility(View.INVISIBLE);
        }
    }

    private void initLeftList(View view) {
        mLeftUp = view.findViewById(R.id.fragment_newspecial_left_up);
        mLeftDown = view.findViewById(R.id.fragment_newspecial_left_down);
        mLeftMenu = view.findViewById(R.id.fragment_newspecial_left_list);
        mLeftManager = new LinearLayoutManager(getContext());
        mLeftManager.setOrientation(LinearLayoutManager.VERTICAL);
        mLeftMenu.setLayoutManager(mLeftManager);
        mNewSpecialLeftAdapter = new NewSpecialLeftAdapter(getContext(), mLeftData, leftPosition);
        mNewSpecialLeftAdapter.setOnFoucesDataChangeListener(new NewSpecialLeftAdapter.OnFoucesDataChangeListener() {
            @Override
            public void onFoucesDataChangeListener(String contentId, int position) {
                printLogAndToast("initLeftList", "position : " + position, false);
                printLogAndToast("initLeftList", "isRightToLeft : " + isRightToLeft + "  isMoveKey : " + isMoveKey, false);
                if (!isRightToLeft) {
                    if (!isMoveKey) {
                        if (!TextUtils.isEmpty(mDefaultContentUUID)) {
                            mLeftContentId = mDefaultContentUUID;
                        } else {
                            mLeftContentId = contentId;
                        }
                        if (leftPosition > 0) {
                            position = leftPosition;
                        }
                    } else {
                        //centerPosition = 0;
                        mCenterUp.setVisibility(View.INVISIBLE);
                        mCenterDown.setVisibility(View.INVISIBLE);
                        mLeftContentId = contentId;
                    }
                    getCenterData(position, mLeftContentId, false);
                } else {
                    isRightToLeft = false;
                }
                setLeftUpVisible(mMoveTag, position);
                setLeftDownVisible(mMoveTag, position);
            }
        });
        mLeftMenu.setAdapter(mNewSpecialLeftAdapter);
        playNextProgram();
        mLeftMenu.setOnGetPositionListener(new FocusRecyclerView.OnGetPositionListener() {
            @Override
            public void GetRightPositionListener(int position) {
                leftPosition = position;
                int firstVP = mCenterManager.findFirstVisibleItemPosition();
                int lastVP = mCenterManager.findLastVisibleItemPosition();
                Message msg = Message.obtain();
                msg.what = LEFT_TO_CENTER_POSITION;
                printLogAndToast("initLeftList", "left to center oldP : " + oldLeftPosition
                        + " position : " + position + "  centerPosition : " + centerPosition
                        + "  oldcenterPosition : " + oldCenterPosition, false);
                if (position == oldLeftPosition) {
                    mCenterMenu.scrollToPosition(oldCenterPosition);
                    msg.arg1 = oldCenterPosition;
                } else {
                    centerPosition = 0;
                    if (centerPosition < firstVP) {
                        mCenterMenu.scrollToPosition(centerPosition);
                    } else if (centerPosition > firstVP && centerPosition < lastVP) {
                        int top = mCenterMenu.getChildAt(centerPosition).getTop();
                        mCenterMenu.smoothScrollBy(0, top);
                    } else {
                        mCenterMenu.scrollToPosition(centerPosition);
                    }
                    msg.arg1 = centerPosition;
                }
                if (isRefreshCenter) {
                    mSpecialHandler.sendMessageDelayed(msg, 50);
                } else {
                    printLogAndToast("initLeftList", "isRefreshCenter" + isRefreshCenter, false);
                }
                mLeftUp.setVisibility(View.INVISIBLE);
                mLeftDown.setVisibility(View.INVISIBLE);
            }

            @Override
            public void GetLeftPositionListener(int position) {
                printLogAndToast("initLeftList", "ywy left position left : " + position, false);
            }
        });

        setScrollLeftPosition();
        initLeftDownStatus();
    }

    private void setScrollLeftPosition() {
        mLeftMenu.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int leftFirstVP = mLeftManager.findFirstVisibleItemPosition();
                int leftLastVP = mLeftManager.findLastVisibleItemPosition();
                printLogAndToast("initLeftList ", " first : " + leftFirstVP + "  last : " + leftLastVP, false);
                if (leftPosition > 7) {
                    mLeftMenu.scrollToPosition(leftPosition);
                } else {
                    printLogAndToast(TAG, "ywy GetLeftPositionListener is null", false);
                }

                mSpecialHandler.sendEmptyMessageDelayed(LEFT_SCROLL_POSITION, 50);
                mLeftMenu.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void setCenterUpVisible(String tag, int centerFocusPt) {
        if (null != mCenterUp) {
            if ((!TextUtils.isEmpty(tag) && tag.equals(UP))) {
                if (centerFocusPt > MIN_DIS_POSTION) {
                    mCenterUp.setVisibility(View.VISIBLE);
                } else {
                    mCenterUp.setVisibility(View.INVISIBLE);
                }
            } else if ((!TextUtils.isEmpty(tag) && tag.equals(DOWN))) {
                if (centerFocusPt > MAX_DIS_POSTION) {
                    mCenterUp.setVisibility(View.VISIBLE);
                } else {
                    mCenterUp.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void setCenterDownVisible(String tag, int centerFocusPt) {
        if (!TextUtils.isEmpty(tag) && tag.equals(DOWN)) {
            if ((mCenterData.size() > MAX_DIS_POSTION + 1) && (centerFocusPt == mCenterData.size() - 1)) {
                mCenterDown.setVisibility(View.INVISIBLE);
            } else if ((mCenterData.size() > MAX_DIS_POSTION)) {
                mCenterDown.setVisibility(View.VISIBLE);
            }
        } else if ((!TextUtils.isEmpty(tag) && tag.equals(UP)) && centerFocusPt < mCenterData.size() - 1 - MAX_DIS_POSTION) {
            mCenterDown.setVisibility(View.VISIBLE);
        } else {
            mCenterDown.setVisibility(View.INVISIBLE);
        }
    }

    private void initCenterDownStatus() {
        if (null != mCenterData && mCenterData.size() > MAX_DIS_POSTION + 1) {
            mCenterDown.setVisibility(View.VISIBLE);
        } else {
            mCenterDown.setVisibility(View.INVISIBLE);
        }
    }

    private void initCenterList(View view) {
        mCenterUp = view.findViewById(R.id.fragment_newspecial_center_up);
        mCenterDown = view.findViewById(R.id.fragment_newspecial_center_down);

        mCenterMenu = view.findViewById(R.id.fragment_newspecial_center_list);
        mCenterManager = new LinearLayoutManager(LauncherApplication.AppContext);
        mCenterManager.setOrientation(LinearLayoutManager.VERTICAL);
        mCenterMenu.setLayoutManager(mCenterManager);
        mNewSpecialCenterAdapter = new NewSpecialCenterAdapter(LauncherApplication.AppContext, mCenterData);
        mCenterMenu.setAdapter(mNewSpecialCenterAdapter);
        mCenterMenu.setOnGetPositionListener(new FocusRecyclerView.OnGetPositionListener() {
            @Override
            public void GetRightPositionListener(int position) {
                //centerPosition = position;
                printLogAndToast("initCenterList", "center get right position : " + position, false);
                setVideoFocus(true);
                setVideoPlayerTipStatus();
            }

            @Override
            public void GetLeftPositionListener(int position) {
                printLogAndToast("initCenterList", "center get   left leftPosition : " + leftPosition + "  position: " + position, false);
                isRightToLeft = true;
                int firstVP = mLeftManager.findFirstVisibleItemPosition();
                int lastVP = mLeftManager.findLastVisibleItemPosition();
                printLogAndToast("initLeftList ", " first : " + firstVP + "  last : " + lastVP, false);
                if (mLeftMenu.getChildAt(leftPosition - firstVP) != null) {
                    mLeftMenu.getChildAt(leftPosition - firstVP).requestFocus();
                } else {
                    printLogAndToast(TAG, "ywy GetLeftPositionListener is null", false);
                }
                setLeftArrowVisible();
            }
        });
        mNewSpecialCenterAdapter.setOnVideoChangeListener(new NewSpecialCenterAdapter.OnVideoChangeListener() {
            @Override
            public void onVideoChangeListener(String title, int position) {
                centerPosition = position;
                mLeftFocusedData = mLeftData;
                mCenterFocusedData = mCenterData;
                if (mCenterData.get(position) != null && (position <= mCenterData.size() - 1)) {
                    setPageNameAndTitle();
                    printLogAndToast("initCenterList", "centerTitle : " + mCenterFocusedData.get(centerPosition).getTitle()
                            + "centerPosition : " + centerPosition, false);
                    mVideoPlayerTitle.setText(mCenterFocusedData.get(centerPosition).getTitle());
                } else {
                    printLogAndToast("initCenterList", "mCenter position view is null", false);
                }
                setVideoFocus(true);
                setSelectBg(position, true);
                setVideoFocusedPlay(position);
            }
        });
        mNewSpecialCenterAdapter.setOnFocusedDataChangeListener(new NewSpecialCenterAdapter.OnFocusedDataChangeListener() {
            @Override
            public void onFocusedDataChangeListener(String contentId, int position) {
                setCenterUpVisible(mMoveTag, position);
                setCenterDownVisible(mMoveTag, position);
            }
        });
    }

    private void initVideo(View view) {
        mVideoLinear = view.findViewById(R.id.video_linear);
        mFocusViewVideo = view.findViewById(R.id.fragment_newspecial_video_focus);
        videoPlayerView = view.findViewById(R.id.fragment_newspecial_video_player);
        videoPlayerView.setPlayerCallback(this);
        videoPlayerView.setFocusView(mFocusViewVideo, true);
        mVideoPlayerTitle = view.findViewById(R.id.fragment_newspecial_video_player_title);
        mFullScreenImage = view.findViewById(R.id.fragment_newspecial_video_player_full_tip);
        //setVideoFocus(false);
        setVideoPlayerVisibility();
        videoPlayerView.setVideoExitCallback(new VideoExitFullScreenCallBack() {
            @Override
            public void videoEitFullScreen(boolean isLiving) {
                setVideoPlayerTipStatus();
            }
        });
    }

    private void setSelectBg(int centerPosition, boolean isClick) {
        if (isPlayNextProgram) {
            mNewSpecialCenterAdapter.setSelected(centerPosition);
            printLogAndToast("setSelectBg", "title: " + mCenterFocusedData.get(centerPosition).getTitle(), false);
            mVideoPlayerTitle.setText(mCenterData.get(centerPosition).getTitle());
            isPlayNextProgram = false;
        }
        mNewSpecialCenterAdapter.reFreshSecleted(centerPosition, isClick);
        printLogAndToast("setSelectBg", "centerP : " + centerPosition + "  isClick : " + isClick, false);
        mNewSpecialCenterAdapter.notifyDataSetChanged();
        if (oldLeftPosition != -1 && mLeftManager.findViewByPosition(oldLeftPosition) != null) {
            mLeftManager.findViewByPosition(oldLeftPosition).setBackgroundColor(Color.parseColor("#00000000"));
        }
        if (mLeftManager.findViewByPosition(leftPosition) != null) {
            mLeftManager.findViewByPosition(leftPosition).setBackgroundResource(R.drawable.xuanhong);
        } else {
            printLogAndToast("setSelectBg", "ywy setSelectBg is null", false);
        }
        oldLeftPosition = leftPosition;
        oldCenterPosition = centerPosition;
        printLogAndToast("setSelectBg", "leftPosition : " + leftPosition, false);
    }

    private void setVideoFocusedPlay(int index) {
        printLogAndToast("setVideoFocusedPlay", "index : " + index, false);
        if (videoPlayerView != null) {
            videoPlayerView.beginChange();
        }
        if (mCenterData != null && mCenterData.get(index) != null) {
            if (videoPlayerView != null && mCacheSubContents.get(leftPosition + uuidTag) != null) {
                Log.d("NewTVLauncherPlayerView", "2  content : " + mCacheSubContents.get(leftPosition + uuidTag).toString());
                videoPlayerView.setSeriesInfo(mCacheSubContents.get(leftPosition + uuidTag));
                videoPlayerView.playSingleOrSeries(index, 0);
            }
        } else {
            if (videoPlayerView != null) {
                videoPlayerView.showProgramError();
            }
        }
    }

    private void getCenterData(final int position, String contentID, final Boolean isPlayNextProgram) {
        // 从服务端去数据
        printLogAndToast("getCenterData", "AppKey : " + Libs.get().getAppKey() + "ChannelId : " + Libs.get().getChannelId() + "contentUUID : " + contentID, false);
        if (!TextUtils.isEmpty(Libs.get().getAppKey()) && !TextUtils.isEmpty(Libs.get().getChannelId())
                && !TextUtils.isEmpty(contentID)) {
            leftPosition = position;
            uuidTag = contentID;
            printLogAndToast("getCenterData", "uuidTag : " + uuidTag, false);
            if (null != mCacheSubContents && mCacheSubContents.containsKey(leftPosition + contentID)) {
                if (null != mCacheSubContents.get(leftPosition + contentID).getData()) {
                    onSubContentResult(contentID, (ArrayList<SubContent>) mCacheSubContents.get(leftPosition + contentID).getData());
                } else {
                    getContent(contentID, mLeftData.get(position));
                }
                if (isPlayNextProgram) {
                    setVideoFocusedPlay(0);
                    setSelectBg(0, true);
                }
                return;
            } else {
                printLogAndToast("getCenterData", "else", false);
                getContent(contentID, mLeftData.get(position));
            }
        }
    }

    private void refreshCenterData(int position, Content mContent) {
        if (mContent != null) {
            if (mContent.getData() != null) {
                printLogAndToast("refreshCenterData", "refreshCenterData", false);
                mCenterData = mContent.getData();
                mNewSpecialCenterAdapter.refreshData(position, mCenterData);
                if (isFristPlay) {
                    mLeftFocusedData = mLeftData;
                    mCenterFocusedData = mCenterData;
                    if (mCenterFocusedData.size() > 0) {
                        mVideoPlayerTitle.setText(mCenterFocusedData.get(centerPosition).getTitle());
                    }
                    mSpecialHandler.sendEmptyMessageDelayed(VIDEO_PLAY, 50);
                    isFristPlay = false;
                }
                if (isPlayNextProgram) {
                    setPageNameAndTitle();
                    mSpecialHandler.sendEmptyMessageDelayed(SELECT_DEFAULT_ITEM, 50);
                }
                refreshCenterPosition();
            } else {
                printLogAndToast("refreshCenterData  5  ", "ywy center_refresh_data Programs is null", false);
            }
        } else {
            printLogAndToast("refreshCenterData  6  ", "ywy center_refresh_data is null", false);
        }
    }

    private void setVideoFocus(boolean isFocus) {
        if (isFocus) {
            mFocusViewVideo.requestFocus();
            setVideoPlayerTipStatus();
        } else {
            mVideoPlayerTitle.setVisibility(View.GONE);
            mFullScreenImage.setVisibility(View.GONE);
        }
    }

    @SuppressLint("CheckResult")
    private void playNextProgram() {
        isVideoEndObservable = RxBus.get().register(Constant.IS_VIDEO_END);
        isVideoEndObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (aBoolean) {
                            printLogAndToast("playNextProgram", "playVod next program ", false);
                            isPlayNextProgram = aBoolean;
                            centerPosition = 0;
                            leftPosition += 1;
                            getCenterData(leftPosition, mLeftData.get(leftPosition).getL_id(), aBoolean);
                        }
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void setPlayNextVideo() {
        if (null != videoPlayerView) {
            videoPlayerView.addListener(new IPlayProgramsCallBackEvent() {
                @Override
                public void onNext(SubContent info, int index, boolean isNext) {
                    if (isNext) {
                        printLogAndToast("setPlayNextVideo", "info : " + info.toString() + "   index : " + index, false);

                        centerPosition = index;
                        oldCenterPosition = index;
                        mCenterMenu.scrollToPosition(centerPosition);

                        if (mLeftFocusedData != null && mLeftFocusedData.size() > 0) {
                            setPageNameAndTitle();
                        } else {
                        }
                        if (info != null) {
                            mVideoPlayerTitle.setText(info.getTitle());
                        }
                        if (centerPosition < mCenterData.size()) {
                            mNewSpecialCenterAdapter.setSelected(centerPosition);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onSubContentResult(@NotNull String uuid, @org.jetbrains.annotations.Nullable ArrayList<SubContent> result) {
        printLogAndToast("onSubContentResult", "refresh center uuid : " + uuid + " result : " + result, false);
        if (null != result) {
            mCenterData = result;
            mNewSpecialCenterAdapter.refreshData(leftPosition, mCenterData);
            if (isFristPlay) {
                mLeftFocusedData = mLeftData;
                mCenterFocusedData = mCenterData;
                if (mCenterFocusedData.size() > 0) {
                    mVideoPlayerTitle.setText(mCenterFocusedData.get(centerPosition).getTitle());
                }
                mSpecialHandler.sendEmptyMessageDelayed(VIDEO_PLAY, 50);
                isFristPlay = false;
            }
            if (isPlayNextProgram) {
                setPageNameAndTitle();
                mSpecialHandler.sendEmptyMessageDelayed(SELECT_DEFAULT_ITEM, 50);
            }
            refreshCenterPosition();
        } else {
            printLogAndToast("refreshCenterData  5  ", "ywy center_refresh_data Programs is null", false);
        }
    }

    @SuppressLint("CheckResult")
    private void setVideoPlayerTipStatus() {
        if (null != videoPlayerView) {
            if (isHaveAD) {
                if (mVideoPlayerTitle != null && mFullScreenImage != null) {
                    mVideoPlayerTitle.setVisibility(View.GONE);
                    mFullScreenImage.setVisibility(View.GONE);
                }
            } else {
                if (mVideoPlayerTitle != null && mFullScreenImage != null) {
                    mFocusViewVideo.removeView(mVideoPlayerTitle);
                    mFocusViewVideo.addView(mVideoPlayerTitle);
                    mVideoPlayerTitle.setVisibility(View.VISIBLE);
                    mFullScreenImage.setVisibility(View.GONE);
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private void setVideoPlayerVisibility() {
        if (null != videoPlayerView) {
            isHaveAdObservable = RxBus.get().register(Constant.IS_HAVE_AD);
            isHaveAdObservable.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean isHaveAd) {
                            Log.d(TAG, "setVideoPlayerVisibility isHaveAd : " + isHaveAd);
                            isHaveAD = isHaveAd;
                            if (isHaveAd) {
                                if (mVideoPlayerTitle != null && mFullScreenImage != null) {
                                    mVideoPlayerTitle.setVisibility(View.GONE);
                                    mFullScreenImage.setVisibility(View.GONE);
                                }
                            } else {
                                if (mVideoPlayerTitle != null && mFullScreenImage != null) {
                                    mVideoPlayerTitle.setVisibility(View.VISIBLE);
                                    mFullScreenImage.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
        }
    }

    private void refreshCenterPosition() {
        printLogAndToast("refreshCenterPosition", "refreshCenterPosition", false);
        mSpecialHandler.sendEmptyMessageDelayed(MSG_REFRESH_CENTER_DATA, 50);
    }

    private void printLogAndToast(String method, String content, boolean showToast) {
        if (showToast) {
            Toast.makeText(LauncherApplication.AppContext, method + " _ " + content, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, content);
    }
}
