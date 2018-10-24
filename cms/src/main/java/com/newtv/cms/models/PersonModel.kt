package com.newtv.cms.models

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import com.newtv.cms.*
import com.newtv.cms.api.IPerson
import com.newtv.cms.bean.ModelResult
import com.newtv.cms.bean.SubContent
import java.util.*

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            com.newtv.cms.models
 * 创建事件:         10:46
 * 创建人:           weihaichao
 * 创建日期:          2018/10/16
 */
internal class PersonModel : BaseModel(), IPerson {
    override fun getType(): String {
        return Model.MODEL_PERSON
    }

    override fun getPersonTvList(appkey: String, channelId: String, UUID: String, observer:
    DataObserver<ModelResult<ArrayList<SubContent>>>): Long {
        if (TextUtils.isEmpty(appkey) || TextUtils.isEmpty(channelId)) {
            observer.onError("AppKey or ChannelCode is Empty")
            return 0
        }
        if (TextUtils.isEmpty(UUID) || UUID.length < 2) {
            observer.onError("ContentId size is to short")
            return 0
        }
        val left: String = getLeft(UUID)
        val right: String = getRight(UUID)
        val executor: Executor<ModelResult<ArrayList<SubContent>>> =
                buildExecutor(Request.person
                        .getPersonTvList
                        (appkey, channelId,
                                left, right, UUID), object : TypeToken<ModelResult<List<SubContent>>>() {}.type)
        executor.observer(observer)
                .execute()
        return executor.getID()
    }

    override fun getPersonProgramList(appkey: String, channelId: String, UUID: String, observer:
    DataObserver<ModelResult<ArrayList<SubContent>>>): Long {
        if (TextUtils.isEmpty(appkey) || TextUtils.isEmpty(channelId)) {
            observer.onError("AppKey or ChannelCode is Empty")
            return 0
        }
        if (TextUtils.isEmpty(UUID) || UUID.length < 2) {
            observer.onError("ContentId size is to short")
            return 0
        }
        val left: String = getLeft(UUID)
        val right: String = getRight(UUID)
        val executor: Executor<ModelResult<ArrayList<SubContent>>> =
                buildExecutor(Request.person
                        .getPersonProgramList
                        (appkey, channelId,
                                left, right, UUID), object : TypeToken<ModelResult<List<SubContent>>>() {}.type)
        executor.observer(observer)
                .execute()
        return executor.getID()
    }

    override fun getPersonFigureList(appkey: String, channelId: String, UUID: String, observer:
    DataObserver<ModelResult<ArrayList<SubContent>>>): Long {
        if (TextUtils.isEmpty(appkey) || TextUtils.isEmpty(channelId)) {
            observer.onError("AppKey or ChannelCode is Empty")
            return 0
        }
        if (TextUtils.isEmpty(UUID) || UUID.length < 2) {
            observer.onError("ContentId size is to short")
            return 0
        }
        val left: String = getLeft(UUID)
        val right: String = getRight(UUID)
        val executor: Executor<ModelResult<ArrayList<SubContent>>> =
                buildExecutor(Request.person
                        .getPersonFigureList
                        (appkey, channelId,
                                left, right, UUID), object : TypeToken<ModelResult<List<SubContent>>>() {}.type)
        executor.observer(observer)
                .execute()
        return executor.getID()
    }
}