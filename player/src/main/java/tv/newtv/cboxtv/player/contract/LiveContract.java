package tv.newtv.cboxtv.player.contract;

import android.content.Context;
import android.text.TextUtils;

import com.newtv.cms.CmsServicePresenter;
import com.newtv.cms.DataObserver;
import com.newtv.cms.ICmsPresenter;
import com.newtv.cms.ICmsView;
import com.newtv.cms.api.IPlayChk;
import com.newtv.cms.bean.ChkRequest;
import com.newtv.cms.bean.Content;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.util.Encryptor;
import com.newtv.libs.util.GsonUtil;
import com.newtv.libs.util.LogUtils;
import com.newtv.libs.util.NetworkManager;
import com.newtv.libs.util.SharePreferenceUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import tv.newtv.cboxtv.player.PlayerErrorCode;
import tv.newtv.cboxtv.player.model.LiveInfo;
import tv.newtv.cboxtv.player.model.LivePermissionCheckBean;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.player.contract
 * 创建事件:         17:16
 * 创建人:           weihaichao
 * 创建日期:          2018/10/12
 */
public class LiveContract {

    public interface View extends ICmsView {
        void liveChkResult(LiveInfo checkBean);

        void onChkError(String code, String desc);
    }

    public interface Presenter extends ICmsPresenter {
        void checkLive(LiveInfo liveInfo);

        boolean isInLive(Content content);
    }

    public static class LivePresenter extends CmsServicePresenter<View> implements Presenter {

        private static final String TAG = LivePresenter.class.getSimpleName();

        public LivePresenter(@NotNull Context context, @NotNull View view) {
            super(context, view);
        }

        ChkRequest createLiveRequestBean(String contentUUID, String liveUrl) {
            ChkRequest playCheckRequestBean = new ChkRequest();
            playCheckRequestBean.setAppKey(Libs.get().getAppKey());
            playCheckRequestBean.setChannelId(Libs.get().getChannelId());
            playCheckRequestBean.setSource("NEWTV");
            playCheckRequestBean.setId(contentUUID);
            playCheckRequestBean.setPid(liveUrl);

            return playCheckRequestBean;
        }

        @Override
        public void checkLive(final LiveInfo liveInfo) {
            final IPlayChk playChk = getService(SERVICE_CHK_PLAY);
            String token = SharePreferenceUtils.getToken(getContext());
            String resultToken = TextUtils.isEmpty(token) ? "" : "Bearer " + token;
            if (playChk != null) {
                ChkRequest request = createLiveRequestBean(liveInfo.getContentUUID(), liveInfo
                        .getLiveUrl());
                playChk.check(request, resultToken, new DataObserver<String>() {
                    @Override
                    public void onError(@NotNull String code, @Nullable String desc) {
                        LogUtils.e(TAG, "onFailure: " + desc);
                        if (!NetworkManager.getInstance().isConnected()) {
                            if (getView() != null)
                                getView().onChkError(PlayerErrorCode.INTERNET_ERROR,
                                        PlayerErrorCode.getErrorDesc(getContext(),
                                                PlayerErrorCode
                                                        .PERMISSION_CHECK_RESULT_EMPTY));
                        } else {
                            if (getView() != null)
                                getView().onChkError(PlayerErrorCode.PERMISSION_CHECK_RESULT_EMPTY,
                                        PlayerErrorCode.getErrorDesc(getContext(), PlayerErrorCode
                                                .PERMISSION_CHECK_RESULT_EMPTY));
                        }
                    }

                    @Override
                    public void onResult(String result, long requestCode) {
                        if (TextUtils.isEmpty(result)) {
                            LogUtils.i(TAG, "onResponse: responseBody==null");
                            if (!NetworkManager.getInstance().isConnected()) {
                                if (getView() != null)
                                    getView().onChkError(PlayerErrorCode.INTERNET_ERROR,
                                            PlayerErrorCode.getErrorDesc(getContext(),
                                                    PlayerErrorCode
                                                            .PERMISSION_CHECK_RESULT_EMPTY));
                            } else {
                                if (getView() != null)
                                    getView().onChkError(PlayerErrorCode
                                                    .PERMISSION_CHECK_RESULT_EMPTY,
                                            PlayerErrorCode.getErrorDesc(getContext(),
                                                    PlayerErrorCode
                                                            .PERMISSION_CHECK_RESULT_EMPTY));
                            }
                            LogUtils.e(TAG, "调用鉴权接口后没有返回数据");
                            return;
                        }
                        LivePermissionCheckBean livePermissionCheck = GsonUtil.fromjson(result,
                                LivePermissionCheckBean.class);
                        if ("0".equals(livePermissionCheck.getErrorCode())) {
                            if (livePermissionCheck.getData() != null && livePermissionCheck
                                    .getData()
                                    .isEncryptFlag()) {
                                liveInfo.setKey(Encryptor.decrypt(Constant.APPSECRET,
                                        livePermissionCheck.getData()
                                                .getDecryptKey()));
                            }
                            if (getView() != null)
                                getView().liveChkResult(liveInfo);
                        } else {
                            if (getView() != null)
                                getView().onChkError(livePermissionCheck.getErrorCode(),
                                        livePermissionCheck.getErrorMessage());
                        }

                    }
                });
            }
        }

        @Override
        public boolean isInLive(Content content) {

            return false;
        }
    }
}
