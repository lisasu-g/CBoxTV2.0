package tv.newtv.cboxtv.player.menu;

import android.view.KeyEvent;
import android.view.View;

/**
 * Created by TCP on 2018/5/15.
 */

public interface IMenuGroupPresenter {

    View getRootView();

    boolean dispatchKeyEvent(KeyEvent event);

    void addSelectListener(MenuGroup.OnSelectListener listener);

    boolean isCanShowMenuGroup();

    void release();

    boolean isShow();

    void gone();
}
