package com.newtv.libs;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by TCP on 2018/5/8.
 */

public class HeadersInterceptor implements okhttp3.Interceptor {
    public static final String CMS = "CMS";
    public static final String SERVER_TIME = "SERVER_TIME";
    public static final String VERSION_UP = "VERSION_UP";
    public static final String AD = "AD";
    public static final String IS_ORIENTED = "IS_ORIENTED";
    public static final String LOG = "LOG";
    public static final String ACTIVATE = "ACTIVATE";
    public static final String ACTIVATE2 = "ACTIVATE2";
    public static final String ACTIVATE3 = "ACTIVATE3";
    public static final String CDN = "CDN";
    public static final String DYNAMIC_KEY = "DYNAMIC_KEY";
    public static final String SEARCH = "SEARCH";
    public static final String PERMISSTION_CHECK = "PERMISSTION_CHECK";
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl oldHttpUrl = request.url();
        Request.Builder builder = request.newBuilder();
        String headerValue = request.header("host_type");
        HttpUrl newBaseUrl = null;
        if(headerValue != null){
            builder.removeHeader("host_type");
            switch (headerValue){
                case CMS:
                    newBaseUrl = HttpUrl.parse(Constant.BASE_URL_CMS);
                    break;
                case SEARCH:
                    newBaseUrl = HttpUrl.parse(Constant.BASE_URL_SEARCH);
                    break;
                case SERVER_TIME:
                    newBaseUrl = HttpUrl.parse(Constant.SERVER_TIME_URL);
                    break;
                case VERSION_UP:
                    newBaseUrl = HttpUrl.parse(Constant.VERSION_UP);
                    break;
                case ACTIVATE:
                    newBaseUrl = HttpUrl.parse(Constant.BASE_URL_ACTIVATE);
                    break;
                case AD:
                    newBaseUrl = HttpUrl.parse(Constant.BASE_URL_AD);
                    break;
                case LOG:
                    newBaseUrl = HttpUrl.parse(Constant.LOG_ADDR);
                    break;
                case CDN:
                    newBaseUrl = HttpUrl.parse(Constant.BASE_URL_CDN);
                    break;
                case DYNAMIC_KEY:
                    newBaseUrl = HttpUrl.parse(Constant.DYNAMIC_KEY);
                    break;
                case PERMISSTION_CHECK:
                    newBaseUrl = HttpUrl.parse(Constant.BASE_PERMISSTION_CHECK);
                    break;
                case IS_ORIENTED:
                    newBaseUrl = HttpUrl.parse(Constant.IS_ORIENTED);
                    break;

            }

            if(newBaseUrl == null){
                return chain.proceed(request);
            }
            HttpUrl newFullUrl = oldHttpUrl.newBuilder()
                    .scheme(newBaseUrl.scheme())
                    .host(newBaseUrl.host())
                    .port(newBaseUrl.port())
                    .build();

            return chain.proceed(builder.url(newFullUrl).build());
        }else{
            return chain.proceed(request);
        }
    }
}