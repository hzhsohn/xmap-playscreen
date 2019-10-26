package android.hxk.playscreen.magr;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created by han.zh on 2016/10/29.
 *
 <!-- SDCard中创建与删除文件权限 -->
 <!-- 向SDCard写入数据权限 -->
 <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

 如果是模拟器.即要手动打开APP的访问SDCard权限
 static public void readVerifyKey()
 {
 //读
 Properties prop= ConfigMagr.loadConfig("hx-kong","verify-key");
 if(prop!=null) {
 verifyKey=(String) prop.get("key");
 }
 }

 static public void writeVerifyKey(String key)
 {
 //写
 Properties prop=new Properties();
 prop.put("key",key);
 ConfigMagr.saveConfig("hx-kong","verify-key",prop);
 }

 */
public class ConfigMagr {
    public static Properties loadConfig(Context cxt, String folder, String file) {
        String fn=cxt.getCacheDir()+"/"+folder+"/"+file;
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(fn);
            properties.load(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return properties;
    }

    //保存配置文件
    public static boolean saveConfig(Context cxt,String folder,String file, Properties properties) {
        try {
            String mkdir = cxt.getCacheDir()+"/"+folder;
            makeRootDirectory(mkdir);
            String fn=mkdir +"/"+ file;
            File fil = new File(fn);
            if(!fil.exists())
               fil.createNewFile();
            FileOutputStream s = new FileOutputStream(fil);
            properties.store(s, "");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void makeRootDirectory(String filePath) {
        File file = null;
      try {
                file = new File(filePath);
                if (!file.exists()) {
                      file.mkdir();
                  }
         } catch (Exception e) {

         }
     }

}
