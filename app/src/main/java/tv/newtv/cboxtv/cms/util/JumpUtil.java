package tv.newtv.cboxtv.cms.util;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.newtv.cms.bean.Program;
import com.newtv.libs.Constant;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.NetworkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import tv.newtv.cboxtv.LauncherApplication;
import tv.newtv.cboxtv.NewTVLauncherPlayerActivity;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.details.ColumnPageActivity;
import tv.newtv.cboxtv.cms.details.PersonsDetailsActivityNew;
import tv.newtv.cboxtv.cms.details.ProgramCollectionActivity;
import tv.newtv.cboxtv.cms.details.ProgrameSeriesAndVarietyDetailActivity;
import tv.newtv.cboxtv.cms.details.SingleDetailPageActivity;
import tv.newtv.cboxtv.cms.listPage.ListPageActivity;
import tv.newtv.cboxtv.cms.mainPage.menu.MainNavManager;
import tv.newtv.cboxtv.cms.screenList.ScreenListActivity;
import tv.newtv.cboxtv.cms.special.SpecialActivity;
import tv.newtv.cboxtv.player.view.NewTVLauncherPlayerViewManager;
import tv.newtv.cboxtv.uc.bean.UserCenterPageBean;
import tv.newtv.cboxtv.utils.PlayInfoUtil;

public class JumpUtil {

    private static int mPlayPosition;

    private static HashMap<String, String> parseParamMap(String paramStr) {
        HashMap<String, String> paramsMap = new HashMap<>();
        if (TextUtils.isEmpty(paramStr)) return paramsMap;
        String[] params = paramStr.split("&");
        for (String param : params) {
            String[] values = param.split("=");
            paramsMap.put(values[0], values.length > 1 ? values[1] : "");
        }
        return paramsMap;
    }

    private static String getParamValue(HashMap<String, String> hashMap, String key) {
        if (hashMap == null) return "";
        if (!hashMap.containsKey(key)) return "";
        return hashMap.get(key);
    }

    public static boolean parseExternalJump(Context context, String action, String params) {
        if (TextUtils.isEmpty(action)) return false;
        int index = action.indexOf("|");
        String actionType = action.substring(0, index);
        String contentType = action.substring(index + 1, action.length());
        JumpUtil.activityJump(context, actionType, contentType,
                parseParamMap(params), true);
        Log.e("Splash", "SplashActivity---> onCreate 接收到外部应用跳转需求, action : "
                + action + " param : " + params+"============="+ action);
        return true;
    }

    public static void activityJump(Context context, Program info) {
        // fix bug LETVYSYY-51
        if (!NetworkManager.getInstance().isConnected()) {
            Toast.makeText(context, R.string.net_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent jumpIntent = getIntent(context, info.getL_actionType(), info.getL_contentType(), info
                .getL_id(), info.getSeriesSubUUID());
        if (jumpIntent != null) {
            jumpIntent.putExtra(Constant.CONTENT_TYPE, info.getL_contentType());
            jumpIntent.putExtra(Constant.CONTENT_UUID, info.getL_id());
            jumpIntent.putExtra(Constant.PAGE_UUID, info.getL_id());
            jumpIntent.putExtra(Constant.ACTION_TYPE, info.getL_actionType());
            jumpIntent.putExtra(Constant.ACTION_URI, info.getL_actionUri());
            jumpIntent.putExtra(Constant.DEFAULT_UUID, info.getL_focusId());
            jumpIntent.putExtra(Constant.FOCUSPARAM, info.getL_focusParam());


            jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityCompat.startActivity(context, jumpIntent, null);
        }
    }

    public static void activityJump(Context context, String actionType, String contentType,
                                    String contentUUID, String actionUri) {
        activityJump(context, actionType, contentType, contentUUID, actionUri, "");
    }

    //bug YSYY130XM-10
    public static void activityJump(Context context, boolean isADEntry,String actionType, String contentType,
                                    String contentUUID, String actionUri) {
        activityJump(context, actionType, contentType, contentUUID, actionUri, "",false,isADEntry);
    }
    //bug YSYY130XM-10
    public static void activityJump(Context context, String actionType, String contentType,
                                    String contentUUID, String actionUri, String seriesSubUUID,
                                    boolean fromOuter,boolean isADEntry) {
        // fix bug LETVYSYY-51
        if (!NetworkManager.getInstance().isConnected()) {
            Toast.makeText(context, R.string.net_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent jumpIntent = getIntent(context, actionType, contentType, contentUUID, seriesSubUUID);
        if (jumpIntent != null) {
            jumpIntent.putExtra(Constant.CONTENT_TYPE, contentType);
            jumpIntent.putExtra(Constant.CONTENT_UUID, contentUUID);
            jumpIntent.putExtra(Constant.PAGE_UUID, contentUUID);
            jumpIntent.putExtra(Constant.ACTION_TYPE, actionType);
            jumpIntent.putExtra(Constant.ACTION_URI, actionUri);
            jumpIntent.putExtra(Constant.ACTION_FROM, fromOuter);
            jumpIntent.putExtra(Constant.ACTION_AD_ENTRY,isADEntry);
            jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityCompat.startActivity(context, jumpIntent, null);
        }
    }

    public static void activityJump(Context context, String actionType, String contentType,
                                    HashMap<String, String> params, boolean fromOuter) {
        Intent jumpIntent = getIntent(context, actionType, contentType, getParamValue
                (params,
                        Constant.EXTERNAL_PARAM_CONTENT_UUID), getParamValue(params, Constant
                .EXTERNAL_PARAM_SERIES_SUB_UUID));
        if (jumpIntent != null) {
            jumpIntent.putExtra(Constant.CONTENT_TYPE, contentType);
            jumpIntent.putExtra(Constant.CONTENT_UUID, getParamValue(params,
                    Constant.EXTERNAL_PARAM_CONTENT_UUID));
            jumpIntent.putExtra(Constant.PAGE_UUID, getParamValue(params,
                    Constant.EXTERNAL_PARAM_CONTENT_UUID));
            jumpIntent.putExtra(Constant.ACTION_TYPE, actionType);
            jumpIntent.putExtra(Constant.ACTION_URI, getParamValue(params,
                    Constant.EXTERNAL_PARAM_ACTION_URI));
            jumpIntent.putExtra(Constant.DEFAULT_UUID, getParamValue(params,
                    Constant.EXTERNAL_PARAM_FOCUS_UUID));
            jumpIntent.putExtra(Constant.ACTION_FROM, fromOuter);
//            jumpIntent.putExtra(Constant.FOCUSPARAM, info.getFocusParam());

            jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityCompat.startActivity(context, jumpIntent, null);
        }
    }


    public static void activityJump(Context context, String actionType, String contentType,
                                    String contentUUID, String actionUri, boolean fromOuter) {
        activityJump(context, actionType, contentType, contentUUID, actionUri, "", fromOuter);
    }

    public static void activityJump(Context context, String actionType, String contentType,
                                    String contentUUID, String actionUri, String seriesSubUUID) {
        activityJump(context, actionType, contentType, contentUUID, actionUri, seriesSubUUID,
                false);
    }

    public static void activityJump(Context context, String actionType, String contentType,
                                    String contentUUID, String actionUri, String seriesSubUUID,
                                    boolean fromOuter) {
        // fix bug LETVYSYY-51
        if (!NetworkManager.getInstance().isConnected()) {
            Toast.makeText(context, R.string.net_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent jumpIntent = getIntent(context, actionType, contentType, contentUUID, seriesSubUUID);
        if (jumpIntent != null) {
            jumpIntent.putExtra(Constant.CONTENT_TYPE, contentType);
            jumpIntent.putExtra(Constant.CONTENT_UUID, contentUUID);
            jumpIntent.putExtra(Constant.PAGE_UUID, contentUUID);
            jumpIntent.putExtra(Constant.ACTION_TYPE, actionType);
            jumpIntent.putExtra(Constant.ACTION_URI, actionUri);
            jumpIntent.putExtra(Constant.DEFAULT_UUID,seriesSubUUID);
            jumpIntent.putExtra(Constant.ACTION_FROM, fromOuter);
            jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityCompat.startActivity(context, jumpIntent, null);
        }
    }

    private static Intent getIntent(final Context context, String actionType, String contentType,
                                    final String contentUUID, String seriesSubUUID) {
        Intent jumpIntent = null;
        try {
            LogUtils.i(Constant.TAG, "actionType : " + actionType);
            LogUtils.i(Constant.TAG, "contentType : " + contentType);
            LogUtils.i(Constant.TAG, "uuid:" + contentUUID);
            if (TextUtils.isEmpty(actionType)) {
                Toast.makeText(LauncherApplication.AppContext, "ActionType值为空", Toast.LENGTH_SHORT)
                        .show();
                return null;
            }
            if (TextUtils.isEmpty(contentType)) {
                Toast.makeText(LauncherApplication.AppContext, "ContentType值为空", Toast
                        .LENGTH_SHORT)
                        .show();
                return null;
            }
            if (TextUtils.isEmpty(contentUUID)) {
                Toast.makeText(LauncherApplication.AppContext, "ContentUUID值为空", Toast
                        .LENGTH_SHORT)
                        .show();
                return null;
            }
            if (Constant.OPEN_DETAILS.equals(actionType)) { // 打开详情
                if (Constant.CONTENTTYPE_PS.equals(contentType)) { // 节目集
                    jumpIntent = new Intent(context, ProgrameSeriesAndVarietyDetailActivity.class);
                } else if (Constant.CONTENTTYPE_CR.equals(contentType)
                        || Constant.CONTENTTYPE_FG.equals(contentType)) {   //人物
//                    jumpIntent = new Intent(context, PersonsDetailsActivity.class);
                    jumpIntent = new Intent(context, PersonsDetailsActivityNew.class);
                } else if (Constant.CONTENTTYPE_CL.equals(contentType)
                        || Constant.CONTENTTYPE_TV.equals(contentType)) {  //栏目
                    jumpIntent = new Intent(context, ColumnPageActivity.class);
                } else if (Constant.CONTENTTYPE_PG.equals(contentType)) {  //单节目
                    jumpIntent = new Intent(context, SingleDetailPageActivity.class);
                } else if (Constant.CONTENTTYPE_CP.equals(contentType)) {  // 子节目
                    Bundle bundle = new Bundle();
                    bundle.putString(Constant.CONTENT_TYPE, contentType);
                    bundle.putString(Constant.CONTENT_UUID, contentUUID);
                    NewTVLauncherPlayerActivity.play(context, bundle);
                } else if (Constant.CONTENTTYPE_CG.equals(contentType)) {
//                    jumpIntent = new Intent(context, ProgramListDetailActiviy.class);
                    jumpIntent = new Intent(context, ProgramCollectionActivity.class);
                } else if (Constant.CONTENTTYPE_CS.equals(contentType)) {  //节目集合集
                    Toast.makeText(context, "节目集合集正在开发中", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, actionType + ":" + contentType, Toast.LENGTH_SHORT)
                            .show();
                }
            } else if (Constant.OPEN_LISTPAGE.equals(actionType)) { // 打开列表页
                jumpIntent = new Intent(context, ScreenListActivity.class);
            } else if (Constant.OPEN_LINK.equals(actionType)) { // 打开链接
                Toast.makeText(context, R.string.no_link, Toast.LENGTH_LONG)
                        .show();
            } else if (Constant.OPEN_USERCENTER.equals(actionType)) { // 打开用户中心
                Toast.makeText(context, R.string.not_support_direct_type, Toast.LENGTH_LONG)
                        .show();
            } else if (Constant.OPEN_APP_LIST.equals(actionType)) { // 打开我的应用
                Toast.makeText(context, R.string.not_support_direct_type, Toast.LENGTH_LONG)
                        .show();
            } else if (Constant.OPEN_SPECIAL.equals(actionType)) { // 打开专题页
                jumpIntent = new Intent(context, SpecialActivity.class);
            } else if (Constant.OPEN_APK.equals(actionType)) { // 打开apk
                Toast.makeText(context, R.string.not_support_direct_type, Toast.LENGTH_LONG)
                        .show();
            } else if (Constant.OPEN_PAGE.equals(actionType)) { // 打开apk
                Toast.makeText(context, R.string.not_support_direct_type, Toast.LENGTH_LONG)
                        .show();
            } else if (Constant.OPEN_VIDEO.equals(actionType)) { //打开视频
                // TODO 后面需要直接播放视频
                Bundle bundle = new Bundle();
                bundle.putString(Constant.CONTENT_TYPE, contentType);
                bundle.putString(Constant.CONTENT_UUID, contentUUID);
                NewTVLauncherPlayerActivity.play(context, bundle);

            }
        } catch (Exception e) {
            LogUtils.e(e);
        } finally {
            if (jumpIntent != null) {
                jumpIntent.putExtra("action_type", actionType);
                jumpIntent.putExtra("content_type", contentType);
                jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
        }
        return jumpIntent;
    }

    public static void detailsJumpActivity(Context context, String contentType,
                                           String contentUUID) {
        detailsJumpActivity(context, contentType, contentUUID, "");
    }

    //跳转详情页
    public static void detailsJumpActivity(Context context, String contentType,
                                           String contentUUID, String seriesSubUUID) {
        // fix bug LETVYSYY-51
        if (!NetworkManager.getInstance().isConnected()) {
            Toast.makeText(context, R.string.net_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent jumpIntent = null;
        if (Constant.CONTENTTYPE_PS.equals(contentType)) { // 节目集
            jumpIntent = new Intent(context, ProgrameSeriesAndVarietyDetailActivity.class);
        } else if (Constant.CONTENTTYPE_CR.equals(contentType)
                || Constant.CONTENTTYPE_FG.equals(contentType)) {   //人物
//            jumpIntent = new Intent(context, PersonsDetailsActivity.class);
            jumpIntent = new Intent(context, PersonsDetailsActivityNew.class);
        } else if (Constant.CONTENTTYPE_CL.equals(contentType)
                || Constant.CONTENTTYPE_TV.equals(contentType)) {  //栏目
            jumpIntent = new Intent(context, ColumnPageActivity.class);
        } else if (Constant.CONTENTTYPE_PG.equals(contentType)) {  //单节目
            jumpIntent = new Intent(context, SingleDetailPageActivity.class);
        } else if (Constant.CONTENTTYPE_CP.equals(contentType)) {  // 子节目
            Bundle bundle = new Bundle();
            bundle.putString(Constant.CONTENT_TYPE, contentType);
            bundle.putString(Constant.CONTENT_UUID, contentUUID);
            NewTVLauncherPlayerActivity.play(context, bundle);
        } else if (Constant.CONTENTTYPE_CG.equals(contentType)) {
//            jumpIntent = new Intent(context, ProgramListDetailActiviy.class);
            jumpIntent = new Intent(context, ProgramCollectionActivity.class);
        } else if (Constant.CONTENTTYPE_CS.equals(contentType)) {  //节目集合集
            Toast.makeText(context, "节目集合集正在开发中", Toast.LENGTH_SHORT).show();
        } else {

        }

        if (jumpIntent != null) {
            jumpIntent.putExtra("content_type", contentType);
            jumpIntent.putExtra("content_uuid", contentUUID);
            jumpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityCompat.startActivity(context, jumpIntent, null);
        }
    }


    private static Context getMainContext(Context context) {
        Context tmpContext = context;
        if (MainNavManager.getInstance().getCurrentFragment() != null) {
            tmpContext = MainNavManager.getInstance().getCurrentFragment().getActivity();
        }
        return tmpContext;
    }
}
