package tv.newtv.cboxtv.views;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.newtv.libs.Constant;
import com.newtv.libs.ad.ADHelper;
import com.newtv.libs.util.ScreenUtils;
import com.squareup.picasso.Picasso;

import tv.newtv.cboxtv.cms.details.presenter.adpresenter.ADPresenter;
import tv.newtv.cboxtv.cms.details.presenter.adpresenter.IAdConstract;


public class AdPopupWindow extends PopupWindow implements IAdConstract.IADConstractView{
    private ADPresenter adPresenter;
    private ImageView imageView;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            dismiss();
        }
    };

    public void show(Context context,View parent){
        View popView = LayoutInflater.from(context).inflate(tv.newtv.cboxtv.R.layout.layout_ad_pop,null);
        imageView = popView.findViewById(tv.newtv.cboxtv.R.id.image);
        setContentView(popView);

        int marginBottom = context.getResources().getDimensionPixelOffset(com.newtv.cms.R.dimen.width_60px);
        int marginRight = context.getResources().getDimensionPixelOffset(com.newtv.cms.R.dimen.width_120px);
        int width = context.getResources().getDimensionPixelOffset(com.newtv.cms.R.dimen.width_359px);
        int height = context.getResources().getDimensionPixelOffset(com.newtv.cms.R.dimen.width_359px);
        setWidth(width);
        setHeight(height);
        setBackgroundDrawable(new BitmapDrawable());
        showAtLocation(parent, Gravity.NO_GRAVITY,ScreenUtils.getScreenW() - width - marginRight,
                ScreenUtils.getScreenH() - height - marginBottom);

        adPresenter = new ADPresenter(this);
        adPresenter.getAD(Constant.AD_DESK,Constant.AD_GLOBAL_POPUP,"");
    }

    @Override
    public void showAd(ADHelper.AD.ADItem item) {
        if(item.PlayTime <= 0){
            item.PlayTime = 5;
        }
        if(!TextUtils.isEmpty(item.AdUrl)){
            Picasso.get().load(item.AdUrl).into(imageView);
            handler.sendEmptyMessageDelayed(0,item.PlayTime * 1000);
        }else {
            dismiss();
        }
    }
}
