package tv.newtv.cboxtv.player.menu;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.utils.ScreenUtils;

/**
 * Created by TCP on 2018/6/11.
 */

public class MenuPopupWindow extends PopupWindow{
    private IMenuGroupPresenter menuGroupPresenter;

    public IMenuGroupPresenter show(Context context,View parent){
        View popView = LayoutInflater.from(context).inflate(R.layout.layout_menu_pop,null);
        FrameLayout rootView = popView.findViewById(R.id.root_view);
        setContentView(popView);

        if(ScreenUtils.getScreenW() == 0){
            ScreenUtils.initScreen(context);
        }
        setWidth(ScreenUtils.getScreenW());
        setHeight(ScreenUtils.getScreenH());
        setBackgroundDrawable(new BitmapDrawable());
        showAtLocation(parent, Gravity.NO_GRAVITY,0,0);

        menuGroupPresenter = new MenuGroupPresenter(context.getApplicationContext());
        rootView.addView(menuGroupPresenter.getRootView());
        return menuGroupPresenter;
    }

}
