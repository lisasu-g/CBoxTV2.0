package tv.newtv.cboxtv.views.detail;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.newtv.libs.Constant;
import com.newtv.libs.util.BitmapUtil;
import com.newtv.libs.util.DeviceUtil;

import tv.newtv.cboxtv.BaseActivity;
import tv.newtv.cboxtv.BuildConfig;
import tv.newtv.cboxtv.R;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.views.detail
 * 创建事件:         13:58
 * 创建人:           weihaichao
 * 创建日期:          2018/10/19
 */
public abstract class DetailPageActivity extends BaseActivity {

    private String contentUUID;

    public String getContentUUID() {
        return contentUUID;
    }

    protected abstract boolean interruptDetailPageKeyEvent(KeyEvent event);

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("intent", getIntent());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            contentUUID = getIntent().getStringExtra(Constant.CONTENT_UUID);
        } else {
            Intent intent = savedInstanceState.getParcelable("intent");
            if (intent != null) {
                if (intent.hasExtra(Constant.CONTENT_UUID))
                    contentUUID = intent.getStringExtra(Constant.CONTENT_UUID);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ViewGroup viewGroup = findViewById(R.id.root_view);
        destroyViewGroup(viewGroup);
        BitmapUtil.recycleImageBitmap(viewGroup);
    }

    private void destroyViewGroup(ViewGroup viewGroup) {
        if (viewGroup != null) {
            int size = viewGroup.getChildCount();
            for (int index = 0; index < size; index++) {
                View view = viewGroup.getChildAt(index);
                if (view instanceof IEpisode) {
                    ((IEpisode) view).destroy();
                } else if (view instanceof ViewGroup) {
                    destroyViewGroup(viewGroup);
                }
            }
            if (viewGroup instanceof SmoothScrollView) {
                ((SmoothScrollView) viewGroup).destroy();
            }
        }
    }

    @SuppressWarnings({"ConstantConditions", "LoopConditionNotUpdatedInsideLoop"})
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (interruptKeyEvent(event)) {
            return super.dispatchKeyEvent(event);
        }
        if (BuildConfig.FLAVOR.equals(DeviceUtil.XUN_MA) && event.getAction() == KeyEvent
                .ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_ESCAPE:
                    finish();
                    return super.dispatchKeyEvent(event);
            }
        }
        if (interruptDetailPageKeyEvent(event)) {
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                finish();
                return super.dispatchKeyEvent(event);
            }
            ViewGroup viewGroup = findViewById(R.id.root_view);
            if (viewGroup == null) {
                return super.dispatchKeyEvent(event);
            }
            int size = viewGroup.getChildCount();
            for (int index = 0; index < size; index++) {
                View view = viewGroup.getChildAt(index);
                if (view != null) {
                    if (!view.hasFocus()) {
                        continue;
                    }
                    if (view instanceof IEpisode && ((IEpisode) view).interuptKeyEvent
                            (event)) {
                        return true;
                    } else {
                        View toView = null;
                        int pos = index;
                        int dir = 0;
                        boolean condition = false;
                        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                            dir = -1;
                            condition = true;
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                            dir = 1;
                            condition = true;
                        }
                        while (condition) {
                            pos += dir;
                            if (pos < 0 || pos > viewGroup.getChildCount()) {
                                break;
                            }
                            toView = viewGroup.getChildAt(pos);
                            if (toView != null) {
                                if (toView instanceof IEpisode && ((IEpisode) toView)
                                        .interuptKeyEvent
                                                (event)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}