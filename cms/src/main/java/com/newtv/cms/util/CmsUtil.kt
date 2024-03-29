package com.newtv.cms.util

import android.annotation.SuppressLint
import android.text.TextUtils
import com.newtv.cms.bean.Alternate
import com.newtv.cms.bean.Content
import com.newtv.cms.bean.LiveParam
import com.newtv.cms.bean.Video
import com.newtv.libs.util.CalendarUtil
import com.newtv.libs.util.LogUtils
import com.newtv.libs.util.PlayerTimeUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            com.newtv.cms.util
 * 创建事件:         16:46
 * 创建人:           weihaichao
 * 创建日期:          2018/10/8
 */
object CmsUtil {

    /**
     * 对播放器的播放列表进行排序处理
     */
    @JvmStatic
    fun sortPlayList(content: Content?) {
//        content?.let { ct ->
//            val playDesc = isPlayDesc(content)
//            if (playDesc) {
//                ct.data?.sortedByDescending { it.periods }
//            } else {
//                ct.data?.sortedBy { it.periods }
//            }
//        }
    }

    /**
     * 对界面显示的数据做排序处理
     */
    @JvmStatic
    fun sortUIList(content: Content?) {
//        content?.let { ct ->
//            val sortDesc = isUIDataDesc(content)
//            if (sortDesc) {
//                ct.data?.sortedByDescending { it.periods }
//            } else {
//                ct.data?.sortedBy { it.periods }
//            }
//        }
    }


    @JvmStatic
    fun isVideoTv(content: Content?): Boolean {
        val videoType = content?.videoType
        return TextUtils.equals(videoType, "电视剧")
                || TextUtils.equals(videoType, "动漫");
    }

    @JvmStatic
    fun parse(time: String): Long {
        return PlayerTimeUtils.parseTime(time, "yyyy-MM-dd HH:mm:ss.S")
    }

    /**
     * 查找当前时间段播放的内容
     *
     * @param alternateList 播放列表
     * @param now           当前时间
     * @param beginIndex    开始查找索引值
     * @param endIndex      结束查找索引值
     *
     * @return 返回时间段内播放的index
     */
    @JvmStatic
    fun binarySearch(alternateList: List<Alternate>, now: Long, beginIndex: Int, endIndex: Int): Int {
        val count = alternateList.size
        var returnIndex = -1
        var index = 0
        while (index < count) {
            val alternate = alternateList[index]
            val begin = parse(alternate.startTime)
            val end = begin + (Integer.parseInt(alternate.duration) * 1000)
            if (now < begin || (now in (begin + 1)..(end - 1))) {
                returnIndex = index
                break
            }
            index++
        }
        return returnIndex
    }


    /**
     * 转换剧集页内外排序之后的对应关系
     */
    @JvmStatic
    fun translateIndex(content: Content?, index: Int): Int {
//        if (isVideoTv(content) && !TextUtils.isEmpty(content?.isFinish) && !TextUtils.isEmpty
//                (content?.playOrder)) {
//            content?.let {
//                val isUiDesc: Boolean = isUIDataDesc(it)
//                val isPlayerDesc: Boolean = isPlayDesc(it)
//                if (isUiDesc == isPlayerDesc) {
//                    return index
//                }
//                it.data?.let { dataList ->
//                    return dataList.size - 1 - index
//                }
//            }
//        }
        return index
    }

    /**
     * UI界面数据是否需要倒序显示
     *
     * 默认返回倒序显示
     */
    @JvmStatic
    fun isUIDataDesc(content: Content?): Boolean {
        content?.let {
            if (!TextUtils.isEmpty(it.sortType)) {
                return "1".equals(it.sortType)
            }
        }
        return true
    }

    /**
     * 播放器播放列表是否倒序播放
     *
     * 默认返回倒序显示
     */
    @JvmStatic
    fun isPlayDesc(content: Content?): Boolean {
        content?.let {
            if (!TextUtils.isEmpty(it.playOrder)) {
                return "1".equals(it.playOrder)
            }
        }
        return true
    }


    private fun getLiveParam(video: Video?): LiveParam? {
        if (video != null) {
            if ("LIVE" == video.videoType && !TextUtils.isEmpty(video.liveUrl)) {
                return isLiveTime(video.liveParam)
            }
        }
        return null
    }

    @JvmStatic
    fun isLive(video: Video?): LiveParam? {
        val param = getLiveParam(video)
        return if (param != null && CmsUtil.checkLiveParam(param)) {
            param
        } else null
    }

    /**
     *
     */
    @JvmStatic
    fun isLiveTime(params: List<LiveParam>?): LiveParam? {
        if (params == null || params.size <= 0)
            return null
        params.forEach {
            if (checkLiveParam(it)) {
                return it
            }
        }
        return null
    }

    /**
     *
     */
    fun checkLiveParam(param: LiveParam): Boolean {
        if (!TextUtils.isEmpty(param.liveParam)) {
            val week: Int = CalendarUtil.getCurrentWeek()
            if (param.liveParam.contains(Integer.toString(week))) {
                return checkInTime(param.playStartTime, param.playEndTime, false)
            }
        } else {
            return checkInTime(param.playStartTime, param.playEndTime, true);
        }
        return false
    }

    @SuppressLint("SimpleDateFormat")
    @Suppress("NAME_SHADOWING")
    private fun checkInTime(start: String, end: String, hasDate: Boolean = false): Boolean {
        val now = Date()
        val fmt: DateFormat
        if (hasDate) {
            fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val start: Date = fmt.parse(start)
            val end: Date = fmt.parse(end)
            return now.after(start) && now.before(end)
        } else {
            fmt = SimpleDateFormat("HH:mm:ss")
            val current = formatToSeconds(fmt.format(now))
            val startInt = formatToSeconds(start)
            val endInt = formatToSeconds(end)
            LogUtils.e("CmsUtil", "start=$startInt end=$endInt current=$current")
            return current in (startInt + 1)..(endInt - 1)
        }
    }

    fun formatToSeconds(timeFormat: String?): Int {
        if (timeFormat == null) {
            return 0
        }
        val times = timeFormat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var result = 0
        for (index in 0..2) {
            if (times.size >= index + 1) {
                val value = times[index]
                if (!TextUtils.isEmpty(value) && !"00".equals(value)) {
                    when (index) {
                        0 -> result += 3600 * Integer.parseInt(value)
                        1 -> result += 60 * Integer.parseInt(value)
                        else -> result += Integer.parseInt(value)
                    }
                }
            }
        }
        return result
    }

    @JvmStatic
    fun getTime(startTime: String, duration: String): String {
        val sb = StringBuilder()
        val calendar = Calendar.getInstance()
        var after: Calendar? = null
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
        val targetFormat = SimpleDateFormat("HH:mm")
        try {
            val parse = format.parse(startTime)
            calendar.time = parse
            after = calendar.clone() as Calendar
            after.add(Calendar.SECOND, Integer.parseInt(duration))

            sb.append(targetFormat.format(calendar.time))
            sb.append("-")
            sb.append(targetFormat.format(after.time))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sb.toString()
    }


}