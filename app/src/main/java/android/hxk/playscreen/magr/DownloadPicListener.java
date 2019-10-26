package android.hxk.playscreen.magr;

import java.util.EventListener;

/**
 * Created by hzh on 2017/8/28.
 *
 */
public interface DownloadPicListener extends EventListener {
    public void success(String url,String album_path);
    public void connect_fail(String url);
    public void save_pic_fail(String url);
}