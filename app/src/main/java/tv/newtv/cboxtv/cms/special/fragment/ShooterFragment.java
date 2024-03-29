package tv.newtv.cboxtv.cms.special.fragment;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.ModelResult;
import com.newtv.cms.bean.Page;
import com.newtv.cms.bean.Program;
import com.newtv.libs.util.DisplayUtils;

import com.newtv.libs.util.ScaleUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.MainLooper;
import tv.newtv.cboxtv.cms.mainPage.AiyaRecyclerView;
import tv.newtv.cboxtv.cms.special.OnItemAction;
import tv.newtv.cboxtv.cms.util.PosterCircleTransform;
import tv.newtv.cboxtv.player.videoview.PlayerCallback;
import tv.newtv.cboxtv.player.videoview.VideoExitFullScreenCallBack;
import tv.newtv.cboxtv.player.videoview.VideoPlayerView;
import tv.newtv.cboxtv.views.custom.CurrentPlayImageViewWorldCup;

/**
 * 项目名称:         CBoxTV
 * 包名:            tv.newtv.cboxtv.cms.special.fragment
 * 创建事件:         13:21
 * 创建人:           weihaichao
 * 创建日期:          2018/4/25
 */
public class ShooterFragment extends BaseSpecialContentFragment implements PlayerCallback {
    private AiyaRecyclerView recyclerView;
    private ModelResult<ArrayList<Page>> moduleInfoResult;
    private View topView;
    private View downView;
    private int videoIndex = 0;
    private View focusView;
    private Content mProgramSeriesInfo;

    private int playIndex = 0;
    private int playPostion = 0;

    @Override
    protected int getVideoPlayIndex() {
        return videoIndex;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.shooter_layout;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        recyclerView = null;
        moduleInfoResult = null;
        focusView = null;
    }

    @Override
    protected void setUpUI(View view) {

        recyclerView = view.findViewById(R.id.shooter_recycle);
        topView = view.findViewById(R.id.shooter_up);
        downView = view.findViewById(R.id.shooter_down);
        topView.setVisibility(View.GONE);
        downView.setVisibility(View.GONE);
//        int itemSpace = getResources().
//                getDimensionPixelSize(R.dimen.width_1px);
//        recyclerView.addItemDecoration(new SpacesItemDecoration(itemSpace));
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(),
                LinearLayoutManager.VERTICAL, false));
        int space = view.getContext().getResources().getDimensionPixelOffset(R.dimen.width_10px);
        recyclerView.setSpace(0, space * -1);

        ShooterAdapter adapter = new ShooterAdapter();
        recyclerView.setItemAnimator(null);
        recyclerView.setAlign(AiyaRecyclerView.ALIGN_START);
        recyclerView.setAdapter(adapter);
        recyclerView.setDirIndicator(topView,downView);
        adapter.setOnItemAction(new OnItemAction<Program>() {
            @Override
            public void onItemFocus(View item) {

            }

            @Override
            public void onItemClick(Program item, int index) {
                videoIndex = index;
                onItemClickAction(item);
            }

            @Override
            public void onItemChange(final int before, final int current) {
                    MainLooper.get().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (recyclerView != null) {
                                recyclerView.getAdapter().notifyItemChanged(before);
                                recyclerView.getAdapter().notifyItemChanged(current);
                            }
                        }
                    }, 300);

            }
        });

        videoPlayerView = view.findViewById(R.id.video_player);
        videoPlayerView.setPlayerCallback(this);
        videoPlayerView.outerControl();
        videoPlayerView.setFocusView(view.findViewById(R.id.video_player_focus), true);
        if (moduleInfoResult != null) {
            adapter.refreshData(moduleInfoResult.getData().get(0).getPrograms())
                    .notifyDataSetChanged();
        }
        videoPlayerView.setVideoExitCallback(new VideoExitFullScreenCallBack() {
            @Override
            public void videoEitFullScreen(boolean isLiving) {
                int position = adapter.getFocusedPosition();
                recyclerView.scrollToPosition(position);
            }
        });
    }

    private void onItemClickAction(Program programInfo) {
        videoPlayerView.beginChange();
        getContent(programInfo.getL_id(),programInfo);
    }

    @Override
    public void setModuleInfo(ModelResult<ArrayList<Page>> infoResult) {
        moduleInfoResult = infoResult;
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            ((ShooterAdapter) recyclerView.getAdapter()).refreshData(infoResult.getData().get(0)
                    .getPrograms()).notifyDataSetChanged();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (getContentView() != null) {
                focusView = getContentView().findFocus();
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (focusView instanceof VideoPlayerView) {
                    return true;
                }
                if (videoPlayerView != null) {
                    videoPlayerView.requestFocus();
                }
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                if (focusView instanceof VideoPlayerView) {
                    return true;
                }
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (focusView instanceof VideoPlayerView) {
                    recyclerView.getDefaultFocusView().requestFocus();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onEpisodeChange(int index, int position) {
        playIndex = index;
    }

    @Override
    public void onPlayerClick(VideoPlayerView videoPlayerView) {
        videoPlayerView.EnterFullScreen(getActivity(), false);
    }

    @Override
    public void AllPlayComplete(boolean isError, String info, VideoPlayerView videoPlayerView) {
        if (recyclerView.getAdapter() != null) {
            ShooterAdapter adapter = (ShooterAdapter) recyclerView.getAdapter();
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView
                    .getLayoutManager();
            videoIndex++;
            if(videoIndex == adapter.getItemCount()){
                videoPlayerView.setisEnd(true);
            }
            Log.d("zhangxianda","videoIndex="+videoIndex);
            if (adapter.getItemCount() - 1 < videoIndex) {
                return;
            }
            int first = layoutManager.findFirstVisibleItemPosition();
            int last = layoutManager.findLastVisibleItemPosition();
            int postion = 0;
            if (videoIndex == first) {
                postion = 0;
            } else if (videoIndex > first && videoIndex <= last) {
                postion = videoIndex - first;
            }

            View view = recyclerView.getChildAt(postion);
            ShooterViewHolder viewHolder = (ShooterViewHolder) recyclerView.getChildViewHolder
                    (view);
            if (viewHolder != null) {
                viewHolder.dispatchSelect();
            }
        }
    }

    @Override
    public void ProgramChange() {
        videoPlayerView.setSeriesInfo(mProgramSeriesInfo);
        videoPlayerView.playSingleOrSeries(playIndex,0);
    }

    @Override
    public void onItemContentResult(String uuid, @Nullable Content content, int playIndex) {
        if (content != null) {
            mProgramSeriesInfo = content;
            Log.e("info", content.toString());
            if (videoPlayerView != null) {
                videoPlayerView.setSeriesInfo(content);
                videoPlayerView.playSingleOrSeries(playIndex, 0);
            }
        } else {
            if (videoPlayerView != null) {
                videoPlayerView.showProgramError();
            }
        }
    }

    private static class ShooterAdapter extends RecyclerView.Adapter<ShooterViewHolder> {

        private List<Program> ModuleItems;
        private OnItemAction<Program> onItemAction;
        private CurrentPlayImageViewWorldCup currentPlayImageView;
        private String currentUUID;
        private int currentIndex = 0;
        private int mFocusedPosition = 0;
        private Transformation transformation;


        ShooterAdapter refreshData(List<Program> datas) {
            ModuleItems = datas;
            return this;
        }

        void setOnItemAction(OnItemAction<Program> programInfoOnItemAction) {
            onItemAction = programInfoOnItemAction;
        }

        @Override
        public ShooterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout
                    .shooter_item_layout, parent, false);
//            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view
//                    .getLayoutParams();
//            int space = view.getContext().getResources().getDimensionPixelOffset(R.dimen
//                    .height_18px) * -1;
//            layoutParams.topMargin = space;
//            view.setLayoutParams(layoutParams);
            return new ShooterViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ShooterViewHolder holder, int position) {
            Program moduleItem = getItem(position);

            holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    mFocusedPosition = position;
                    if (hasFocus) {
                        ScaleUtils.getInstance().onItemGetFocus(view, holder.posterFocus);
                    } else {
                        ScaleUtils.getInstance().onItemLoseFocus(view, holder.posterFocus);
                    }
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (currentPlayImageView != null) {
                        currentPlayImageView.setIsPlaying(false, false);
                    }
                    currentPlayImageView = holder.poster;
                    holder.poster.setIsPlaying(true, false);
                    final Program moduleItem = getItem(holder.getAdapterPosition());
                    if (moduleItem != null) {
                        currentUUID = moduleItem.getL_id();
                        onItemAction.onItemChange(currentIndex, holder.getAdapterPosition());

                        currentIndex = holder.getAdapterPosition();
                        onItemAction.onItemClick(moduleItem, holder.getAdapterPosition());
                    }
                }
            });
            if (moduleItem != null) {
                holder.poster.setIsPlaying(TextUtils.equals(moduleItem.getL_id(),currentUUID),
                false);

                int targetWidth =holder.itemView.getContext().getResources().getDimensionPixelOffset(R.dimen.height_518px);
                int targetHeiht = holder.itemView.getContext().getResources().getDimensionPixelOffset(R.dimen.height_200px);
                int radius = holder.itemView.getContext().getResources().getDimensionPixelOffset(R.dimen.width_4px);
                Picasso.get()
                        .load(moduleItem.getImg())
                        .transform(new PosterCircleTransform(holder.itemView.getContext(), radius))
                        .resize(targetWidth,targetHeiht)
                        .into(holder.poster);



                if (TextUtils.equals(moduleItem.getL_id(),currentUUID) && position == currentIndex) {
                    holder.poster.setIsPlaying(true, false);
                } else {
                    holder.poster.setIsPlaying(false, false);
                }
                if (TextUtils.isEmpty(currentUUID)) {
                    if (position == 0) {
                        holder.dispatchSelect();
                    }
                }
            }
        }
        public int getFocusedPosition(){
            return mFocusedPosition;
        }
        private Program getItem(int position) {
            if (ModuleItems == null || position < 0 || ModuleItems.size() <= position) {
                return null;
            }
            return ModuleItems.get(position);
        }

        @Override
        public int getItemCount() {
            return ModuleItems != null ? ModuleItems.size() : 0;
        }
    }

    private static class ShooterViewHolder extends RecyclerView.ViewHolder {
        public CurrentPlayImageViewWorldCup poster;
        public ImageView posterFocus;

        ShooterViewHolder(View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.shooter_poster);
            posterFocus = itemView.findViewById(R.id.shooter_poster_onfocus);
            DisplayUtils.adjustView(itemView.getContext(), poster, posterFocus, R.dimen.width_17dp, R.dimen.height_16dp);

        }

        public void dispatchSelect() {
            itemView.requestFocus();
            itemView.performClick();
        }
    }

}
