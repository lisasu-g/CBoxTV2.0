package tv.newtv.cboxtv.player.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.newtv.libs.bean.CountDown;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.SPrefUtils;

import java.util.Locale;

import tv.newtv.player.R;


/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.views.custom
 * 创建事件:         13:11
 * 创建人:           weihaichao
 * 创建日期:          2018/11/26
 */
public class AlternateTipView extends FrameLayout {

    private static final String ALTERNATE_TIP_TAG = "int_alternate_tip";
    private CountDown downTimer;
    private TextView tipView;
    private boolean needTip = true;
    private int currentValue = 0;

    public AlternateTipView(Context context) {
        this(context, null);
    }

    public AlternateTipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlternateTipView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public boolean isNeedTip() {
        return needTip;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SPrefUtils.setValue(getContext(), ALTERNATE_TIP_TAG, currentValue + 1);
        if (downTimer != null) {
            downTimer.cancel();
            downTimer = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        CountDown countDown = new CountDown(5);
        countDown.listen(new CountDown.Listen() {
            @Override
            public void onCount(final int time) {
                if (tipView != null) {
                    tipView.post(new Runnable() {
                        @Override
                        public void run() {
                            tipView.setText(String.format(Locale.getDefault(), "知道了(%d)",
                                    time));
                        }
                    });
                }
            }

            @Override
            public void onComplete() {
                if (tipView != null) {
                    tipView.post(new Runnable() {
                        @Override
                        public void run() {
                            tipView.setText(String.format(Locale.getDefault(), "知道了(%d)",
                                    0));
                        }
                    });
                }

                AlternateTipView.this.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ViewGroup parentView = (ViewGroup) getParent();
                        if (parentView != null) {
                            parentView.removeView(AlternateTipView.this);
                        }
                    }
                }, 300);
            }

            @Override
            public void onCancel() {

            }
        });
        countDown.start();
    }

    private void initialize(Context context) {
        currentValue = (int) SPrefUtils.getValue(context, ALTERNATE_TIP_TAG, 0);
        needTip = currentValue < 3;

        LogUtils.d("AlternateTipView", "current tip time=" + currentValue);

        LayoutInflater.from(context).inflate(R.layout.alternate_tip_layout, this, true);
        tipView = findViewById(R.id.tip_button);
        if (tipView != null) {
            tipView.setText(String.format(Locale.getDefault(), "知道了(%d)",
                    5));
        }
    }

    public boolean interruptKeyEvent(KeyEvent event) {
        return getChildCount() > 0 && getVisibility() == VISIBLE;
    }
}
