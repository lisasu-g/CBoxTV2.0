package tv.newtv.cboxtv;

import android.app.Activity;

import com.newtv.libs.MainLooper;
import com.newtv.libs.ServerTime;
import com.newtv.libs.util.LogUtils;

import java.util.Stack;

/**
 * 项目名称:          CBoxTV
 * 包名:             tv.newtv
 * 创建事件:         18:23
 * 创建人:           weihaichao
 * 创建日期:         2018/5/7
 */
public final class ActivityStacks {

    private static final String TAG = ActivityStacks.class.getSimpleName();

    private static ActivityStacks instance;
    private final Stack<BaseActivity> activities = new Stack<>();
    private int activityCount = 0;

    private ActivityStacks() {

    }

    public static ActivityStacks get() {
        if (instance == null) {
            synchronized (ActivityStacks.class) {
                if (instance == null) instance = new ActivityStacks();
            }
        }
        return instance;
    }

    public void lifeCycle(ILifeCycle lifeCycle) {
        BaseActivity activity = getCurrentActivity();
        if (activity != null) {
            activity.lifeCycle(lifeCycle);
        }
    }

    public boolean isBackGround() {
        return activityCount == 0;
    }

    public void onStart(BaseActivity activity) {
        LogUtils.d(TAG, "onStart->" + activity);
        synchronized (activities) {
            activityCount++;
        }
    }

    public void onPause(BaseActivity activity) {
        LogUtils.d(TAG, "onPause->" + activity);
    }

    public void onResume(BaseActivity activity) {
        LogUtils.d(TAG, "onResume->" + activity);

    }

    public void onStop(BaseActivity activity) {
        LogUtils.d(TAG, "onStop->" + activity);
        synchronized (activities) {
            activityCount--;
        }

        LogUtils.d(TAG, "onStop-> count=" + activityCount);
        MainLooper.get().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (activityCount == 0) {
                    ServerTime.get().onEnterBackground();
                }
            }
        }, 3000);

    }

    public void onCreate(BaseActivity activity) {
        synchronized (activities) {
            LogUtils.d(TAG, "onCreate->" + activity);

            if (isVideoPlayerActivity(activity)) {
                finishBfPlayerActivity();
            }
            activities.push(activity);
        }
    }

    private boolean isVideoPlayerActivity(BaseActivity activity) {
        return activity.hasPlayer();
    }

    private void finishBfPlayerActivity() {
        if (activities.isEmpty()) return;
        synchronized (activities) {
            try {
                int size = activities.size();
                int left = 0;
                for (int index = size - 1; index > 0; index--) {
                    BaseActivity activity = activities.get(index);
                    if (isVideoPlayerActivity(activity)) {
                        if (left < 2) {
                            left += 1;
                            continue;
                        }
                        finishActivity(activity);
                    }
                }
            } catch (Exception e) {
                LogUtils.e(e.toString());
            }
        }
    }

    private void finishActivity(BaseActivity activity) {
        synchronized (activities) {
            LogUtils.d(TAG, "finishActivity");
            activity.finish();
            activities.removeElement(activity);
        }
    }

    /**
     * 获取当前Activity
     *
     * @return
     */
    public BaseActivity getCurrentActivity() {
        synchronized (activities) {
            if (activities.size() > 0)
                return activities.peek();

            return null;
        }
    }


    /**
     * 关闭当前Acitivity
     */
    public void FinishActivity() {
        synchronized (activities) {
            activities.pop().finish();
        }
    }

    /**
     * finish all Activity
     */
    public void finishAllActivity() {
        LogUtils.d(TAG, "finishAllActivity");
        if (activities.isEmpty()) return;
        synchronized (activities) {
            try {
                for (Activity activity : activities) {
                    activity.finish();
                }
            } catch (Exception e) {
                LogUtils.e(e.toString());
            }
            activities.clear();
        }
    }

    public void finishActivitysExcept(BaseActivity activity) {

    }

    public void ExitApp() {
        finishAllActivity();
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    /**
     * 销毁Activity
     *
     * @param activity
     */
    public void onDestroy(BaseActivity activity) {
        LogUtils.d(TAG, "onDestroy->" + activity);
        synchronized (activities) {
            if (!activities.empty()) {
                activities.removeElement(activity);
            }
        }
    }


}
