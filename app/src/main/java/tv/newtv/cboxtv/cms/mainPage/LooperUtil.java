package tv.newtv.cboxtv.cms.mainPage;

/**
 * 项目名称:         CBoxTV
 * 包名:            tv.newtv.cboxtv.cms.mainPage
 * 创建事件:         17:55
 * 创建人:           weihaichao
 * 创建日期:          2018/5/31
 */
public final class LooperUtil {

    public static final int MAX_VALUE = 2000;

    public static int getMiddleValue(int size){
        int maxValue = MAX_VALUE - (MAX_VALUE % size);

//        return  (maxValue - (maxValue % 2 != 0 ? size : 0)) / 2;
        if(size == 0){
            return MAX_VALUE /2;
        }
        return maxValue / size / 2 * size;
    }
}
