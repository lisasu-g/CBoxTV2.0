package tv.newtv.cboxtv.cms.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.newtv.libs.AnimationBuilder;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.ScaleUtils;

import java.util.List;

import tv.newtv.cboxtv.player.PlayerConfig;

import static android.content.Context.ACTIVITY_SERVICE;


public class Utils {
    private static final String TAG = Utils.class.getName();

    public static void zoomByFactor(View view, float factor, int duration) {
        if (!view.isFocusable()) {
            return;
        }
//        ScaleAnimation animation = AnimationBuilder.getInstance()
//                .getScaleAnimation(1.0f, factor, 1.0f, factor,
//                        Animation.RELATIVE_TO_SELF, 0.5f,
//                        Animation.RELATIVE_TO_SELF, 0.5f, duration);
        ScaleUtils.getInstance().onItemGetFocus(view,duration,factor);
        TextView name = (TextView) view.findViewWithTag("");
        if (name != null) {
            name.setVisibility(View.VISIBLE);
        }
//        if (animation != null) {
//            view.startAnimation(animation);
//        }
    }

    public static void scaleToOriginalDimension(View view, float factor, int duration) {
//        ScaleAnimation animation = AnimationBuilder.getInstance()
//                .getScaleAnimation(factor, 1.0f, factor, 1.0f,
//                        Animation.RELATIVE_TO_SELF, 0.5f,
//                        Animation.RELATIVE_TO_SELF, 0.5f, duration);
        ScaleUtils.getInstance().onItemLoseFocus(view,duration,factor);
        TextView name = (TextView) view.findViewWithTag("");
        if (name != null) {
            name.setSelected(false);
        }
//        if (animation != null) {
//            view.startAnimation(animation);
//        }
    }

    public static String getSplitFirst(String s) {
        String result = "";
        String[] sp = s.split("|");
        if (sp != null && sp.length > 0) {
            result = sp[0];
        }

        return result;
    }



    public static long getSysTime(){
        return System.currentTimeMillis()/1000;
    }

    /**
     * 判断某一个类是否存在任务栈里面
     * @return
     */
    public static boolean isExsitActivity(Context context, Class<?> cls){
        boolean flag = false;
        try {
            Intent intent = new Intent(context, cls);
            ComponentName cmpName = intent.resolveActivity(context.getPackageManager());

            if (cmpName != null) { // 说明系统中存在这个activity
                ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> taskInfoList = am.getRunningTasks(10);
                for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                    if (taskInfo.baseActivity.equals(cmpName)) { // 说明它已经启动了
                        flag = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }

        return flag;
    }



    //用于传入播放器，播放器传入广告sdk后获取广告
    public static String buildExtendString() {
        String extend = "";

        String columnId = PlayerConfig.getInstance().getColumnId();
        if (!TextUtils.isEmpty(columnId)) {
            if (!TextUtils.isEmpty(columnId)) {
                extend = "panel=" + columnId;
            }

            String secondColumnId = PlayerConfig.getInstance().getSecondColumnId();
            if (!TextUtils.isEmpty(secondColumnId)) {
                if (TextUtils.isEmpty(extend)) {
                    extend += "secondpanel=" + secondColumnId;
                } else {
                    extend += "&secondpanel=" + secondColumnId;
                }
            }
        } else {
            String parentId = PlayerConfig.getInstance().getFirstChannelId();
            if (!TextUtils.isEmpty(parentId)) {
                extend = "panel=" + parentId;
            }

            String contentId = PlayerConfig.getInstance().getSecondChannelId();
            if (!TextUtils.isEmpty(contentId)) {
                if (TextUtils.isEmpty(extend)) {
                    extend = "secondpanel=" + contentId;
                } else {
                    extend += "&secondpanel=" + contentId;
                }
            }
        }

        String topic = PlayerConfig.getInstance().getTopicId();
        if (!TextUtils.isEmpty(topic)) {
            extend += "&topic=" + topic;
        }

        LogUtils.i(TAG, "extend=" + extend);
        return extend;
    }
}
