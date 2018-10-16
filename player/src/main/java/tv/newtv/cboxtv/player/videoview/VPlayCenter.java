package tv.newtv.cboxtv.player.videoview;

import android.text.TextUtils;

import com.newtv.cms.bean.Content;
import com.newtv.cms.bean.SubContent;
import com.newtv.libs.Constant;
import com.newtv.libs.util.LogUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 项目名称:         CBoxTV
 * 包名:            tv.newtv.cboxtv.player.videoview
 * 创建事件:         09:24
 * 创建人:           weihaichao
 * 创建日期:          2018/4/30
 */
public class VPlayCenter {

    public static final int PLAY_SINGLE = 0;
    public static final int PLAY_SERIES = 1;

    private List<Content> programSeriesInfo;
    private int currentIndex = 0;
    private int currentType;

    VPlayCenter() {
    }

    public int getCurrentType() {
        return currentType;
    }

    public boolean isReady() {
        if (programSeriesInfo == null) return false;
        return programSeriesInfo.size() > 0;
    }

    /**
     * @param index
     * @return
     */
    private DataStruct getDataStruct(int index) {
        Content seriesInfo = getCurrentSeriesInfo();
        DataStruct dataStruct = null;
        if (seriesInfo != null) {
            try {
                if (Constant.CONTENTTYPE_PG.equals(seriesInfo.getContentType()) || Constant
                        .CONTENTTYPE_CP.equals(seriesInfo.getContentType())) {
                    dataStruct = new DataStruct();
                    dataStruct.title = seriesInfo.getTitle();
                    dataStruct.uuid = seriesInfo.getContentUUID();
                    dataStruct.img = seriesInfo.getHImage();
                    dataStruct.playType = PLAY_SINGLE;
                    currentType = PLAY_SINGLE;
                } else {
                    if (seriesInfo.getData() != null && seriesInfo.getData().size() > index) {
                        dataStruct = new DataStruct();
                        SubContent programsInfo = seriesInfo.getData().get(index);
                        dataStruct.title = programsInfo.getTitle();
                        dataStruct.uuid = programsInfo.getContentUUID();
                        dataStruct.img = programsInfo.getHImage();
                        dataStruct.albumId = seriesInfo.getContentUUID();
                        dataStruct.playType = PLAY_SERIES;
                        currentType = PLAY_SERIES;
                    }
                }
            } catch (Exception e) {
                LogUtils.e(e.toString());
            }
        }
        return dataStruct;
    }

    public DataStruct getDataStruct() {
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        return getDataStruct(currentIndex);
    }

    /**
     *
     */
    public void playComplete() {
        currentIndex += 1;
    }

    /**
     * @return
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        currentIndex = index;
    }

    /**
     * 是否还有下一集
     *
     * @return
     */
    public boolean hasNext() {
        if (currentType == PLAY_SINGLE) return false;
        DataStruct dataStruct = getDataStruct(currentIndex);
        if (dataStruct == null) {
            return false;
        }
        return true;
    }

    boolean hasNext(int index) {
        if (currentType == PLAY_SINGLE) return false;
        DataStruct dataStruct = getDataStruct(index);
        if (dataStruct == null) {
            return false;
        }
        return true;
    }

    /**
     * 获取当前播放源
     *
     * @return
     */
    public Content getCurrentSeriesInfo() {
        if (programSeriesInfo != null && programSeriesInfo.size() > 0) {
            return programSeriesInfo.get(0);
        }
        return null;
    }

    /**
     * 添加播放源
     *
     * @param seriesInfo
     */
    public void addSeriesInfo(Content seriesInfo) {
        if (seriesInfo == null) {
            return;
        }

        if (programSeriesInfo == null) {
            programSeriesInfo = new ArrayList<>();
        }
        currentIndex = 0;
        programSeriesInfo.clear();
        programSeriesInfo.add(seriesInfo);
    }

    public Content getCurrentProgramSeriesInfo() {
        if (programSeriesInfo == null) return null;
        if (programSeriesInfo.size() <= currentIndex) return null;
        return programSeriesInfo.get(currentIndex);
    }

    /**
     * 播放信息结构体
     */
    public static class DataStruct {

        public String title = "";
        public String uuid = "";
        public String albumId = "";
        public String img = "";
        public int playType;


        public boolean isSingle() {
            return TextUtils.isEmpty(albumId);
        }
    }
}
