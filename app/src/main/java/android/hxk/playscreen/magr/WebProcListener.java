package android.hxk.playscreen.magr;

import java.util.EventListener;

/**
 * Created by hzh on 2017/8/28.
 *
 */
public interface WebProcListener extends EventListener {
    public void cookies(String url,String cookie);
    public void success_html(String url,String html);
    public void fail(String url,String errMsg);
}