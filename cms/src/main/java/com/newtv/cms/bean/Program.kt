@file:Suppress("UNREACHABLE_CODE")

package com.newtv.cms.bean


data class Program(
        val dataUrl: String,
        val defaultFocus: Int,
        val contentId: String,
        val contentType: String,
        val img: String,
        val title: String,
        val subTitle: String,
        val l_id: String,
        val l_uuid: String,
        val l_contentType: String,
        val l_actionType: String,
        val l_actionUri: String,
        val l_focusId: String,
        val l_focusParam: String,
        val grade: String,
        val columnPoint: String,
        val rowPoint: String,
        val columnLength: String,
        val rowHeight: String,
        val cellType: String,
        val cellCode: String,
        val isAd: Int,
        val sortNum: String,
        val seriesSubUUID: String,
        val realExclusive: String,
        val alternateNumber: String,
        val apkParam: String,
        val specialParam: String,
        val recommendedType: String,
        val recentMsg: String,
        var video: Video?,
        val apk: String,
        val apkPageType: String,
        val apkPageParam: String,
        val recentNum: String,
        val isFinish: String
) {
    fun getLiveParam(): LiveParam? {
        return null
    }

    override fun toString(): String {
        return "Program(dataUrl='$dataUrl', defaultFocus=$defaultFocus, contentId='$contentId', contentType='$contentType', img='$img', title='$title', subTitle='$subTitle', l_id='$l_id', l_uuid='$l_uuid', l_contentType='$l_contentType', l_actionType='$l_actionType', l_actionUri='$l_actionUri', l_focusId='$l_focusId', l_focusParam='$l_focusParam', grade='$grade', columnPoint='$columnPoint', rowPoint='$rowPoint', columnLength='$columnLength', rowHeight='$rowHeight', cellType='$cellType', cellCode='$cellCode', isAd=$isAd, sortNum='$sortNum', seriesSubUUID='$seriesSubUUID', realExclusive='$realExclusive', alternateNumber='$alternateNumber', apkParam='$apkParam', specialParam='$specialParam', recommendedType='$recommendedType', recentMsg='$recentMsg', video=$video, apk='$apk', apkPageType='$apkPageType', apkPageParam='$apkPageParam', recentNum='$recentNum', isFinish='$isFinish')"
    }
}