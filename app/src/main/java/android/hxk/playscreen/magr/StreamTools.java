package android.hxk.playscreen.magr;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by han.zh on 2016/10/31.
 */

public class StreamTools {
    /**
     * 工具方法
     * @param is 输入流
     * @return 文本字符串
     * @throws Exception
     */
    public static  String readStream(InputStream is) throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while((len = is.read(buffer))!=-1){
            baos.write(buffer, 0, len);
        }
        is.close();
        String temp =  baos.toString();
        return temp;
    }
}

