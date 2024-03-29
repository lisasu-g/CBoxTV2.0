package com.newtv.cms.models

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import com.newtv.cms.*
import com.newtv.cms.api.IPage
import com.newtv.cms.bean.ModelResult
import com.newtv.cms.bean.Page

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            com.newtv.cms.models
 * 创建事件:         15:23
 * 创建人:           weihaichao
 * 创建日期:          2018/9/26
 */
internal class PageModel : BaseModel(), IPage {
    override fun getType(): String {
        return Model.MODEL_PAGE
    }

    override fun getPage(appkey: String, channelId: String, pageId: String,
                         observer: DataObserver<ModelResult<ArrayList<Page>>>): Long {
        if (TextUtils.isEmpty(appkey) || TextUtils.isEmpty(channelId)) {
            observer.onError(CmsErrorCode.APP_ERROR_KEY_CHANNEL_EMPTY, CmsErrorCode.getErrorMessage(CmsErrorCode.APP_ERROR_KEY_CHANNEL_EMPTY))
            return 0
        }
        if (TextUtils.isEmpty(pageId)) {
            observer.onError(CmsErrorCode.APP_ERROR_CONTENT_ID_EMPTY, CmsErrorCode
                    .getErrorMessage(CmsErrorCode.APP_ERROR_CONTENT_ID_EMPTY))
            return 0
        }

        val executor: Executor<ModelResult<ArrayList<Page>>> =
                buildExecutor(Request.page.getPageData(appkey,
                        channelId, pageId),
                        object : TypeToken<ModelResult<ArrayList<Page>>>() {}.type)
        executor.observer(observer)
                .execute()
        return executor.getID()
    }
}