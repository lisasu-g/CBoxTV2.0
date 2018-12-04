package tv.newtv.cboxtv.views.detail;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.newtv.cms.bean.Alternate;
import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.SubContent;
import com.newtv.cms.contract.AlternateContract;
import com.newtv.cms.contract.ContentContract;
import com.newtv.cms.contract.PersonDetailsConstract;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.views.custom.CurrentPlayImageView;
import tv.newtv.cboxtv.views.custom.RecycleImageView;
import tv.newtv.cboxtv.views.widget.HorizontalRecycleView;
import tv.newtv.cboxtv.views.widget.NewTvRecycleAdapter;
import tv.newtv.cboxtv.views.widget.RecycleFocusItemDecoration;

/**
 * 项目名称:         CBoxTV
 * 包名:            tv.newtv.cboxtv.views.detailpage
 * 创建事件:         13:54
 * 创建人:           weihaichao
 * 创建日期:          2018/7/27
 */
@SuppressWarnings("unchecked")
public class EpisodeHorizontalListView extends RelativeLayout implements IEpisode,
        PersonDetailsConstract.View, ContentContract.View, AlternateContract.View {

    public static final int TYPE_PERSON_HOST_LV = 0;         //主持的栏目列表（人物详情）
    public static final int TYPE_PERSON_RELATION_LV = 1;     //主持相关节目列表（人物详情）
    public static final int TYPE_PROGRAM_SERICE_LV = 2;     //节目合集
    public static final int TYPE_ALTERNATE_LIST = 3;     //节目合集

    public static final int DIRECTION_HORIZONTAL = 1;
    public static final int DIRECTION_VERTICAL = 2;

    private String mContentUuid;
    private HorizontalRecycleView mRecycleView;
    private List<?> mProgramSeriesInfo;
    private TextView mTitleText;
    private NewTvRecycleAdapter mAdapter;
    private onEpisodeItemClick onItemClickListener;

    private View currentFocusView;

    private PersonDetailsConstract.PersonDetailPresenter mPersonPresenter;
    private ContentContract.Presenter mContentPresenter;
    private AlternateContract.Presenter mAlternatePresenter;
    private View controlView;

    private int mHorizontalCount = 4;

    private OnSeriesInfoResult mOnSeriesInfoResult;
    private int item_layout = R.layout.program_horizontal_layout;
    private int place_holder = R.drawable.focus_384_216;
    private int direction = DIRECTION_HORIZONTAL;

    public EpisodeHorizontalListView(Context context) {
        this(context, null);
    }

    public EpisodeHorizontalListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public EpisodeHorizontalListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    public void setOnSeriesInfoResult(OnSeriesInfoResult infoResult) {
        mOnSeriesInfoResult = infoResult;
    }

    @Override
    public void destroy() {
        controlView = null;
        onItemClickListener = null;
        mAdapter = null;

        if (mPersonPresenter != null) {
            mPersonPresenter.destroy();
            mPersonPresenter = null;
        }

        if (mContentPresenter != null) {
            mContentPresenter.destroy();
            mContentPresenter = null;
        }
    }

    public void setOnItemClick(onEpisodeItemClick listener) {
        onItemClickListener = listener;
    }

    public String setTitleByType(int type) {
        switch (type) {
            case TYPE_PERSON_HOST_LV:
                return "TA 的节目";
            case TYPE_PERSON_RELATION_LV:
                return "TA 主持的CCTV+栏目";
            case TYPE_PROGRAM_SERICE_LV:
                return "节目合集";
            case TYPE_ALTERNATE_LIST:
                return "节目单";
            default:
                return "详情列表";
        }
    }

    private void initialize(Context context, AttributeSet attrs) {
        mPersonPresenter = new PersonDetailsConstract.PersonDetailPresenter(getContext(), this);
        mContentPresenter = new ContentContract.ContentPresenter(getContext(), this);
        mAlternatePresenter = new AlternateContract.AlternatePresenter(getContext(), this);
    }

    private void buildUI(List<?> infos, int type) {
        switch (type) {
            case TYPE_PERSON_HOST_LV:
            case TYPE_PERSON_RELATION_LV:
            case TYPE_PROGRAM_SERICE_LV:
            case TYPE_ALTERNATE_LIST:
                buildSubContentListView(infos, type);
                break;
            default:
                break;
        }
    }

    private void buildSubContentListView(List<?> infos, int type) {

        if (mOnSeriesInfoResult != null) {
            mOnSeriesInfoResult.onResult(infos);
        }

        if (infos.size() > 0) {
            mProgramSeriesInfo = infos;
            if (getChildCount() == 0) {
                LayoutInflater.from(getContext()).inflate(R.layout.episode_horizontal_layout, this,
                        true);
                mRecycleView = findViewById(R.id.list_view);
                mRecycleView.setLayoutManager(new LinearLayoutManager(getContext(),
                        LinearLayoutManager
                                .HORIZONTAL, false));
                mRecycleView.setShowCounts(4);
                mRecycleView.addItemDecoration(new RecycleFocusItemDecoration(getResources()
                        .getDimensionPixelOffset(R.dimen.width_48px)));
                mRecycleView.setDirectors(findViewById(R.id.dir_left), findViewById(R.id
                        .dir_right));

                mTitleText = findViewById(R.id.id_title);
                if (mTitleText != null) {
                    mTitleText.setText(setTitleByType(type));
                }

                mAdapter = new NewTvRecycleAdapter<Object, ViewHolder>() {
                    @Override
                    public List<Object> getDatas() {
                        return (List<Object>) mProgramSeriesInfo;
                    }

                    @Override
                    public ViewHolder createHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext()).inflate(item_layout,
                                parent, false);
                        return new ViewHolder(view);
                    }

                    @Override
                    public void bind(Object data, ViewHolder holder, boolean select) {
                        String image = "";
                        String title = "";
                        if (data instanceof SubContent) {
                            if (direction == DIRECTION_HORIZONTAL) {
                                image = ((SubContent) data).getHImage();
                            } else {
                                image = ((SubContent) data).getVImage();
                            }
                            title = ((SubContent) data).getTitle();
                        } else if (data instanceof Alternate) {
                            image = ((Alternate) data).getHImage();
                            title = ((Alternate) data).getTitle();
                        }

                        holder.posterView.placeHolder(place_holder)
                                .errorHolder(place_holder).hasCorner
                                (true).load(image);
                        holder.titleText.setText(title);

                        if (holder.posterView instanceof CurrentPlayImageView) {
                            holder.posterView.setIsPlaying(select);
                        }
                    }

                    @Override
                    public boolean onItemClick(Object data, int position) {
                        if (onItemClickListener != null) {
                            boolean interrupt = onItemClickListener.onItemClick(position, data);
                            if (!interrupt) {
                                mAdapter.setSelectedIndex(position);
                            }
                            return interrupt;
                        }
                        return false;
                    }

                    @Override
                    public void onFocusChange(View view, int position, boolean hasFocus) {
                        if (hasFocus) {
                            currentFocusView = view;
                        }
                    }
                };

                mRecycleView.setAdapter(mAdapter);
            }
            mAdapter.notifyDataSetChanged();
        } else {
            onLoadError();
        }
    }

    private void onLoadError() {

        if (getParent() != null) {
            ViewGroup parentView = (ViewGroup) getParent();
            parentView.removeView(this);
            if (controlView != null) {
                parentView.removeView(controlView);
            }
        }
    }

    @Override
    public String getContentUUID() {
        return mContentUuid;
    }

    public void setContentUUID(String uuid) {
        mContentUuid = uuid;
    }

    public void setCurrentPlay(int index) {
        if (mAdapter != null) {
            mAdapter.setSelectedIndex(index);
        }
        if (!hasFocus() && mRecycleView != null) {
            int first = mRecycleView.getFirstVisiblePosition();
            int last = mRecycleView.getLastVisiblePosition();
            if (index < first || index > last) {
                mRecycleView.scrollToPosition(index);
            }
        }
    }

    public void setHorizontalItemLayout(int layout, int count, int placeHolder, int dir) {
        item_layout = layout;
        mHorizontalCount = count;
        place_holder = placeHolder;
        direction = dir;
    }

    public void setContentUUID(int type, String uuid, View view) {
        controlView = view;
        mContentUuid = uuid;

        switch (type) {
            case TYPE_PERSON_HOST_LV:
                mPersonPresenter.getPersonTvList(mContentUuid);
                break;
            case TYPE_PERSON_RELATION_LV:
                mPersonPresenter.getPersonProgramList(mContentUuid);
                break;
            case TYPE_PROGRAM_SERICE_LV:
                //TODO 获取节目合集数据
                mContentPresenter.getSubContent(mContentUuid);
                break;
            case TYPE_ALTERNATE_LIST:
                mAlternatePresenter.getTodayAlternate(mContentUuid);
                break;
        }
    }

    @Override
    public boolean interruptKeyEvent(KeyEvent event) {
        if (!hasFocus() && mRecycleView != null && mRecycleView.getChildCount() > 0) {
            mRecycleView.requestDefaultFocus(mAdapter.getSelectedIndex());
            return true;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mAdapter != null && mRecycleView != null) {
            setCurrentPlay(mAdapter.getSelectedIndex());
        }
    }

    @Override
    public void tip(@NotNull Context context, @NotNull String message) {

    }

    @Override
    public void onError(@NotNull Context context, @Nullable String desc) {

    }

    @Override
    public void setPersonTvList(@Nullable ArrayList<SubContent> contents) {
        if (contents == null || contents.size() <= 0) {
            onLoadError();
            return;
        }
        buildUI(contents, TYPE_PERSON_HOST_LV);
    }

    @Override
    public void setPersonProgramList(@Nullable ArrayList<SubContent> contents) {
        if (contents == null || contents.size() <= 0) {
            onLoadError();
            return;
        }
        buildUI(contents, TYPE_PERSON_RELATION_LV);
    }

    @Override
    public void onContentResult(@NotNull String uuid, @Nullable Content content) {

    }

    @Override
    public void onSubContentResult(@NotNull String uuid, @Nullable ArrayList<SubContent> contents) {
        if (contents == null || contents.size() <= 0) {
            onLoadError();
            return;
        }
        buildUI(contents, TYPE_PROGRAM_SERICE_LV);
    }

    @Override
    public void onAlternateResult(@Nullable List<Alternate> alternates) {
        if (alternates == null || alternates.size() <= 0) {
            onLoadError();
            return;
        }
        buildUI(alternates, TYPE_ALTERNATE_LIST);

    }

    public interface OnSeriesInfoResult<T> {
        void onResult(List<T> result);
    }

    private static class ViewHolder extends NewTvRecycleAdapter.NewTvViewHolder implements
            OnFocusChangeListener {
        private RecycleImageView posterView;
        private TextView titleText;
        private FocusRelativeLayout modleView;

        public ViewHolder(View itemView) {
            super(itemView);

            modleView = itemView.findViewById(R.id.id_module_view);
            posterView = itemView.findViewWithTag("tag_poster_image");
            titleText = itemView.findViewWithTag("tag_poster_title");

            modleView.setOnFocusChangeListener(this);
            modleView.setResizeView(itemView);
            modleView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    performClick();
                }
            });
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            titleText.setSelected(hasFocus);

            performFocus(hasFocus);
            if (hasFocus) {
                itemView.bringToFront();
            }
        }
    }
}
