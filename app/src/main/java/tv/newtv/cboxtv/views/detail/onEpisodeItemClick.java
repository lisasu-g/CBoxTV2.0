package tv.newtv.cboxtv.views.detail;

import com.newtv.cms.bean.SubContent;

import tv.newtv.cboxtv.player.ProgramSeriesInfo;

/**
 * 项目名称:         CBoxTV
 * 包名:            tv.newtv.cboxtv.views.detailpage
 * 创建事件:         13:25
 * 创建人:           weihaichao
 * 创建日期:          2018/7/30
 */
public interface onEpisodeItemClick<T> {
    boolean onItemClick(int position,T data);
}
