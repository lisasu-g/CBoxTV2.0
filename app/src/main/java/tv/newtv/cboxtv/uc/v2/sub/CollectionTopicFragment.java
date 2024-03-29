package tv.newtv.cboxtv.uc.v2.sub;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.Program;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.db.DBCallback;
import com.newtv.libs.db.DBConfig;
import com.newtv.libs.db.DataSupport;
import com.newtv.libs.util.SharePreferenceUtils;
import com.newtv.libs.util.SystemUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.cms.mainPage.model.ModuleInfoResult;
import tv.newtv.cboxtv.cms.mainPage.model.ModuleItem;
import tv.newtv.cboxtv.cms.net.NetClient;
import tv.newtv.cboxtv.cms.util.ModuleUtils;
import tv.newtv.cboxtv.uc.bean.UserCenterPageBean;
import tv.newtv.cboxtv.uc.v2.BaseDetailSubFragment;
import tv.newtv.cboxtv.uc.v2.TokenRefreshUtil;

/**
 * 项目名称:         央视影音
 * 包名:            tv.newtv.tvlauncher
 * 创建时间:         下午2:14
 * 创建人:           lixin
 * 创建日期:         2018/9/24
 */


public class CollectionTopicFragment extends BaseDetailSubFragment {
    private final String TAG = "CollectionTopicFragment";
    private RecyclerView mRecyclerView;
    private RecyclerView mHotRecommendRecyclerView;
    private TextView emptyTextView;
    private List<UserCenterPageBean.Bean> mDatas;
    private String mLoginTokenString;//登录token,用于判断登录状态
    private String userId;
    private UserCenterUniversalAdapter mAdapter;
    private final int COLUMN_COUNT = 6;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_history_record;
    }

    @Override
    public void onResume() {
        super.onResume();
        //获取用户登录状态
        requestUserInfo();
    }

    @Override
    protected void updateUiWidgets(View view) {
    }

    //获取用户登录状态
    private void requestUserInfo() {
        Observable.create(new ObservableOnSubscribe<String>() {
            @SuppressLint("LongLogTag")
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                boolean status = TokenRefreshUtil.getInstance().isTokenRefresh(getActivity());
                Log.d(TAG, "---isTokenRefresh:status:" + status);
                //获取登录状态
                mLoginTokenString = SharePreferenceUtils.getToken(getActivity().getApplicationContext());
                if (!TextUtils.isEmpty(mLoginTokenString)) {
                    userId = SharePreferenceUtils.getUserId(getActivity().getApplicationContext());
                    e.onNext(mLoginTokenString);
                } else {
                    userId = SystemUtils.getDeviceMac(getActivity().getApplicationContext());
                    e.onNext("");
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {

                    @Override
                    public void accept(String value) throws Exception {
                        requestData();
                    }
                });
    }

    //读取数据库中数据
    private void requestData() {
        //收藏数据表表名
        String tableNameCollect = DBConfig.COLLECT_TABLE_NAME;
        if (!TextUtils.isEmpty(mLoginTokenString)) {
            tableNameCollect = DBConfig.REMOTE_COLLECT_TABLE_NAME;
        }
        DataSupport.search(tableNameCollect)
                .condition()
                .eq(DBConfig.CONTENTTYPE, "SA")
                .eq(DBConfig.USERID, userId)
                .OrderBy(DBConfig.ORDER_BY_TIME)
                .build()
                .withCallback(new DBCallback<String>() {
                    @Override
                    public void onResult(int code, String result) {
                        if (code == 0) {
                            UserCenterPageBean userCenterUniversalBean = new UserCenterPageBean("");
                            Gson gson = new Gson();
                            Type type = new TypeToken<List<UserCenterPageBean.Bean>>() {}.getType();
                            List<UserCenterPageBean.Bean> universalBeans = gson.fromJson(result, type);
//                            List<UserCenterPageBean.Bean> beanList = new ArrayList<>();
//                            if (universalBeans != null && universalBeans.size() > 0) {
//                                for (int i = 0; i < universalBeans.size(); i++) {
//                                    UserCenterPageBean.Bean bean = null;
//                                    bean = universalBeans.get(i);
//                                    String contentType = "";
//                                    if (bean != null) {
//                                        contentType = bean.get_contenttype();
//                                    }
//                                    if (TextUtils.equals(contentType, "SA")) {
//                                        beanList.add(bean);
//                                    }
//                                }
//                            }
                            userCenterUniversalBean.data = universalBeans;
                            if (userCenterUniversalBean.data != null && userCenterUniversalBean.data.size() > 0) {
                                inflatePage(userCenterUniversalBean);
                            } else {
                                inflatePageWhenNoData();
                            }
                        } else {
                            inflatePageWhenNoData();
                        }
                    }
                }).excute();
    }

    private void inflatePage(UserCenterPageBean bean) {
        if (contentView == null) {
            return;
        }

        if (bean == null || bean.data == null || bean.data.size() == 0) {
            inflatePageWhenNoData();
            return;
        }

        if (mDatas == null) {
            mDatas = bean.data;
            mRecyclerView = contentView.findViewById(R.id.id_history_record_rv);
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 6));
            mAdapter = new UserCenterUniversalAdapter(getActivity(), mDatas, Constant.UC_COLLECTION);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.bottom = 72;
                    int index = parent.getChildLayoutPosition(view);
                    if (index < COLUMN_COUNT) {
                        outRect.top = 23;
                    }
                }
            });
        } else {
            if (mDatas != null && mAdapter != null) {
                mDatas.clear();
                mDatas.addAll(bean.data);
                mAdapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void inflatePageWhenNoData() {
        mRecyclerView = contentView.findViewById(R.id.id_history_record_rv);
        mRecyclerView.setVisibility(View.INVISIBLE);

        showEmptyTip();
        showHotRecommend();
    }

    /**
     * 展示无数据提示
     */
    private void showEmptyTip() {
        ViewStub emptyViewStub = contentView.findViewById(R.id.id_empty_view_vs);
        if (emptyViewStub != null) {
            View emptyView = emptyViewStub.inflate();
            if (emptyView != null) {
                if (emptyTextView == null) {
                    emptyTextView = emptyView.findViewById(R.id.empty_textview);
                    emptyTextView.setText("您还没有收藏任何节目哦～");
                    emptyTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * 展示热门订阅数据
     */
    private void showHotRecommend() {
        NetClient.INSTANCE.getHotSubscribeApi()
                .getHotSubscribeInfo(Libs.get().getAppKey(), Libs.get().getChannelId(), "489")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody result) {
                        try {
                            ModuleInfoResult infoResult = ModuleUtils.getInstance().parseJsonForModuleInfo(result.string());
                            if (infoResult == null) {
                                return;
                            }

                            List<ModuleItem> moduleItems = infoResult.getDatas();
                            List<Program> programInfos = moduleItems.get(0).getDatas();

                            ViewStub viewStub = contentView.findViewById(R.id.id_hot_recommend_area_vs);
                            if (viewStub != null) {
                                View view = viewStub.inflate();

                                if (view != null) {
                                    mHotRecommendRecyclerView = view.findViewById(R.id.id_hot_recommend_area_rv);
                                    mHotRecommendRecyclerView.setHasFixedSize(true);
                                    mHotRecommendRecyclerView.setItemAnimator(null);
                                    mHotRecommendRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false) {
                                        @Override
                                        public boolean canScrollHorizontally() {
                                            return false;
                                        }
                                    });
                                    mHotRecommendRecyclerView.setAdapter(new HotRecommendAreaAdapter(getActivity(), programInfos));
                                    mHotRecommendRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                                        @Override
                                        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                                            int index = parent.getChildLayoutPosition(view);
                                            if (index < COLUMN_COUNT) {
                                                outRect.top = 23;
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
