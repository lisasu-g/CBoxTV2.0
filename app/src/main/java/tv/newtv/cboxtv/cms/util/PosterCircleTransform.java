package tv.newtv.cboxtv.cms.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.newtv.libs.util.DensityUtil;
import com.squareup.picasso.Transformation;

/**
 * 项目名称： NewTVLauncher
 * 类描述：对详情页圆角工具类半径参数改造
 * 创建人：wqs
 * 创建时间： 2018/3/28 0028 11:24
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
public class PosterCircleTransform implements Transformation {
    private Context mContext;
    private int radius = 4;// 圆角半径

    public PosterCircleTransform(Context context, int radius) {
        mContext = context.getApplicationContext();
        this.radius = DensityUtil.dp2px(mContext, radius); // 圆角半径
    }

    public PosterCircleTransform(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int widthLight = source.getWidth();
        int heightLight = source.getHeight();

        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paintColor = new Paint();
        paintColor.setFlags(Paint.ANTI_ALIAS_FLAG);

        RectF rectF = new RectF(new Rect(0, 0, widthLight, heightLight));

        canvas.drawRoundRect(rectF, radius, radius, paintColor);
//        canvas.drawRoundRect(rectF, widthLight / 5, heightLight / 5, paintColor);

        Paint paintImage = new Paint();
        paintImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(source, 0, 0, paintImage);
        source.recycle();
        return output;
    }

    @Override
    public String key() {
        return "roundcorner";
    }
}
