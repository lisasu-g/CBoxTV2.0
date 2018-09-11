package tv.newtv.cboxtv.utils;

import android.view.KeyEvent;

import java.lang.reflect.Field;

import tv.newtv.cboxtv.BuildConfig;

public class XunMaKeyUtils {

    public static void key(KeyEvent keyEvent){
        if(BuildConfig.FLAVOR.equals(DeviceUtil.XUN_MA) && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ESCAPE){
            try {
                Field mKeyCode = keyEvent.getClass().getDeclaredField("mKeyCode");
                mKeyCode.setAccessible(true);
                mKeyCode.set(keyEvent,KeyEvent.KEYCODE_BACK);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
