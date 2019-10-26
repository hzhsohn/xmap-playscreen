package android.hxk.playscreen.magr;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by han.zh on 2016/11/8.
 */

public class WebProc {

    private String sUrl=null;
    private Vector eventListener = new Vector();

    public void addListener(Object listener) {
        eventListener.add(listener);
    }

    public void getHtml(final String str_url, final String parameter) {
        sUrl=str_url;
        //访问网络，把html源文件下载下来
        new Thread() {
            public void run() {
                try {
                    String url_path = str_url + "?" + parameter;
                    CookieManager manager = new java.net.CookieManager();
                    CookieHandler.setDefault(manager);
                    URL url = new URL(url_path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");//声明请求方式 默认get
                    conn.setConnectTimeout(5000);//设置请求超时时间
                    Object obj = conn.getContent();
                    CookieStore cookieJar = manager.getCookieStore();
                    List<HttpCookie> cookies = cookieJar.getCookies();
                    for (HttpCookie cookie : cookies) {
                        cookies_notify(cookie.toString());
                    }
                    /////////////////////////////////////////////
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        InputStream is = conn.getInputStream();
                        String result = StreamTools.readStream(is);
                        /////////////////////////////////////////////
                        succ_html(result);
                    }
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }.start();
    }

    private void cookies_notify(final String strCookies) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator iterator = eventListener.iterator();
                while (iterator.hasNext()) {
                    WebProcListener sl = (WebProcListener) iterator.next();
                    sl.cookies(sUrl,strCookies);
                }
            }
        });
    }

    private void succ_html(final String strHtml) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator iterator = eventListener.iterator();
                while (iterator.hasNext()) {
                    WebProcListener sl = (WebProcListener) iterator.next();
                    sl.success_html(sUrl,strHtml);
                }
            }
        });
    }

    private void fail(final String msg) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator iterator = eventListener.iterator();
                while (iterator.hasNext()) {
                    WebProcListener sl = (WebProcListener) iterator.next();
                    sl.fail(sUrl,msg);
                }
            }
        });
    }
}

