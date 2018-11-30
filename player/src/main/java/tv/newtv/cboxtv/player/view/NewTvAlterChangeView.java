package tv.newtv.cboxtv.player.view;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.newtv.cms.contract.AdContract;
import com.newtv.libs.Constant;
import com.newtv.libs.util.GlideUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import tv.newtv.player.R;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.player.view
 * 创建事件:         20:06
 * 创建人:           weihaichao
 * 创建日期:          2018/11/29
 */
public class NewTvAlterChangeView extends FrameLayout implements AdContract.View {

    private AdContract.Presenter mAdPresenter;

    private TextView channelText;
    private TextView titleText;
    private ImageView background;

    private String currentChannel, currentTitle;

    public NewTvAlterChangeView(Context context) {
        this(context, null);
    }

    public NewTvAlterChangeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NewTvAlterChangeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(getContext()).inflate(R.layout.change_alternate_layout, this, true);
        mAdPresenter = new AdContract.AdPresenter(getContext(), this);

        channelText = findViewById(R.id.alter_channel);
        titleText = findViewById(R.id.alter_title);
        background = findViewById(R.id.background_ad);
    }

    public void setChannelText(String text) {
        currentChannel = text;
        if (channelText != null) {
            channelText.setText(currentChannel);
        }
    }

    public void setTitleText(String text) {
        currentTitle = text;
        if (titleText != null) {
            titleText.setText(currentTitle);
        }
    }

    public void show() {
        mAdPresenter.getAdByType(Constant.AD_DESK, Constant.AD_CAROUSEL_CORNER, "", null);
        setVisibility(VISIBLE);
        NewTVLauncherPlayerViewManager.getInstance().setShowingView(NewTVLauncherPlayerView
                .SHOWING_ALTER_CHANGE_VIEW);
    }

    public void dismiss() {
        setVisibility(GONE);
        if (NewTVLauncherPlayerViewManager.getInstance().getShowView() == NewTVLauncherPlayerView
                .SHOWING_ALTER_CHANGE_VIEW) {
            NewTVLauncherPlayerViewManager.getInstance().setShowingView(NewTVLauncherPlayerView
                    .SHOWING_NO_VIEW);
        }
    }

    @Override
    public void showAd(@Nullable String type, @Nullable String url, @Nullable HashMap<?, ?>
            hashMap) {
        if (Constant.AD_IMAGE_TYPE.equals(type)) {
            if (background != null && !TextUtils.isEmpty(url)) {
                if (url.startsWith("http") || url.startsWith("https")) {
                    GlideUtil.loadImage(getContext(), background, url, R.drawable.normalplayer_bg, R.drawable.normalplayer_bg, false);
                } else if (url.startsWith("file:")) {
                    background.setImageURI(Uri.parse(url));
                }
            }
        }
    }

    @Override
    public void updateTime(int total, int left) {

    }

    @Override
    public void complete() {
//        dismiss();
    }

    @Override
    public void tip(@NotNull Context context, @NotNull String message) {

    }

    @Override
    public void onError(@NotNull Context context, @Nullable String desc) {

    }
}