package tv.newtv.cboxtv.views.detail;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;

import com.newtv.libs.Constant;
import com.newtv.libs.ad.ADHelper;
import com.newtv.libs.ad.ADPresenter;

import tv.newtv.cboxtv.player.PlayerConfig;


public class SmallWindowView extends BaseAdView implements IEpisode {
    private static final String TAG = "SmallWindowView";

    public SmallWindowView(Context context) {
        this(context, null);
    }

    public SmallWindowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmallWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void getAD(ADPresenter mADPresenter) {
        mADPresenter.getAD(Constant.AD_DESK, Constant.AD_DETAILPAGE_RIGHTPOS, "", PlayerConfig
                .getInstance().getFirstChannelId(), PlayerConfig.getInstance().getSecondChannelId
                (), PlayerConfig.getInstance().getTopicId());
    }

    @Override
    public void showAd(ADHelper.AD.ADItem result) {
        Log.i(TAG, "showAd: " + result);
        super.showAd(result);
    }


    @Override
    public String getContentUUID() {
        return null;
    }

    @Override
    public boolean interuptKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || event.getKeyCode() == KeyEvent
                    .KEYCODE_DPAD_RIGHT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        mADPresenter.destroy();
    }

}