package tv.newtv.cboxtv.uc.v2.sub;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.newtv.cms.bean.Page;
import com.newtv.cms.bean.Program;
import com.newtv.cms.contract.PageContract;
import com.newtv.libs.BootGuide;
import com.newtv.libs.Constant;
import com.newtv.libs.Libs;
import com.newtv.libs.db.DBCallback;
import com.newtv.libs.db.DBConfig;
import com.newtv.libs.db.DataSupport;
import com.newtv.libs.util.LogUploadUtils;
import com.newtv.libs.util.RxBus;
import com.newtv.libs.util.SharePreferenceUtils;
import com.newtv.libs.util.SystemUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import tv.newtv.cboxtv.LauncherApplication;
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
 * 创建时间:         下午4:41
 * 创建人:           lixin
 * 创建日期:         2018/9/11
 */


public class SubscribeFragment extends BaseDetailSubFragment implements PageContract.View {
    private final String TAG = "lx";
    private RecyclerView mRecyclerView; // 展示订阅列表的recyclerview
    private RecyclerView mHotRecommendRecyclerView; // 展示热门订阅的recyclerview
    private TextView mHotRecommendTitle;
    private List<UserCenterPageBean.Bean> mDatas;
    private TextView emptyTextView;
    private String mLoginTokenString;//登录token,用于判断登录状态
    private String userId;

    private UserCenterUniversalAdapter mAdapter;
    private final int COLUMN_COUNT = 6;
    private PageContract.ContentPresenter mContentPresenter;

    private List<UserCenterPageBean.Bean> localData;
    private List<UserCenterPageBean.Bean> remoteData;
    private boolean localDataReqComp;
    private boolean remoteDataReqComp;

    private static final int MSG_SYNC_DATA_COMP = 10033;
    private static final int MSG_INFLATE_PAGE = 10034;

    private static SubscribeHandler mHandler;
    private int move = -1;
    private Observable<Integer> observable;

    private ImageView mHotRecommendTitleIcon;
    private View mHotRecommendArea;

    private Observable<Map<String, String>> operationObs;
    private String operationType;
    private String operationId;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_history_record;
    }

    @Override
    public void onCreate(@android.support.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new SubscribeHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        observable = RxBus.get().register("recordPosition");
        observable.observeOn(AndroidSchedulers .mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        move = integer;
                    }
                });

        operationObs = RxBus.get().register("operation_param");
        operationObs.observeOn(AndroidSchedulers .mainThread())
                .subscribe(new Consumer<Map<String, String>>() {
                    @Override
                    public void accept(Map<String, String> map) throws Exception {
                        operationType = map.get("operation_type");
                        operationId = map.get("operation_id");
                        Log.d(TAG, "type : " + operationType + ", id : " + operationId);
                    }
                });

        Log.e(TAG, "---onResume");
        //订阅页面上报日志
        LogUploadUtils.uploadLog(Constant.LOG_NODE_USER_CENTER, "3,3");
        //获取登录状态
        requestUserInfo();
    }

    @Override
    protected void updateUiWidgets(View view) {
        Log.e(TAG, "---updateUiWidgets");
    }

    //获取用户登录状态
    private void requestUserInfo() {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                boolean status = TokenRefreshUtil.getInstance().isTokenRefresh(getActivity());
                Log.d(TAG, "---isTokenRefresh:status:" + status);
                //获取登录状态
                mLoginTokenString = SharePreferenceUtils.getToken(getActivity());
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
        localDataReqComp = false;
        remoteDataReqComp = false;

        Log.d("sub", "requestData");
        if (!TextUtils.isEmpty(mLoginTokenString)) {
            if (SharePreferenceUtils.getSyncStatus(LauncherApplication.AppContext) == 0) {
                DataSupport.search(DBConfig.SUBSCRIBE_TABLE_NAME)
                        .condition()
                        .eq(DBConfig.USERID, SystemUtils.getDeviceMac(LauncherApplication.AppContext))
                        .OrderBy(DBConfig.ORDER_BY_TIME)
                        .build()
                        .withCallback(new DBCallback<String>() {
                            @Override
                            public void onResult(int code, final String result) {
                                if (code == 0) {
                                    Gson gson = new Gson();
                                    Type type = new TypeToken<List<UserCenterPageBean.Bean>>() {}.getType();
                                    localData = gson.fromJson(result, type);

                                    if (localData == null) {
                                        localData = new ArrayList<>(Constant.BUFFER_SIZE_8);
                                    }
                                }

                                Log.d("sub", "本地数据库查询完毕");
                                localDataReqComp = true;
                                if (mHandler != null) {
                                    mHandler.sendEmptyMessage(MSG_SYNC_DATA_COMP);
                                }
                            }
                        }).excute();

                DataSupport.search(DBConfig.REMOTE_SUBSCRIBE_TABLE_NAME)
                        .condition()
                        .eq(DBConfig.USERID, SharePreferenceUtils.getUserId(LauncherApplication.AppContext))
                        .OrderBy(DBConfig.ORDER_BY_TIME)
                        .build()
                        .withCallback(new DBCallback<String>() {
                            @Override
                            public void onResult(int code, final String result) {
                                if (code == 0) {
                                    Gson gson = new Gson();
                                    Type type = new TypeToken<List<UserCenterPageBean.Bean>>() {}.getType();
                                    remoteData = gson.fromJson(result, type);

                                    if (remoteData == null) {
                                        remoteData = new ArrayList<>(Constant.BUFFER_SIZE_8);
                                    }
                                }

                                Log.d("sub", "远程数据库查询完毕");

                                remoteDataReqComp = true;
                                if (mHandler != null) {
                                    mHandler.sendEmptyMessage(MSG_SYNC_DATA_COMP);
                                }
                            }
                        }).excute();
            } else {
                requestDataByDB(DBConfig.REMOTE_SUBSCRIBE_TABLE_NAME);
            }
        } else {
            requestDataByDB(DBConfig.SUBSCRIBE_TABLE_NAME);
        }
    }


    private boolean isSameItem(UserCenterPageBean.Bean item, List<UserCenterPageBean.Bean> datas) {
        for (UserCenterPageBean.Bean comp : datas) {
            if (TextUtils.equals(comp.get_contentuuid(), item.get_contentuuid())) {
                return true;
            }
        }
        return false;
    }

    private void requestDataByDB(String tableName) {
        Log.d("sub", "requestDataByDB tableName : " + tableName + ", userId : " + userId);
        DataSupport.search(tableName)
                .condition()
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
                            userCenterUniversalBean.data = universalBeans;
                            if (userCenterUniversalBean.data != null && userCenterUniversalBean.data.size() > 0) {
                                inflatePage(userCenterUniversalBean.data);
                            } else {
                                inflatePageWhenNoData();
                            }
                        } else {
                            inflatePageWhenNoData();
                        }
                    }
                }).excute();
    }

    private void inflatePage(List<UserCenterPageBean.Bean> bean) {
        Log.d("sub", "inflatePage");
        if (contentView == null) {
            return;
        }

        Log.d("sub", "size : " + bean.size());

        if (bean == null || bean.size() == 0) {
            inflatePageWhenNoData();
            return;
        }

        hideView(emptyTextView);
        hideView(mHotRecommendTitle);
        hideView(mHotRecommendTitleIcon);
        hideView(mHotRecommendRecyclerView);

        showView(mRecyclerView);

        if (mDatas == null) {
            mDatas = bean;
            mRecyclerView = contentView.findViewById(R.id.id_history_record_rv);
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), COLUMN_COUNT));
            mAdapter = new UserCenterUniversalAdapter(getActivity(), mDatas, Constant.UC_SUBSCRIBE);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    int index = parent.getChildLayoutPosition(view);
                    if (index < COLUMN_COUNT) {
                        outRect.top = 23;
                    }

                    outRect.bottom = 72;
                }
            });
        } else {
            if (mAdapter != null) {
                if (TextUtils.equals(operationType, "delete")) {
                    boolean refresh = (bean.size() == mDatas.size());
                    mAdapter.setRefresh(refresh);
                    mDatas.clear();
                    mDatas.addAll(bean);
                    if (!refresh) {
                        mAdapter.notifyItemRemoved(move);
                    }

                } else if (TextUtils.equals(operationType, "add")) {
                    mDatas.clear();
                    mDatas.addAll(bean);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }


    private void inflatePageWhenNoData() {
        hideView(mRecyclerView);
        showEmptyTip();
        String hotRecommendParam = BootGuide.getBaseUrl(BootGuide.PAGE_SUBSCRIPTION);
        if (!TextUtils.isEmpty(hotRecommendParam)) {
            mContentPresenter = new PageContract.ContentPresenter(getActivity(), this);
            mContentPresenter.getPageContent(hotRecommendParam);
        } else {
            Log.e(TAG, "wqs:PAGE_SUBSCRIPTION==null");
        }
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
                    emptyTextView.setText("您还没有订阅任何节目哦～");
                    emptyTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    @Override
    public void onPageResult(@Nullable List<Page> page) {
        try {
            if (page == null && page.size() <= 0) {
                return;
            }
            List<Program> programInfos = page.get(0).getPrograms();

            ViewStub viewStub = contentView.findViewById(R.id.id_hot_recommend_area_vs);
            if (viewStub != null) {
                View view = viewStub.inflate();

                if (view != null) {
                    mHotRecommendArea = view;
                    mHotRecommendTitleIcon = mHotRecommendArea.findViewById(R.id.id_hot_recommend_area_icon);
                    mHotRecommendTitle = mHotRecommendArea.findViewById(R.id.id_hot_recommend_area_title);
                    mHotRecommendTitle.setText(page.get(0).getBlockTitle());
                    mHotRecommendRecyclerView = mHotRecommendArea.findViewById(R.id.id_hot_recommend_area_rv);
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

            showView(mHotRecommendTitle);
            showView(mHotRecommendTitleIcon);
            showView(mHotRecommendRecyclerView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tip(@NotNull Context context, @NotNull String message) {

    }

    @Override
    public void onError(@NotNull Context context, @Nullable String desc) {

    }

    @Override
    public void startLoading() {

    }

    @Override
    public void loadingComplete() {

    }

    private void checkDataSync() {
        Log.d("sub", "checkDataSync");
        if (remoteDataReqComp && localDataReqComp) {
            mHandler.removeMessages(MSG_SYNC_DATA_COMP);

            List<UserCenterPageBean.Bean> subscribeRecords = new ArrayList<>();
            List<UserCenterPageBean.Bean> temp = new ArrayList<>(Constant.BUFFER_SIZE_16);
            temp.addAll(remoteData);
            temp.addAll(localData);

            for (UserCenterPageBean.Bean item : temp) {
                if (!isSameItem(item, subscribeRecords)) {
                    subscribeRecords.add(item);
                }
            }

            Message msg = Message.obtain();
            msg.what = MSG_INFLATE_PAGE;
            msg.obj = subscribeRecords;
            mHandler.sendMessage(msg);
        } else {
            mHandler.sendEmptyMessageDelayed(MSG_SYNC_DATA_COMP, 100);
        }
    }

    class SubscribeHandler extends android.os.Handler {
        WeakReference<SubscribeFragment> reference;

        SubscribeHandler(SubscribeFragment setFragment) {
            reference = new WeakReference<>(setFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SYNC_DATA_COMP) {
                checkDataSync();
            } else if (msg.what == MSG_INFLATE_PAGE) {
                Log.d("sub", "接收到 MSG_INFLATE_PAGE 消息");
                List<UserCenterPageBean.Bean> datas = (List<UserCenterPageBean.Bean>) msg.obj;
                if (datas != null && datas.size() > 0) {
                    inflatePage(datas);
                } else {
                    inflatePageWhenNoData();
                }
            } else {
                Log.d("sub", "unresolved msg : " + msg.what);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxBus.get().unregister("recordPosition",observable);
        RxBus.get().unregister("operation_param", operationObs);
    }

    private void hideView(View view) {
        if (view != null) {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private void showView(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }
}
