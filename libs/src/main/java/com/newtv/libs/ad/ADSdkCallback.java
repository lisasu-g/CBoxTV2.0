package com.newtv.libs.ad;


import com.newtv.libs.bean.AdInfo;

/**
 * Created by Administrator on 2018/5/1.
 */

public abstract class ADSdkCallback implements ADHelper.ADCallback{

    public void showAd(String type, String url){}

    public void AdPrepare(ADHelper.AD ad){

    }

    public void showAd(AdInfo adInfo){}

    public void showAdItem(ADHelper.AD.ADItem adItem){}

    public void updateTime(int total, int left){}

    public void complete(){}
}