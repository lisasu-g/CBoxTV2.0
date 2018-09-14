package tv.newtv.cboxtv.cms.util;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tv.icntv.logsdk.logSDK;
import tv.newtv.cboxtv.Constant;

public class LogUploadUtils {
    private static String TAG = "logsdk";

    private static boolean isUpload = true;//日志初始化和上传的开关
    private static logSDK logUpload;//日志上传对象
    //初始化是否成功
    private volatile static boolean isInit = false;
    //是否正在初始化
    private volatile static boolean isInitializing = false;

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static List<Runnable> runnableList = new ArrayList<>();
    private static int retryNumber = 0;

    @SuppressLint("CheckResult")
    public static void initSDk() {
        if (isInit || isInitializing) {
            return ;
        }
        isInitializing = true;

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                logUpload = logSDK.getInstance();
                isInit = logUpload.sdkInit(Constant.LOG_ADDR, "", Constant.UUID, Constant
                        .CHANNEL_CODE, Constant.APPKEY);

                if(isInit){
                    isInitializing = false;
                    for(Runnable runnable : runnableList){
                        executorService.submit(runnable);
                    }
                    runnableList.clear();
                } else {
                    try {
                        if(retryNumber++ < 10){
                            Thread.sleep(10 * 1000);
                            executorService.submit(this);
                        }else {
                            isInitializing = false;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * @param type
     * @param content
     * @return 0成功 <0失败
     */
    @SuppressLint("CheckResult")
    public static void uploadLog(final int type, final String content) {
        if (!isUpload) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "日志上报内容=" + content + ",type=" + type
                        +",日志上传结果result="+logUpload.logUpload(type,content));
            }
        };

        if(isInit){
            executorService.execute(runnable);
        }else {
            runnableList.add(runnable);
            if(!isInitializing){
                initSDk();
            }
        }
    }

}
