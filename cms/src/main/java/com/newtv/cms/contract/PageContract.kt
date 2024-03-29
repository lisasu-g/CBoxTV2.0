package com.newtv.cms.contract

import android.content.Context

import com.newtv.cms.CmsServicePresenter
import com.newtv.cms.DataObserver
import com.newtv.cms.ICmsPresenter
import com.newtv.cms.ICmsView
import com.newtv.cms.api.IPage
import com.newtv.cms.bean.ModelResult
import com.newtv.cms.bean.Page
import com.newtv.libs.Libs

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.cboxtv.cms.mainPage.view
 * 创建事件:         15:50
 * 创建人:           weihaichao
 * 创建日期:          2018/9/27
 */
class PageContract {

    interface View : ICmsView,LoadingView {
        fun onPageResult(page: List<Page>?)
    }

    interface ModelView : ICmsView,LoadingView {
        fun onPageResult(page: ModelResult<ArrayList<Page>>)
    }

    interface LoadingView  {
        fun startLoading()
        fun loadingComplete()
    }

    interface Presenter : ICmsPresenter {
        fun getPageContent(contentId: String)
    }

    class ContentPresenter(context: Context, view: ICmsView)
        : CmsServicePresenter<ICmsView>(context, view), Presenter {

        private var pageService:IPage? = null

        init {
            pageService = getService<IPage>(CmsServicePresenter.SERVICE_PAGE)
        }


        override fun getPageContent(contentId: String) {
            view?.let {
                if (it is LoadingView) {
                    it.startLoading()
                }
            }
            pageService?.getPage(
                    Libs.get().appKey,
                    Libs.get().channelId,
                    contentId, object : DataObserver<ModelResult<ArrayList<Page>>> {
                override fun onResult(result: ModelResult<ArrayList<Page>>, requestCode: Long) {
                    if (result.isOk()) {
                        view?.let {
                            if (it is View) {
                                it.onPageResult(result.data)
                            } else if (it is ModelView) {
                                it.onPageResult(result)
                            }
                            if (it is LoadingView) {
                                it.loadingComplete()
                            }
                        }
                    } else {
                        onError(result.errorCode, result.errorMessage)
                    }
                }

                override fun onError(code: String?, desc: String?) {
                    view?.onError(context,code, desc)
                }
            })
        }
    }

}
