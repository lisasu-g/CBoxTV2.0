package tv.newtv.cboxtv.views;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import tv.newtv.cboxtv.utils.ScreenUtils;

/**
 * 项目名称:         CBoxTV
 * 包名:            tv
 * 创建事件:         19:12
 * 创建人:           weihaichao
 * 创建日期:          2018/4/2
 */

public class ScollLinearLayoutManager extends LinearLayoutManager {
    private float MILLISECONDS_PER_INCH = 2.8f;  //修改可以改变数据,越大速度越慢
    private Context contxt;
    private int mSize = 0;
    private boolean mAutoResize = true;

    public ScollLinearLayoutManager(Context context) {
        super(context, LinearLayoutManager.HORIZONTAL, false);
        this.contxt = context;
    }

    public void setAutoResize(boolean value) {
        mAutoResize = value;
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {

        return super.onInterceptFocusSearch(focused, direction);
    }

    public void setDataSize(int size) {
        mSize = size;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int
            position) {
        LinearSmoothScroller linearSmoothScroller =
                new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return ScollLinearLayoutManager.this
                                .computeScrollVectorForPosition(targetPosition);
                    }

                    //This returns the milliseconds it takes to
                    //scroll one pixel.
                    @Override
                    protected float calculateSpeedPerPixel
                    (DisplayMetrics displayMetrics) {
                        return MILLISECONDS_PER_INCH / displayMetrics.density;
                    }

                };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    //可以用来设置速度
    public void setSpeedSlow(float x) {
        //自己在这里用density去乘，希望不同分辨率设备上滑动速度相同
        //0.3f是自己估摸的一个值，可以根据不同需求自己修改
        MILLISECONDS_PER_INCH = contxt.getResources().getDisplayMetrics().density * 0.3f + (x);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int
            widthSpec, int heightSpec) {
        if (getChildCount() > 0 && mAutoResize) {
            View firstChildView = getChildAt(0);
            measureChild(firstChildView, widthSpec, heightSpec);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)
                    firstChildView.getLayoutParams();
            int measureSize = (firstChildView.getMeasuredWidth() + layoutParams
                    .leftMargin + layoutParams.rightMargin) * mSize;
            int width = measureSize > ScreenUtils
                    .getScreenW() ? ScreenUtils.getScreenW() : measureSize;
            setMeasuredDimension(width, View.MeasureSpec.getSize(heightSpec));
            ViewGroup.LayoutParams parentLayout = ((RecyclerView) firstChildView.getParent())
                    .getLayoutParams();
            parentLayout.width = width;
            ((RecyclerView) firstChildView.getParent()).setLayoutParams(parentLayout);
            ((RecyclerView) firstChildView.getParent()).requestLayout();
        } else {
            super.onMeasure(recycler, state, widthSpec, heightSpec);
        }
    }
}