package com.newtv.cms.contract

import android.content.Context
import com.newtv.cms.CmsServicePresenter
import com.newtv.cms.DataObserver
import com.newtv.cms.ICmsPresenter
import com.newtv.cms.ICmsView
import com.newtv.cms.api.ISearch
import com.newtv.cms.bean.ModelResult
import com.newtv.cms.bean.SubContent
import com.newtv.libs.Libs
import java.nio.ByteOrder
import java.util.*


/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.contract
 * 创建事件:         14:44
 * 创建人:           weihaichao
 * 创建日期:          2018/10/16
 */
class SearchContract {
    interface View : ICmsView {
        fun searchResult(requestID: Long, result: ArrayList<SubContent>?, total: Int?)
    }

    interface LoadingView : View {
        fun onLoading()
        fun loadingFinish()
    }

    interface Presenter : ICmsPresenter {
        /**
         * 搜索
         */
        fun search(condition: SearchCondition): Long
        fun cancel(id: Long)
    }

    @Suppress("FunctionName")
    class SearchCondition {

        internal var categoryId = ""
        internal var contentType = ""
        internal var videoType = ""
        internal var videoClass = ""
        internal var area = ""
        internal var year = ""
        internal var keyword = ""
        internal var page = "0"          //第几页
        internal var rows = "40"         //每页条数
        internal var keywordType = ""
        internal var orderby = "1"

        fun setRows(rows: String): SearchCondition {
            this.rows = rows
            return this
        }

        fun setPage(page: String): SearchCondition {
            this.page = page
            return this
        }

        fun setKeywordType(keywordType: String): SearchCondition {
            this.keywordType = keywordType
            return this
        }

        fun setKeyword(keyword: String): SearchCondition {
            this.keyword = keyword
            return this
        }

        fun setYear(year: String): SearchCondition {
            this.year = year
            return this
        }

        fun setVideoType(videoType: String): SearchCondition {
            this.videoType = videoType
            return this
        }

        fun setVideoClass(videoClass: String): SearchCondition {
            this.videoClass = videoClass
            return this
        }

        fun setContentType(contentType: String): SearchCondition {
            this.contentType = contentType
            return this
        }

        fun setCategoryId(categoryId: String): SearchCondition {
            this.categoryId = categoryId
            return this
        }

        fun setArea(area: String): SearchCondition {
            this.area = area
            return this
        }

        fun  setOrderby(orderby: String):SearchCondition{
            this.orderby = orderby
            return this
        }

        companion object {
            /**
             * @return
             */
            @JvmStatic
            fun Builder(): SearchCondition {
                return SearchCondition()
            }
        }
    }

    class SearchPresenter(context: Context, view: View)
        : CmsServicePresenter<View>(context, view), Presenter {

        private var searchService:ISearch? = null

        init {
            searchService = getService(CmsServicePresenter.SERVICE_SEARCH)
        }


        override fun cancel(id: Long) {
            searchService?.cancel(id)
        }

        override fun search(condition: SearchCondition): Long {
            searchService?.let { iSearch ->
                view?.let {
                    if (it is LoadingView) {
                        it.onLoading()
                    }
                }
                val requestID: Long = iSearch.search(
                        Libs.get().appKey,
                        Libs.get().channelId, condition.categoryId,
                        condition.contentType, condition.videoType, condition.videoClass, condition
                        .area, condition.year, condition.keyword, condition.page, condition
                        .rows, condition.keywordType, condition.orderby, object : DataObserver<ModelResult<ArrayList<SubContent>>> {
                    override fun onResult(result: ModelResult<ArrayList<SubContent>>, requestCode: Long) {
                        if (result.isOk()) {
                            view?.let {
                                if (it is LoadingView) {
                                    it.loadingFinish()
                                }
                                it.searchResult(requestCode, result.data, result.total)
                            }
                        } else {
                            view?.onError(context,result.errorCode, result.errorMessage)
                        }
                    }

                    override fun onError(code: String?, desc: String?) {
                        view?.let { callback ->
                            callback.onError(context,code, desc)
                            if (callback is LoadingView) {
                                callback.loadingFinish()
                            }
                        }
                    }
                })
                return requestID
            }
            return 0
        }
    }
}
