package tv.newtv.cboxtv.views.detail;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.newtv.cms.bean.SubContent;
import com.newtv.libs.ad.ADHelper;

import java.util.List;

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.views.detail
 * 创建事件:         15:41
 * 创建人:           weihaichao
 * 创建日期:          2018/10/25
 */
public abstract class AbsEpisodeFragment extends Fragment {


    protected View contentView;

    protected View findViewById(int id){
        if(contentView != null){
            return contentView.findViewById(id);
        }
        return null;
    }

    protected void postDelayed(Runnable runnable,int delay){
        if(contentView != null){
            contentView.postDelayed(runnable,delay);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if(contentView == null){
            contentView = createView(inflater, container, savedInstanceState);
        }
        if(contentView.getParent() != null){
            ViewGroup viewGroup = (ViewGroup) contentView.getParent();
            viewGroup.removeView(contentView);
        }
        return contentView;
    }

    protected abstract View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                  @Nullable Bundle savedInstanceState);
    public abstract void setAdItem(ADHelper.AD.ADItem adItem);
    public abstract int getPageSize();
    public void destroy(){
        contentView = null;
    }
    public abstract void clear();
    public abstract void setViewPager(ResizeViewPager viewPager, int position, EpisodeChange
            change);
    public abstract int getCurrentIndex();
    public abstract void requestDefaultFocus();
    public abstract void setSelectIndex(final int index);
    public abstract void setData(List<SubContent> data);
    public abstract void requestFirst();
    public abstract void requestLast();
    public abstract String getTabString(int index,int endIndex);


}
