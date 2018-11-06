package tv.newtv.cboxtv.cms.special.doubleList.fragment;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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
import com.newtv.libs.util.GsonUtil;
import com.newtv.libs.util.LogUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import tv.newtv.cboxtv.LauncherApplication;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.mainPage.model.ModuleInfoResult;
import tv.newtv.cboxtv.cms.net.NetClient;
import tv.newtv.cboxtv.cms.special.doubleList.adapter.NewSpecialCenterAdapter;
import tv.newtv.cboxtv.cms.special.doubleList.adapter.NewSpecialLeftAdapter;
import tv.newtv.cboxtv.cms.special.doubleList.bean.SpecialBean;
import tv.newtv.cboxtv.cms.special.doubleList.view.FocusRecyclerView;
import tv.newtv.cboxtv.cms.special.fragment.BaseSpecialContentFragment;
import tv.newtv.cboxtv.cms.util.ModuleUtils;
import tv.newtv.cboxtv.player.videoview.PlayerCallback;
import tv.newtv.cboxtv.player.videoview.VideoExitFullScreenCallBack;
import tv.newtv.cboxtv.player.videoview.VideoPlayerView;

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
    private List<SpecialBean.DataBean.ProgramsBean> mCenterData = new ArrayList<>();
    private List<SpecialBean.DataBean.ProgramsBean> mCenterFocusedData = new ArrayList<>();
    private boolean isFristPlay = true, isRightToLeft = false, isMoveKey = false;
    private int leftPosition = 0, oldLeftPosition = -1, centerPosition = 0;
    private String mDefaultContentUUID;
    private View focusView;
    private String mMoveTag = DOWN;
    private int currentIndex = 0;
    private ModelResult<ArrayList<Page>> mModuleInfoResult;
    private String mLeftContentId;
    private SpecialBean mSpecialBean = null;
    private Content mProgramSeriesInfo;
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
                            sendEmptyMessageDelayed(SELECT_DEFAULT_ITEM, 400);
                        } else {
                            printLogAndToast("Handler", "ywy video playurl is null", false);
                        }
                        break;
                    case SELECT_DEFAULT_ITEM:
                        setSelectBg(0, false);
                        break;
                    case VIDEO_NEXT_PLAY:
                        if (mLeftFocusedData != null && mLeftFocusedData.size() > 0) {
                            mSpecialTopicName.setText(mLeftFocusedData.get(leftPosition).getTitle());
                            mSpecialTopicTitle.setText(mLeftFocusedData.get(leftPosition).getSubTitle());
                        } else {
                            if (mLeftData != null) {
                                printLogAndToast("Handler", "video next play data: " + mLeftData.toString(), false);
                            } else {
                                printLogAndToast("Handler", "video next play data is null ", false);
                            }
                        }
                        if (mCenterFocusedData != null && mCenterFocusedData.size() > 0) {
                            mVideoPlayerTitle.setText(mCenterFocusedData.get(centerPosition).getTitle());
                        }
                        setVideoFocusedPlay(centerPosition);
                        mNewSpecialCenterAdapter.setSelected(centerPosition);
                        break;
                    case LEFT_SCROLL_POSITION:
                        if (mLeftMenu != null && mLeftMenu.getChildAt(leftPosition) != null) {
                            mLeftMenu.getChildAt(leftPosition).setFocusable(true);
                            mLeftMenu.getChildAt(leftPosition).requestFocus();
                        } else {
                            printLogAndToast("left_scroll_position", "position is null", true);
                        }
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
    protected void onItemContentResult(Content content) {

    }

    @Override
    protected void setUpUI(View view) {
        initTitle(view);
        initLeftList(view);
        initCenterList(view);
        initVideo(view);
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
                    mDefaultContentUUID = mLeftData.get(i).getContentId();
                    printLogAndToast("setLeftDefaultFocusedP", "default leftPosition : " + leftPosition + "  mDefaultContentUUID : " + mDefaultContentUUID, false);
                } else {
                    mLiftListData.add(mLeftData.get(i));
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
        printLogAndToast("onPlayerClick", "EnterFullScreen", false);
        videoPlayerView.EnterFullScreen(getActivity(), false);
        mVideoPlayerTitle.setVisibility(View.GONE);
        mFullScreenImage.setVisibility(View.GONE);
    }

    @Override
    public void AllPlayComplete(boolean isError, String info, VideoPlayerView videoPlayerView) {
        printLogAndToast("AllPalyComplete  ", "isError : " + isError + " info : " + info + "  centerPosition : " + centerPosition, false);
        if (mCenterFocusedData.size() - 1 <= centerPosition) {
            if (mVideoPlayerTitle != null) {
                mVideoPlayerTitle.setVisibility(View.GONE);
            }
            return;
        } else {
            centerPosition++;
            mCenterMenu.smoothScrollToPosition(centerPosition);
            mSpecialHandler.sendEmptyMessageDelayed(VIDEO_NEXT_PLAY, 50);
        }
    }

    @Override
    public void ProgramChange() {
        printLogAndToast("ProgramChange", "ProgramChange", false);
        videoPlayerView.setSeriesInfo(mProgramSeriesInfo);
        videoPlayerView.playSingleOrSeries(currentIndex, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        /*if (leftPosition > 0) {
            mLeftMenu.getChildAt(leftPosition).requestFocus();
        }
        if (centerPosition > 0) {
            mCenterMenu.getChildAt(centerPosition).requestFocus();
        }*/
    }

    @Override
    public void onStop() {
        super.onStop();
        printLogAndToast("onStop", "onStop", false);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isMoveKey = false;
        mDefaultContentUUID = null;
        oldLeftPosition = 0;
        leftPosition = -1;
        centerPosition = 0;
        mLeftData.clear();
        mLeftFocusedData.clear();
        mCenterData.clear();
        mCenterFocusedData.clear();
        mNewSpecialCenterAdapter.clearList();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        printLogAndToast("onDetach", "onDetach", false);
        isFristPlay = true;
        videoPlayerView = null;
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
            }

            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                mMoveTag = DOWN;
                isMoveKey = true;
            }
            printLogAndToast("dispatchKeyEvent", "isInstanceof : " + (focusView instanceof VideoPlayerView) + " code : " + event.getKeyCode(), false);
            printLogAndToast("dispatchKeyEvent", "view  is  : " + focusView.getClass(), false);
            if (focusView instanceof VideoPlayerView) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        mVideoPlayerTitle.setVisibility(View.GONE);
                        mFullScreenImage.setVisibility(View.GONE);
                        mCenterMenu.smoothScrollToPosition(centerPosition);
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
        if (mLeftData != null && mLeftData.size() > 0) {
            mSpecialTopicName.setText(mLeftData.get(leftPosition).getTitle());
        }

        if (mModuleInfoResult != null) {
            mSpecialTopicTitle.setText(mModuleInfoResult.getDescription());
            String url = mModuleInfoResult.getBackground();
            if (TextUtils.isEmpty(url)) {
                mNewSpecialLayout.setBackgroundResource(R.drawable.new_special_bg);
            }
        }

    }

    private void setLeftUpVisible(String tag, int leftFocusPt) {
        if (null != mLeftUp) {
            if (!TextUtils.isEmpty(tag) && tag.equals(UP)) {
                if (leftFocusPt > MIN_DIS_POSTION) {
                    mLeftUp.setVisibility(View.VISIBLE);
                } else {
                    mLeftUp.setVisibility(View.INVISIBLE);
                }
            } else if (!TextUtils.isEmpty(tag) && tag.equals(DOWN)) {
                if (leftFocusPt > MAX_DIS_POSTION) {
                    mLeftUp.setVisibility(View.VISIBLE);
                } else {
                    mLeftUp.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void setLeftDownVisible(String tag, int leftFocusPt) {
        if (!TextUtils.isEmpty(tag) && tag.equals(DOWN)) {
            if ((mLeftData.size() > MAX_DIS_POSTION) && (leftFocusPt == mLeftData.size() - 1)) {
                mLeftDown.setVisibility(View.INVISIBLE);
            } else if ((mLeftData.size() > MAX_DIS_POSTION)) {
                mLeftDown.setVisibility(View.VISIBLE);
            }
        } else if ((!TextUtils.isEmpty(tag) && tag.equals(UP)) && leftFocusPt < mLeftData.size() - 1 - MAX_DIS_POSTION) {
            mLeftDown.setVisibility(View.VISIBLE);
        } else {
            mLeftDown.setVisibility(View.INVISIBLE);
        }
    }

    private void initLeftDownStatus() {
        if (null != mLeftData && mLeftData.size() > MAX_DIS_POSTION) {
            mLeftDown.setVisibility(View.VISIBLE);
        } else {
            mLeftDown.setVisibility(View.INVISIBLE);
        }
    }

    private void initLeftList(View view) {
        printLogAndToast("initLeftList", "ywy : initLeftList", false);
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
                    getCenterData(position, mLeftContentId);
                } else {
                    isRightToLeft = false;
                }
                setLeftUpVisible(mMoveTag, position);
                setLeftDownVisible(mMoveTag, position);
            }
        });
        mLeftMenu.setAdapter(mNewSpecialLeftAdapter);
        mLeftMenu.setOnGetPositionListener(new FocusRecyclerView.OnGetPositionListener() {
            @Override
            public void GetRightPositionListener(int position) {
                printLogAndToast("initLeftList", "ywy left position right : " + position + " centerPosition : " + centerPosition, false);
                leftPosition = position;
                int firstVP = mCenterManager.findFirstVisibleItemPosition();
                int lastVP = mCenterManager.findLastVisibleItemPosition();
                Message msg = Message.obtain();
                msg.what = LEFT_TO_CENTER_POSITION;
                if (position == oldLeftPosition) {
                    mCenterMenu.smoothScrollToPosition(centerPosition);
                    msg.arg1 = centerPosition;
                    mSpecialHandler.sendMessage(msg);
                } else {
                    centerPosition = 0;
                    if (centerPosition < firstVP) {
                        mCenterMenu.smoothScrollToPosition(centerPosition);
                    } else if (centerPosition > firstVP && centerPosition < lastVP) {
                        int top = mCenterMenu.getChildAt(centerPosition).getTop();
                        mCenterMenu.smoothScrollBy(0, top);
                    } else {
                        mCenterMenu.smoothScrollToPosition(centerPosition);
                    }
                    msg.arg1 = centerPosition;
                    mSpecialHandler.sendMessageDelayed(msg, 200);
                }

            }

            @Override
            public void GetLeftPositionListener(int position) {
                printLogAndToast("initLeftList", "ywy left position left : " + position, false);
            }
        });
        mSpecialHandler.sendEmptyMessageDelayed(LEFT_SCROLL_POSITION, 50);
        initLeftDownStatus();
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
            if ((mCenterData.size() > MAX_DIS_POSTION) && (centerFocusPt == mCenterData.size() - 1)) {
                mCenterDown.setVisibility(View.INVISIBLE);
            } else if ((mLeftData.size() > MAX_DIS_POSTION)) {
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
        printLogAndToast("initCenterList", "ywy : initCenterList", false);

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
                printLogAndToast("initCenterList", "center get   right", false);
                setVideoFocus(true);
                if (centerPosition == -1) {
                    mVideoPlayerTitle.setText(mCenterFocusedData.get(0).getTitle());
                } else {
                    mVideoPlayerTitle.setText(mCenterFocusedData.get(centerPosition).getTitle());
                }
            }

            @Override
            public void GetLeftPositionListener(int position) {
                printLogAndToast("initCenterList", "center get   left leftPosition : " + leftPosition + "  position: " + position, false);
                isRightToLeft = true;
                //setVideoFocus(false);
                printLogAndToast("initCenterList", "ywy first : " + (mCenterManager.findFirstVisibleItemPosition() > leftPosition) + "  last " + (mCenterManager.findLastVisibleItemPosition() < leftPosition), false);
                //mLeftMenu.smoothScrollToPosition(leftPosition);
                int firstVP = mLeftManager.findFirstVisibleItemPosition();
                int lastVP = mLeftManager.findLastVisibleItemPosition();
                /*if (leftPosition < firstVP) {
                    mLeftMenu.smoothScrollToPosition(leftPosition);
                } else if (leftPosition > firstVP && leftPosition < lastVP) {
                    int top = mCenterMenu.getChildAt(leftPosition).getTop();
                    mLeftMenu.smoothScrollBy(0, top);
                } else {
                    mLeftMenu.smoothScrollToPosition(leftPosition);
                }*/
                printLogAndToast("initLeftList ", "leftPosition : " + leftPosition, false);
                if (mLeftMenu.getChildAt(leftPosition - firstVP) != null) {
                    mLeftMenu.getChildAt(leftPosition - firstVP).requestFocus();
                } else {
                    printLogAndToast(TAG, "ywy GetLeftPositionListener is null", false);
                }
            }
        });
        mNewSpecialCenterAdapter.setOnVideoChangeListener(new NewSpecialCenterAdapter.OnVideoChangeListener() {
            @Override
            public void onVideoChangeListener(String title, int position) {
                centerPosition = position;
                mLeftFocusedData = mLeftData;
                mCenterFocusedData = mCenterData;
                if (mCenterData.get(position) != null) {
                    mSpecialTopicName.setText(mLeftFocusedData.get(leftPosition).getTitle());
                    mSpecialTopicTitle.setText(mLeftFocusedData.get(leftPosition).getSubTitle());
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
        videoPlayerView.setVideoExitCallback(new VideoExitFullScreenCallBack() {
            @Override
            public void videoEitFullScreen() {
                if (mVideoPlayerTitle != null && mFullScreenImage != null) {
                    mVideoPlayerTitle.setVisibility(View.VISIBLE);
                    mFullScreenImage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setSelectBg(int centerPosition, boolean isClick) {
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
        printLogAndToast("setSelectBg", "leftPosition : " + leftPosition, false);
    }

    private void setVideoFocusedPlay(int index) {
        if (videoPlayerView != null) {
            videoPlayerView.beginChange();
        }
        if (mProgramSeriesInfo != null) {
            if (videoPlayerView != null) {
                videoPlayerView.setSeriesInfo(mProgramSeriesInfo);
                videoPlayerView.playSingleOrSeries(index, 0);
            }
        } else {
            if (videoPlayerView != null) {
                videoPlayerView.showProgramError();
            }
        }
    }

    private void getCenterData(final int position, String contentUUID) {

        if (contentUUID.length() > 2) {
            printLogAndToast("getCenterData", "left : " + contentUUID.substring(0, 2) + "  right : " + contentUUID.substring(contentUUID.length() - 2, contentUUID.length()) + "  all : " + contentUUID, false);
        } else {
            printLogAndToast("getCenterData", "contentUUID length < 2 ", false);
        }

        // 从服务端去数据
        NetClient.INSTANCE.getSpecialApi().getDoublePageData(Libs.get().getAppKey(), Libs.get().getChannelId(),
                contentUUID.substring(0, 2), contentUUID.substring(contentUUID.length() - 2, contentUUID.length()),
                contentUUID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(ResponseBody value) {
                        try {
                            String result = value.string();
                            try {
                                JSONObject jsonObject = new JSONObject(result);
                                String data = jsonObject.getString("data");
                                mProgramSeriesInfo = GsonUtil.fromjson(data, Content.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mSpecialBean = ModuleUtils.getInstance()
                                    .parseJsonForNewSpecialSecondData(result);
                            printLogAndToast("getCenterData", "bean : " + mSpecialBean.toString() + " value : " + value.string(), false);

                            refreshCenterData(position, mSpecialBean);
                            initCenterDownStatus();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogUtils.e(e.toString());
                        printLogAndToast("getCenterData onError", "ywy Throwable: " + e.toString(), false);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void refreshCenterData(int position, SpecialBean mSpecialBean) {
        printLogAndToast("refreshCenterData", "case CENTER_REFRESH_DATA", false);
        if (null != mSpecialBean) {
            if (mSpecialBean.getData() != null) {
                printLogAndToast("refreshCenterData  2  ", "case msg.obj Center_Refresh_data : " + mSpecialBean.getData().toString(), false);
            }
        } else {
            printLogAndToast("refreshCenterData  4  ", "case msg.obj Center_Refresh_data is null ", false);
        }
        if (mSpecialBean.getData() != null) {
            if (mSpecialBean.getData().getPrograms() != null) {
                mCenterData = mSpecialBean.getData().getPrograms();
                mNewSpecialCenterAdapter.refreshData(position, mCenterData);
                if (isFristPlay) {
                    mLeftFocusedData = mLeftData;
                    mCenterFocusedData = mCenterData;
                    mSpecialHandler.sendEmptyMessageDelayed(VIDEO_PLAY, 50);
                    isFristPlay = false;
                }
            } else {
                printLogAndToast("refreshCenterData  5  ", "ywy center_refresh_data Programs is null", false);
            }
        } else {
            printLogAndToast("refreshCenterData  6  ", "ywy center_refresh_data is null", false);
        }
    }

    private void setLeftRecyclerFocused(boolean isFocus) {
        mLeftMenu.setFocusable(isFocus);
        if (isFocus) {
            mLeftMenu.requestFocus();
        }
    }

    private void setCenterRecyclerFocused(boolean isFocus) {
        mCenterMenu.setFocusable(isFocus);
        if (isFocus) {
            mCenterMenu.requestFocus();
        }
    }

    private void setVideoFocus(boolean isFocus) {
        /*if (mVideoLinear != null) {
            mVideoLinear.setFocusable(isFocus);
        }
        if (videoPlayerView != null) {
            videoPlayerView.setFocusable(isFocus);
        }*/
        if (isFocus) {
            mFocusViewVideo.requestFocus();
            mVideoPlayerTitle.setVisibility(View.VISIBLE);
            mFullScreenImage.setVisibility(View.VISIBLE);
            //videoPlayerView.setFocusView(mFocusViewVideo, true);
        } else {
            mVideoPlayerTitle.setVisibility(View.GONE);
            mFullScreenImage.setVisibility(View.GONE);
        }
    }

    private void printLogAndToast(String method, String content, boolean showToast) {
        if (showToast) {
            Toast.makeText(LauncherApplication.AppContext, method + " ywy " + content, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, method + " ywy " + content);
    }

}