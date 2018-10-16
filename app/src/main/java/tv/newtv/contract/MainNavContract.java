package tv.newtv.contract;

import android.content.Context;

import com.newtv.cms.BuildConfig;
import com.newtv.cms.CmsServicePresenter;
import com.newtv.cms.DataObserver;
import com.newtv.cms.ICmsPresenter;
import com.newtv.cms.ICmsView;
import com.newtv.cms.api.INav;
import com.newtv.cms.bean.ModelResult;
import com.newtv.cms.bean.Nav;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.cms.mainPage.menu
 * 创建事件:         15:28
 * 创建人:           weihaichao
 * 创建日期:          2018/9/27
 */
public class MainNavContract {
    public interface View extends ICmsView {
        void onNavResult(Context context, List<Nav> result);
    }

    public interface Presenter extends ICmsPresenter {
        void requestNav();
    }

    public static class MainNavPresenter extends CmsServicePresenter<View> implements Presenter {

        public MainNavPresenter(Context context, View view) {
            super(context, view);
        }

        @Override
        public void requestNav() {
            INav nav = getService(SERVICE_NAV);
            if (nav != null) {
                nav.getNav(BuildConfig.APP_KEY, BuildConfig.CHANNEL_ID,
                        new DataObserver<ModelResult<List<Nav>>>() {
                            @Override
                            public void onResult(ModelResult<List<Nav>> result) {
                                if (result.isOk()) {
                                    getView().onNavResult(getContext(), result.getData());
                                } else {
                                    onError(result.getErrorMessage());
                                }
                            }

                            @Override
                            public void onError(@Nullable String desc) {
                                getView().onError(getContext(), desc);
                            }
                        });
            }
        }

    }


}