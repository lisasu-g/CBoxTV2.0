package com.newtv.cms.service

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            com.newtv.cms.api.v31
 * 创建事件:         09:51
 * 创建人:           weihaichao
 * 创建日期:          2018/9/26
 */
internal interface ICategoryRetro{

    /**
     * 获取栏目树
     * @param appkey
     * @param channelCode
     */
    @GET("api/v31/{appkey}/{channelCode}/categorytree/categorytree.json")
    fun getCategoryTree(@Path("appkey") appkey: String,
                @Path("channelCode") channelId: String): Observable<ResponseBody>

    /**
     * 获取栏目内容
     * @param appkey
     * @param channelCode
     * @param categoryId
     */
    @GET("api/v31/{appkey}/{channelCode}/categorytree/categorytree.json")
    fun getCategoryContent(@Path("appkey") appkey: String,
                        @Path("channelCode") channelId: String,
                        @Path("left") left: String,
                        @Path("right") right: String,
                        @Path("contentID") contentUUID: String): Observable<ResponseBody>
}