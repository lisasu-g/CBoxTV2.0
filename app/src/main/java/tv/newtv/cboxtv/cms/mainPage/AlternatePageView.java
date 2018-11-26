package tv.newtv.cboxtv.cms.mainPage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.newtv.cms.bean.Page;
import com.newtv.cms.bean.Program;

import java.util.List;

import tv.newtv.cboxtv.Navigation;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.player.listener.ScreenListener;
import tv.newtv.cboxtv.views.custom.BlockPosterView;
import tv.newtv.cboxtv.views.custom.ICustomPlayer;
import tv.newtv.cboxtv.views.widget.NewTvRecycleAdapter;
import tv.newtv.cboxtv.views.widget.VerticalRecycleView;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.cms.mainPage
 * 创建事件:         11:32
 * 创建人:           weihaichao
 * 创建日期:          2018/11/20
 */
public class AlternatePageView extends FrameLayout implements IProgramChange {

    private BlockPosterView mBlockPosterView;
    private VerticalRecycleView mRecycleView;
    private int curPlayIndex = 0;
    private Page mPage;
    private String mPageUUID;

    public AlternatePageView(Context context) {
        this(context, null);
    }

    public AlternatePageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlternatePageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }


    public void setPageUUID(String uuid) {
        mPageUUID = uuid;
        mBlockPosterView.setPageUUID(mPageUUID);
    }

    public void setProgram(Page page) {
        mPage = page;
        setUp();
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle) {
        LayoutInflater.from(context).inflate(R.layout.content_alternate_view_layout, this, true);
        mBlockPosterView = findViewById(R.id.block_poster);
        mRecycleView = findViewById(R.id.alternate_list);

        setUp();
    }

    private void setUp() {
        if (mPage == null || mBlockPosterView == null || mRecycleView == null) {
            return;
        }
        if (mPage.getPrograms() != null && mPage.getPrograms().size() > 0) {
            setRecycleView();
            Program program = mPage.getPrograms().get(curPlayIndex);
            play(program);
        }
    }

    private void play(Program program) {
        mBlockPosterView.setData(program);
    }

    private void setRecycleView() {
        if (mRecycleView == null) return;
        AlternateAdapter adapter = (AlternateAdapter) mRecycleView.getAdapter();
        if (adapter == null) {
            adapter = new AlternateAdapter(this);
            mRecycleView.setAdapter(adapter);
        }
        adapter.setData(subList(mPage.getPrograms(), 1, 5));
    }

    @SuppressWarnings("SameParameterValue")
    private List<Program> subList(List<Program> value, int start, int length) {
        if (start < 0 || length < 0) return null;
        int toIndex = length + start;
        if (toIndex < 0 || toIndex < start) return null;
        if (value == null) return null;
        if (value.size() >= toIndex) return value.subList(start, toIndex);
        return value.subList(start, value.size() - start);
    }

    @Override
    public void onChange(Program data, int position) {
        curPlayIndex = position + 1;
        play(data);
    }

    private static class AlternateAdapter extends NewTvRecycleAdapter<Program,
            AlternateViewHolder> {

        private IProgramChange mListener;
        private List<Program> mPrograms;

        AlternateAdapter(IProgramChange listener) {
            mListener = listener;
        }

        public void setData(List<Program> programs) {
            mPrograms = programs;
            notifyDataSetChanged();
        }

        @Override
        public List<Program> getDatas() {
            return mPrograms;
        }

        @Override
        public AlternateViewHolder createHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.alternate_index_item_layout, parent, false);
            return new AlternateViewHolder(itemView);
        }

        @Override
        public void bind(Program data, AlternateViewHolder holder, boolean selected) {
            holder.mAlternateId.setText("111");
            holder.mAlternateTitle.setText(data.getTitle());
            holder.mAlternateSubTitle.setText(data.getSubTitle());
            holder.itemView.setActivated(selected);
        }

        @Override
        public void onItemClick(Program data, int position) {
            if (mListener != null) {
                mListener.onChange(data, position);
            }
        }

        @Override
        public void onFocusChange(View view, int position, boolean hasFocus) {

        }
    }

    private static class AlternateViewHolder extends NewTvRecycleAdapter.NewTvViewHolder
            implements OnFocusChangeListener, OnClickListener {

        private TextView mAlternateId, mAlternateTitle, mAlternateSubTitle;

        AlternateViewHolder(View itemView) {
            super(itemView);

            mAlternateId = itemView.findViewById(R.id.alternate_id);
            mAlternateTitle = itemView.findViewById(R.id.alternate_title);
            mAlternateSubTitle = itemView.findViewById(R.id.alternate_subtitle);

            itemView.setOnFocusChangeListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            performClick();
        }

        @Override
        public void onFocusChange(View view, boolean b) {
            performFocus(b);
        }
    }
}