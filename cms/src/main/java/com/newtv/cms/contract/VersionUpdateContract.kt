package com.newtv.cms.contract

import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Log
import com.newtv.cms.CmsServicePresenter
import com.newtv.cms.DataObserver
import com.newtv.cms.ICmsPresenter
import com.newtv.cms.ICmsView
import com.newtv.cms.api.IUpVersion
import com.newtv.cms.bean.Oriented
import com.newtv.cms.bean.UpVersion
import com.newtv.libs.Constant
import com.newtv.libs.Libs
import com.newtv.libs.util.LogUtils
import com.newtv.libs.util.SystemUtils
import java.util.*

/**
 * 项目名称:         CBoxTV2.0
 * 包名:            tv.newtv.contract
 * 创建事件:         11:26
 * 创建人:           weihaichao
 * 创建日期:          2018/10/11
 */
class VersionUpdateContract {
    interface View : ICmsView {
        fun versionCheckResult(versionBeen: UpVersion?, isForce: Boolean)
    }

    interface Presenter : ICmsPresenter {
        fun checkVersionUpdate(context: Context)
        fun checkCreateUpVersion(context: Context)
    }

    class UpdatePresenter(context: Context, view: View) : CmsServicePresenter<View>(context, view), Presenter {

        private var updateService: IUpVersion? = null

        init {
            updateService = getService<IUpVersion>(CmsServicePresenter.SERVICE_UPVERSTION)
        }

        override fun checkVersionUpdate(context: Context) {

            updateService?.let { update ->
                val params = createOrientedParam(context)
                update.getIsOriented(params, object : DataObserver<Oriented> {
                    override fun onResult(result: Oriented, requestCode: Long) {
                        val hardwareCode: String? = if ("enable".equals(result.oriented))
                            params["hardwareCode"] else ""
                        checkUpVersion(context, Integer.parseInt(params["versionCode"]), hardwareCode)
                    }

                    override fun onError(code: String?, desc: String?) {
                        view?.onError(context, code, desc)
                    }
                })
            }
        }

        internal fun checkUpVersion(context: Context, versionCode: Int, hardwareCode: String?) {
            updateService?.let {update->
                val params = createUpVersionParam(context, versionCode,
                        hardwareCode)
                update.getUpVersion(params, object : DataObserver<UpVersion> {
                    override fun onResult(result: UpVersion, requestCode: Long) {
                        if (TextUtils.isEmpty(result.versionCode) || "null" == result
                                        .versionCode) {
                            return
                        }
                        if (Integer.parseInt(result.versionCode) > versionCode && !TextUtils
                                        .isEmpty(result.versionName)) {
                            if (!TextUtils.isEmpty(result.packageMD5)) {
                                val mSharedPreferences = context.getSharedPreferences("VersionMd5", Context.MODE_PRIVATE)
                                mSharedPreferences.edit().putString("versionmd5", result
                                        .packageMD5).apply()
                            }
                            if ("1" == result.upgradeType) {
                                //强制升级
                                view?.versionCheckResult(result, true)
                            } else {
                                //非强制升级
                                view?.versionCheckResult(result, false)
                            }

                            Constant.VERSION_UPDATE = true;
                        }
                    }

                    override fun onError(code: String?, desc: String?) {
                        view?.onError(context,code, desc)
                    }
                })
            }
        }

        /**
         * @param context
         * @param versionCode
         * @param hardwareCode
         * @return
         */
        internal fun createUpVersionParam(context: Context, versionCode: Int,
                                          hardwareCode: String?): HashMap<String, String> {
            val hashMap = HashMap<String, String>()
            hashMap["appKey"] = Libs.get().appKey
            hashMap["channelCode"] = Libs.get().channelId
            hashMap["versionCode"] = Integer.toString(versionCode)
            if (!TextUtils.isEmpty(hardwareCode)) {
                hashMap["hardwareCode"] = "" + hardwareCode
            }
            hashMap["uuid"] = Constant.UUID
            hashMap["mac"] = SystemUtils.getMac(context)
            return hashMap
        }

        /**
         * @param context
         * @return
         */
        internal fun createOrientedParam(context: Context): HashMap<String, String> {
            var versionCode = 0
            val hardwareCode = SystemUtils.getMac(context)
            val hashMap = HashMap<String, String>()
            hashMap["appKey"] = Libs.get().appKey
            hashMap["channelCode"] = Libs.get().channelId
            try {
                versionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            } catch (e: PackageManager.NameNotFoundException) {
                LogUtils.e(e.toString())
            }

            hashMap["versionCode"] = Integer.toString(versionCode)
            hashMap["uuid"] = Constant.UUID
            hashMap["mac"] = hardwareCode
            hashMap["hardwareCode"] = hardwareCode
            return hashMap
        }

        override fun checkCreateUpVersion(context: Context){
            Log.d("ywy OK version : ","checkVersion");
            updateService?.let {update->
                val versionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                val params = createUpParam(context, versionCode)
                update.getUpVersion(params, object : DataObserver<UpVersion> {
                    override fun onResult(result: UpVersion, requestCode: Long) {
                        if (TextUtils.isEmpty(result.versionCode) || "null" == result
                                        .versionCode) {
                            return
                        }
                        if (Integer.parseInt(result.versionCode) > versionCode && !TextUtils
                                        .isEmpty(result.versionName)) {
                            if (!TextUtils.isEmpty(result.packageMD5)) {
                                val mSharedPreferences = context.getSharedPreferences("VersionMd5", Context.MODE_PRIVATE)
                                mSharedPreferences.edit().putString("versionmd5", result
                                        .packageMD5).apply()
                            }
                            if ("1" == result.upgradeType) {
                                //强制升级
                                view?.versionCheckResult(result, true)
                            } else {
                                //非强制升级
                                view?.versionCheckResult(result, false)
                            }

                            Constant.VERSION_UPDATE = true;
                        }
                    }

                    override fun onError(code: String?, desc: String?) {
                        view?.onError(context, code, desc)
                    }
                })
            }
        }

        internal fun createUpParam(context: Context, versionCode: Int): HashMap<String, String> {
            val hardwareCode = SystemUtils.getMac(context)

            val hashMap = HashMap<String, String>()
            hashMap["appKey"] = Libs.get().appKey
            hashMap["channelCode"] = Libs.get().channelId
            hashMap["versionCode"] = Integer.toString(versionCode)
            if (!TextUtils.isEmpty(hardwareCode)) {
                hashMap["hardwareCode"] = "" + hardwareCode
            }
            Log.d("ywy OK version create: ","appKey : " + Libs.get().appKey +
                    "  channel : " + Libs.get().channelId +
                    "  version : "+Integer.toString(versionCode) + "  hardwareCode : "+hardwareCode);
            return hashMap
        }

    }
}
