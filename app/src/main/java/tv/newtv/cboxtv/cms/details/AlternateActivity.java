package tv.newtv.cboxtv.cms.details;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.newtv.cms.bean.Alternate;
import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.SubContent;
import com.newtv.cms.contract.ContentContract;
import com.newtv.libs.Constant;
import com.newtv.libs.util.ToastUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import tv.newtv.cboxtv.JumpScreen;
import tv.newtv.cboxtv.R;
import tv.newtv.cboxtv.views.custom.AlternateView;
import tv.newtv.cboxtv.views.detail.AlterHeaderView;
import tv.newtv.cboxtv.views.detail.DetailPageActivity;
import tv.newtv.cboxtv.views.detail.EpisodeHorizontalListView;
import tv.newtv.cboxtv.views.detail.onEpisodeItemClick;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv
 * 创建事件:         12:50
 * 创建人:           weihaichao
 * 创建日期:          2018/11/13
 */
public class AlternateActivity extends DetailPageActivity implements AlternateView
        .AlternateCallback, onEpisodeItemClick<Alternate>, ContentContract.LoadingView {
    private String contentUUID;
    private AlterHeaderView headerView;
    private EpisodeHorizontalListView mPlayListView;

    private String currentUUID = "";
    private long currentRequest = 0L;
    private ContentContract.Presenter mPresenter;

    @Override
    public void prepareMediaPlayer() {
        super.prepareMediaPlayer();

        if (headerView != null) {
            headerView.prepareMediaPlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (headerView != null) {
            headerView.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (headerView != null) {
            headerView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        headerView = null;
    }

    @Override
    public boolean hasPlayer() {
        return true;
    }

    @Override
    protected void buildView(@Nullable Bundle savedInstanceState, String contentID) {
        setContentView(R.layout.activity_alternate_layout);
        contentUUID = contentID;

        if (TextUtils.isEmpty(contentUUID)) {
            ToastUtil.showToast(getApplicationContext(), "节目ID为空");
            finish();
            return;
        }
        setUp();
    }

    @Override
    protected boolean interruptDetailPageKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void setUp() {
        headerView = findViewById(R.id.header_view);
        mPlayListView = findViewById(R.id.play_list);
        mPlayListView.setOnItemClick(this);

        headerView.setCallback(this);
        headerView.setContentUUID(contentUUID);
    }

    @Override
    public void onAlternateResult(@org.jetbrains.annotations.Nullable List<Alternate> result) {
        if (mPlayListView != null) {
            mPlayListView.onAlternateResult(result);
        }
    }

    @Override
    public void onPlayIndexChange(int index) {
        if (mPlayListView != null) {
            mPlayListView.setCurrentPlay(index);
        }
    }

    @Override
    public boolean onItemClick(int position, Alternate data) {
        if (Constant.CONTENTTYPE_PG.equals(data.getContentType())) {
            JumpScreen.jumpActivity(getApplicationContext(), data.getContentID(), Constant
                    .OPEN_DETAILS, Constant.CONTENTTYPE_PG);
        } else {
            if (mPresenter == null) {
                mPresenter = new ContentContract.ContentPresenter(getApplicationContext(), this);
            }
            if (currentRequest != 0L) {
                mPresenter.cancel(currentRequest);
                currentRequest = 0L;
            }
            currentUUID = data.getContentID();
            currentRequest = mPresenter.getContent(data.getContentID(), false);
        }
        return true;
    }

    private String getFormatId(String ids) {
        String[] idArr = ids.split("\\|");
        if (idArr.length >= 1) {
            return idArr[0];
        }
        return "";
    }

    @Override
    public void onContentResult(@NotNull String uuid, @Nullable Content content) {
        if (content != null && TextUtils.equals(currentUUID, uuid)) {
            String tvIds = content.getTvContentIDs();//电视栏目ID
            String csIds = content.getCsContentIDs();//节目集ID
            String cgIds = content.getCgContentIDs();//节目合集ID
            if (!TextUtils.isEmpty(tvIds)) {
                //打开电视栏目详情页
                JumpScreen.jumpActivity(getApplicationContext(),
                        getFormatId(tvIds),
                        Constant.OPEN_DETAILS,
                        Constant.CONTENTTYPE_TV, content.getContentUUID());
            } else if (!TextUtils.isEmpty(csIds)) {
                //打开节目集详情页
                JumpScreen.jumpActivity(getApplicationContext(),
                        getFormatId(csIds),
                        Constant.OPEN_DETAILS,
                        Constant.CONTENTTYPE_PS, content.getContentUUID());
            } else if (!TextUtils.isEmpty(cgIds)) {
                //打开节目合集详情页
                JumpScreen.jumpActivity(getApplicationContext(),
                        getFormatId(cgIds),
                        Constant.OPEN_DETAILS,
                        Constant.CONTENTTYPE_CG, content.getContentUUID());
            }
            currentRequest = 0L;
            currentUUID = "";
        }
    }

    @Override
    public void onSubContentResult(@NotNull String uuid, @org.jetbrains.annotations.Nullable
            ArrayList<SubContent> result) {

    }

    @Override
    public void onLoading() {

    }

    @Override
    public void loadComplete() {

    }

    @Override
    public void tip(@NotNull Context context, @NotNull String message) {

    }

    @Override
    public void onError(@NotNull Context context, @org.jetbrains.annotations.Nullable String desc) {

    }
}