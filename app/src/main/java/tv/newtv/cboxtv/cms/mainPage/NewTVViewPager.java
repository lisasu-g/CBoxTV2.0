package tv.newtv.cboxtv.cms.mainPage;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.Scroller;

import java.lang.reflect.Field;

import tv.newtv.cboxtv.cms.util.LogUtils;

/**
 * Created by lixin on 2018/3/17.
 */

public class NewTVViewPager extends ViewPager {
    public boolean mScrollable;

    public Scroller mScroller;

    public NewTVViewPager(Context context) {
        this(context, null);
    }


    public NewTVViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollable(boolean scrollable) {
        this.mScrollable = scrollable;
    }

    public void setCustomScroller(Scroller scroller) {
        mScroller = scroller;

        Field fieldScroller;
        try {
            fieldScroller = ViewPager.class.getDeclaredField("mScroller");
            fieldScroller.setAccessible(true);
            fieldScroller.set(this, mScroller);
        } catch (NoSuchFieldException e) {
            LogUtils.e(e);
        } catch (IllegalAccessException e) {
            LogUtils.e(e);
        } catch (IllegalArgumentException e) {
            LogUtils.e(e);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mScrollable) {
            super.scrollTo(x, y);
        }
    }
}
