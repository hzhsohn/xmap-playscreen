package android.hxk.playscreen.magr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import android.os.Handler;

/**
 * Created by hzh on 2017/8/29.
 */

public class DownloadPic {

    private Vector eventListener = new Vector();
    private static String ALBUM_PATH = null; //保存路径
    private static String pic_url = null; //图片路径
    private String mFileName;
    private Handler connectHanlder = null;
    private Context cxt;


    public void addListener(Object listener) {
        eventListener.add(listener);
    }


    public DownloadPic(Context context)
    {
        cxt=context;
    }

    //下载图片
    /*
    * return 0=成功
    *        1=图片保存失败
    *        2=无效图片
    *        3=网络连接失败
    * */
    public void download(String str_url, String saveFolder,String saveFilename) {
        ALBUM_PATH = cxt.getCacheDir() + "/" + saveFolder + "/";
        pic_url = str_url;
        mFileName=saveFilename;
        new Thread(connectNet).start();
    }

    /**
     * Get image from newwork
     *
     * @param path The path of image
     * @return byte[]
     * @throws Exception
     */
    public byte[] getImage(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        InputStream inStream = conn.getInputStream();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return readStream(inStream);
        }
        return null;
    }

    /**
     * Get image from newwork
     *
     * @param path The path of image
     * @return InputStream
     * @throws Exception
     */
    public InputStream getImageStream(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return conn.getInputStream();
        }
        return null;
    }

    /**
     * Get data from stream
     *
     * @param inStream
     * @return byte[]
     * @throws Exception
     */
    public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    /**
     * 保存文件
     *
     * @param fileName
     * @throws IOException
     */
    public void saveFile(byte[] data, String fileName) throws IOException {
        File dirFile = new File(ALBUM_PATH);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        File myCaptureFile = new File(ALBUM_PATH + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
        bos.write(data);
        bos.flush();
        bos.close();
    }

    /*
     * 连接网络
     * 由于在4.0中不允许在主线程中访问网络，所以需要在子线程中访问
     */
    private Runnable connectNet = new Runnable() {
        @Override
        public void run() {
            try {
                //以下是取得图片的两种方法
                //////////////// 方法1：取得的是byte数组, 从byte数组生成bitmap
                byte[] data = getImage(pic_url);
                if (data != null) {
                    try {
                        saveFile(data, mFileName);
                        succ();

                    } catch (IOException e) {
                        save_fail();
                    }
                } else {
                    Message msg = Message.obtain();//减少消息创建的数量
                    msg.obj = "Image error!";
                    msg.what = 2;
                    connectHanlder.sendMessage(msg);
                }
                ////////////////////////////////////////////////////////

            } catch (Exception e) {
                e.printStackTrace();
                conn_fail();
            }

        }

    };


    private void succ() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator iterator = eventListener.iterator();
                while (iterator.hasNext()) {
                    DownloadPicListener sl = (DownloadPicListener) iterator.next();
                    sl.success(pic_url,ALBUM_PATH + mFileName);
                }
            }
        });
    }

    private void conn_fail() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator iterator = eventListener.iterator();
                while (iterator.hasNext()) {
                    DownloadPicListener sl = (DownloadPicListener) iterator.next();
                    sl.connect_fail(pic_url);
                }
            }
        });
    }

    private void save_fail() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator iterator = eventListener.iterator();
                while (iterator.hasNext()) {
                    DownloadPicListener sl = (DownloadPicListener) iterator.next();
                    sl.save_pic_fail(pic_url);
                }
            }
        });
    }
}