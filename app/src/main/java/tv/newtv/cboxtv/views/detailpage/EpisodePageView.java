package tv.newtv.cboxtv.views.detailpage;

import android.content.Context;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.MainLooper;
import tv.newtv.cboxtv.cms.details.model.ProgramSeriesInfo;
import tv.newtv.cboxtv.cms.mainPage.AiyaRecyclerView;
import tv.newtv.cboxtv.cms.util.LogUtils;
import tv.newtv.cboxtv.views.CurrentPlayImageView;


/**
 * 项目名称:         CBoxTV
 * 包名:            tv.newtv.cboxtv.views
 * 创建事件:         13:02
 * 创建人:           weihaichao
 * 创建日期:          2018/5/3
 */
public class EpisodePageView extends RelativeLayout implements IEpisode, EpisodeChange {
    private static final String INFO_TEXT_TAG = "info_text";
    private ResizeViewPager ListPager;
    private FragmentManager mFragmentManager;
    private String mContentUUID;
    private AiyaRecyclerView aiyaRecyclerView;
    private EpisodePageAdapter pageItemAdapter;
    private OnEpisodeChange mOnEpisodeChange;
    private int currentIndex = 0;
    private List<EpisodeFragment> fragments;
    private TextView leftDir, rightDir;
    private CurrentPlayImageView mCurrentPlayImage;
    private View mControlView;
    private boolean move = true;
    private LinearLayoutManager mLinearLayoutManager;
    private View TitleView;
    private Disposable mDisposable;

    private ProgramSeriesInfo mSeriesInfo;

    public EpisodePageView(Context context) {
        super(context);
    }

    public EpisodePageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initalize(context);
    }

    public EpisodePageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initalize(context);
    }

    @Override
    public void destroy() {
        if(mDisposable != null){
            mDisposable.dispose();
            mDisposable = null;
        }
        if (aiyaRecyclerView != null) {
            aiyaRecyclerView = null;
        }
        if (ListPager != null) {
            ListPager.destroy();
            ListPager = null;
        }
        mOnEpisodeChange = null;
        if (pageItemAdapter != null) {
            pageItemAdapter.release();
        }
        pageItemAdapter = null;
        if (fragments != null && fragments.size() > 0) {
            for (EpisodeFragment episodeFragment : fragments) {
                episodeFragment.destroy();
            }
            fragments.clear();
        }
        fragments = null;
        mControlView = null;
        removeAllViews();
    }

    public void setCurrentPlayIndex(final int index) {
        currentIndex = index;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                int page = 0;
                if (index > 7) {
                    page = index / 8;
                }
                int selectIndex = index > 7 ? index % 8 : index;
                if (ListPager != null)
                    ListPager.setCurrentItem(page, selectIndex);
            }
        }, 300);
    }

    public void resetProgramInfo() {
        if (mOnEpisodeChange != null && mSeriesInfo != null) {
            mOnEpisodeChange.onGetProgramSeriesInfo(mSeriesInfo);
        }
    }

    public void setOnEpisodeChange(OnEpisodeChange onEpisodeChange) {
        mOnEpisodeChange = onEpisodeChange;
    }

    private void ShowInfoTextView(String text) {
        TextView infoText = findViewWithTag(INFO_TEXT_TAG);
        if (!TextUtils.isEmpty(text)) {
            if (infoText == null) {
                infoText = new TextView(getContext().getApplicationContext());
                infoText.setTag(INFO_TEXT_TAG);
                infoText.setTextAppearance(getContext(), R.style.ModuleTitleStyle);
                infoText.setText(text);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams
                        .WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                infoText.setLayoutParams(layoutParams);
                addView(infoText, layoutParams);
            } else {
                infoText.setText(text);
            }
        } else {
            if (infoText != null) {
                removeView(infoText);
            }
        }
    }

    private void moveToPosition(int n) {
        //先从RecyclerView的LayoutManager中获取第一项和最后一项的Position
        int firstItem = mLinearLayoutManager.findFirstVisibleItemPosition();
        int lastItem = mLinearLayoutManager.findLastVisibleItemPosition();
        if (aiyaRecyclerView == null) return;
        //然后区分情况
        if (n <= firstItem) {
            //当要置顶的项在当前显示的第一个项的前面时
            aiyaRecyclerView.smoothScrollToPosition(n);
        } else if (n <= lastItem) {
            //当要置顶的项已经在屏幕上显示时
            int top = aiyaRecyclerView.getChildAt(n - firstItem).getLeft();
            aiyaRecyclerView.scrollBy(top, 0);
        } else {
            //当要置顶的项在当前显示的最后一项的后面时
            aiyaRecyclerView.smoothScrollToPosition(n);
            //这里这个变量是用在RecyclerView滚动监听里面的
            move = true;
        }

    }

    private void initalize(Context context) {
        ShowInfoTextView("正在加载...");

        TitleView = LayoutInflater.from(context).inflate(R.layout.episode_header_ad_layout, this,
                false);
        TitleView.setId(R.id.id_detail_title);
        LayoutParams title_layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams
                .WRAP_CONTENT);
        TitleView.setLayoutParams(title_layoutParams);
        addView(TitleView, title_layoutParams);

        TitleView.findViewById(R.id.id_title_icon).setVisibility(View.VISIBLE);
        TextView titleView = TitleView.findViewById(R.id.id_title);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText("播放列表");
        }

        ListPager = new ResizeViewPager(context);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams
                .WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.id_detail_title);
        layoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.height_22px);
        ListPager.setId(R.id.id_viewpager);

        PagerScroller pagerScroller = new PagerScroller(getContext());
        pagerScroller.attachToViewPager(ListPager, 500);
        ListPager.setLayoutParams(layoutParams);
        ListPager.addOnPageChange(new ResizeViewPager.OnPageChange() {
            @Override
            public void onChange(int prePage, int currentPage, int totalPage) {
                if (totalPage == 1) {
                    leftDir.setVisibility(View.GONE);
                    rightDir.setVisibility(View.GONE);
                    return;
                }
                if (currentPage == 0) {
                    leftDir.setVisibility(View.GONE);
                    rightDir.setVisibility(View.VISIBLE);
                } else if (currentPage == ListPager.getAdapter().getCount() - 1) {
                    rightDir.setVisibility(View.GONE);
                    leftDir.setVisibility(View.VISIBLE);
                } else {
                    leftDir.setVisibility(View.VISIBLE);
                    rightDir.setVisibility(View.VISIBLE);
                }
            }
        });
        ListPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {
                ListPager.requestLayout();
            }

            @Override
            public void onPageSelected(final int position) {
                if (pageItemAdapter.getSelectedIndex() == position) return;
                pageItemAdapter.setSelectedIndex(position);
                aiyaRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (move) {
                                move = false;
                                moveToPosition(position);
                            }


                        }

                    }
                });

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {

                }
            }
        });
        addView(ListPager, layoutParams);

        leftDir = new TextView(getContext());
        int dir_width = getResources().getDimensionPixelOffset(R.dimen.width_25px);
        int dir_height = getResources().getDimensionPixelOffset(R.dimen.width_41px);
        LayoutParams left_layoutParam = new LayoutParams(dir_width, dir_height);
        //  left_layoutParam.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        left_layoutParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            left_layoutParam.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        }
        left_layoutParam.leftMargin = dir_width;
//        left_layoutParam.topMargin = getResources().getDimensionPixelOffset(R.dimen.height_300px);
        leftDir.setBackgroundResource(R.drawable.icon_detail_tips_left);
        leftDir.setLayoutParams(left_layoutParam);
        addView(leftDir, left_layoutParam);
        leftDir.setVisibility(View.INVISIBLE);
        rightDir = new TextView(getContext());
        LayoutParams right_layoutParam = new LayoutParams(dir_width, dir_height);
        // right_layoutParam.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        right_layoutParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            right_layoutParam.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        }
        right_layoutParam.rightMargin = dir_width;
//        right_layoutParam.topMargin = getResources().getDimensionPixelOffset(R.dimen
// .height_300px);
        rightDir.setBackgroundResource(R.drawable.icon_detail_tips_right);
        rightDir.setLayoutParams(right_layoutParam);
        addView(rightDir, right_layoutParam);
        rightDir.setVisibility(View.INVISIBLE);

        aiyaRecyclerView = new AiyaRecyclerView(context, false);
        int height = context.getResources().getDimensionPixelOffset(R.dimen.height_70px);
        LayoutParams aiyaLayoutParam = new LayoutParams(LayoutParams.MATCH_PARENT, height);
        mLinearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager
                .HORIZONTAL, false);
        aiyaRecyclerView.setLayoutManager(mLinearLayoutManager);
        aiyaRecyclerView.setAlign(AiyaRecyclerView.ALIGN_CENTER);
        aiyaRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) aiyaRecyclerView.getItemAnimator()).setSupportsChangeAnimations
                (false);
        int margin = context.getResources().getDimensionPixelOffset(R.dimen.width_120px);
        aiyaLayoutParam.addRule(RelativeLayout.BELOW, R.id.id_viewpager);
        aiyaLayoutParam.leftMargin = margin;
        aiyaLayoutParam.rightMargin = margin;
        aiyaRecyclerView.setLayoutParams(aiyaLayoutParam);
        addView(aiyaRecyclerView, aiyaLayoutParam);

        pageItemAdapter = new EpisodePageAdapter();
        pageItemAdapter.setOnItemClick(new EpisodePageAdapter.OnItemClick() {
            @Override
            public void onClick(int position, View view) {
                ListPager.setCurrentItem(position);
                aiyaRecyclerView.setFocusView(view);
            }
        });
        aiyaRecyclerView.setAdapter(pageItemAdapter);
    }

    public void setContentUUID(int EpisodeType, FragmentManager manager, String uuid, View
            controlView) {
        mFragmentManager = manager;
        mContentUUID = uuid;
        mControlView = controlView;

        String leftUUID = uuid.substring(0, 2);
        String rightUUID = uuid.substring(uuid.length() - 2, uuid.length());

        Observable<ResponseBody> observable = EpisodeHelper.GetInterface(EpisodeType, leftUUID,
                rightUUID,
                uuid);
        if (observable != null) {
            observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            mDisposable = d;
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            if (responseBody != null) {
                                try {
                                    parseResult(responseBody.string());
                                } catch (IOException e) {
                                    LogUtils.e(e.toString());
                                    onLoadError(e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            onLoadError(e.getMessage());
                        }

                        @Override
                        public void onComplete() {
                            mDisposable = null;
                        }
                    });
        } else {
            onLoadError("接口调用失败，暂时没有定义");
        }
    }

    private void onLoadError(String message) {
        ShowInfoTextView(message);
    }

    private void parseResult(String result) {
        if (TextUtils.isEmpty(result)) {
            onLoadError("获取结果为空");
            return;
        }
        try {
            JSONObject object = new JSONObject(result);
            if (object.getInt("errorCode") == 0) {
                JSONObject obj = object.getJSONObject("data");
                Gson gson = new Gson();
                mSeriesInfo = gson.fromJson(obj.toString(), ProgramSeriesInfo.class);

                if (mOnEpisodeChange != null) {
                    mOnEpisodeChange.onGetProgramSeriesInfo(mSeriesInfo);
                }

                if (mSeriesInfo.getData() != null && mSeriesInfo.getData().size() > 0) {
                    if (mControlView != null) {
                        mControlView.setVisibility(View.VISIBLE);
                    }
                    setVisibility(View.VISIBLE);
                    ShowInfoTextView("");

                    if (mSeriesInfo.getData().size() > 0) {
                        rightDir.setVisibility(View.VISIBLE);
                    }

                    List<ProgramSeriesInfo.ProgramsInfo> results = mSeriesInfo.getData();
                    int size = mSeriesInfo.getData().size();
                    fragments = new ArrayList<>();
                    List<EpisodePageAdapter.PageItem> pageItems = new ArrayList<>();
                    for (int index = 0; index < size; index += 8) {
                        int endIndex = index + 8;
                        if (endIndex > size) {
                            endIndex = size;
                        }
                        EpisodeFragment episodeFragment = new EpisodeFragment();
                        episodeFragment.setData(results.subList(index, endIndex));
                        episodeFragment.setViewPager(ListPager, fragments.size(), this);
                        fragments.add(episodeFragment);
                        pageItems.add(new EpisodePageAdapter.PageItem(String
                                .format(Locale.getDefault(), "%d-%d", index + 1, endIndex)));
                    }
                    EpisodeAdapter episodeAdapter = new EpisodeAdapter(mFragmentManager, fragments);
                    ListPager.setAdapter(episodeAdapter);
                    pageItemAdapter.setPageData(pageItems, aiyaRecyclerView).notifyDataSetChanged();
                    //ListPager.resetHeight(0);
                } else {
                    onLoadError("暂时没有数据");
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
            onLoadError(e.getMessage());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;

        if (TitleView != null) {
            LayoutParams layoutParams = (LayoutParams) TitleView.getLayoutParams();
            TitleView.measure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height += TitleView.getMeasuredHeight() + layoutParams.topMargin + layoutParams
                    .bottomMargin;
        }

        if (ListPager != null) {
            LayoutParams layoutParams = (LayoutParams) ListPager.getLayoutParams();
            ListPager.measure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height += ListPager.getMeasuredHeight() + layoutParams.topMargin + layoutParams
                    .bottomMargin;

            if (leftDir != null) {
                LayoutParams leftParams = (LayoutParams) leftDir.getLayoutParams();
                leftParams.topMargin = ListPager.getTop() + (ListPager.getMeasuredHeight() - leftDir
                        .getMeasuredHeight()) / 2;
                leftDir.setLayoutParams(leftParams);
            }

            if (rightDir != null) {
                LayoutParams rightParams = (LayoutParams) rightDir.getLayoutParams();
                rightParams.topMargin = ListPager.getTop() + (ListPager.getMeasuredHeight() - rightDir
                        .getMeasuredHeight()) / 2;
                rightDir.setLayoutParams(rightParams);
            }
        }
        if (aiyaRecyclerView != null) {
            LayoutParams layoutParams = (LayoutParams) aiyaRecyclerView.getLayoutParams();
            aiyaRecyclerView.measure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height += aiyaRecyclerView.getMeasuredHeight() + layoutParams.topMargin +
                    layoutParams.bottomMargin;
        }

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);


    }

    @Override
    public String getContentUUID() {
        return mContentUUID;
    }

    @Override
    public boolean interuptKeyEvent(KeyEvent event) {
        if (ListPager.getChildCount() == 0) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (aiyaRecyclerView.hasFocus()) {
                    return false;
                }
                if (!ListPager.hasFocus() && !aiyaRecyclerView.hasFocus()) {
                    ListPager.requestDefaultFocus();
                    return true;
                }
                View view = FocusFinder.getInstance().findNextFocus(ListPager, ListPager.
                        findFocus(), View.FOCUS_DOWN);
                if (view == null) {
                    final LinearLayoutManager layoutManager = (LinearLayoutManager) aiyaRecyclerView
                            .getLayoutManager();
                    int first = layoutManager.findFirstVisibleItemPosition();
                    int last = layoutManager.findLastVisibleItemPosition();
                    final int select = ListPager.getCurrentItem();
                    if (select >= first && select <= last) {
                        aiyaRecyclerView.getChildAt(select - first).requestFocus();
                    } else {
                        /**
                         * aiyaRecyclerView中的对应item未显示出来，需要先滚动到select位置，在获取焦点
                         */
                        aiyaRecyclerView.scrollToPosition(select);
                        MainLooper.get().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int first = layoutManager.findFirstVisibleItemPosition();
                                aiyaRecyclerView.getChildAt(select - first).requestFocus();
                            }
                        }, 50);

                    }
                    return true;
                } else {
                    view.requestFocus();
                    return true;
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                if (!ListPager.hasFocus() && !aiyaRecyclerView.hasFocus()) {
                    if (aiyaRecyclerView.getDefaultFocusView() != null)
                        aiyaRecyclerView.getDefaultFocusView().requestFocus();
                    return true;
                }
                View view = FocusFinder.getInstance().findNextFocus(this, findFocus(),
                        View.FOCUS_UP);
                if (view != null) {
                    view.requestFocus();
                    return true;
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                View focusView = null;
                ViewGroup parentView = null;
                if (aiyaRecyclerView.hasFocus()) {
                    focusView = aiyaRecyclerView.findFocus();
                    parentView = aiyaRecyclerView;
                } else if (ListPager.hasFocus()) {
                    focusView = ListPager.findFocus();
                    parentView = ListPager;
                }

                View target = FocusFinder.getInstance().findNextFocus(parentView, focusView, View
                        .FOCUS_RIGHT);
                if (target != null) {
                    target.requestFocus();
                } else {
                    if (ListPager.hasFocus()) {
                        if (ListPager.getCurrentItem() < ListPager.getAdapter().getCount() - 1) {
                            ListPager.setCurrentItem(ListPager.getCurrentItem() + 1);
                        }
                        return true;
                    }
                }
                return true;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                View focusView = null;
                ViewGroup parentView = null;
                if (aiyaRecyclerView.hasFocus()) {
                    focusView = aiyaRecyclerView.findFocus();
                    parentView = aiyaRecyclerView;
                } else if (ListPager.hasFocus()) {
                    focusView = ListPager.findFocus();
                    parentView = ListPager;
                }

                View target = FocusFinder.getInstance().findNextFocus(parentView, focusView, View
                        .FOCUS_LEFT);
                if (target != null) {
                    target.requestFocus();
                } else {
                    if (ListPager.hasFocus()) {
                        if (ListPager.getCurrentItem() > 0) {
                            ListPager.setCurrentItem(ListPager.getCurrentItem() - 1);
                        }
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onChange(CurrentPlayImageView imageView, int index) {
        if (mCurrentPlayImage != null) {
            mCurrentPlayImage.setIsPlaying(false);
        }
        currentIndex = index;
        mCurrentPlayImage = imageView;
        imageView.setIsPlaying(true);
        if (mOnEpisodeChange != null) {
            mOnEpisodeChange.onChange(index);
        }
    }


    public interface OnEpisodeChange {
        void onGetProgramSeriesInfo(ProgramSeriesInfo seriesInfo);

        void onChange(int index);
    }


}
