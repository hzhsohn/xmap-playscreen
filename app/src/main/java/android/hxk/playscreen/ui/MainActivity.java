package android.hxk.playscreen.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hxk.playscreen.magr.DownloadPic;
import android.hxk.playscreen.magr.DownloadPicListener;
import android.hxk.playscreen.magr.WebProc;
import android.hxk.playscreen.magr.WebProcListener;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.hxk.playscreen.R;
import android.hxk.playscreen.ebus.EBusContent;
import android.hxk.playscreen.ebus.EBusType;
import android.hxk.playscreen.mqtt.MqttManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.lichfaker.log.Logger;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    public static final String URL = "tcp://192.168.1.102:1883";
    public static final String mCommandUrl0="http://192.168.1.101/playscreen/command.php";
    public static final String mCommandUrl1="http://192.168.1.101/playscreen/command1.php";
    public static final String mCommandUrl2="http://192.168.1.101/playscreen/command2.php";
    public static String mCommandUrl=mCommandUrl0;
    private String userName = "";
    private String password = "";
    private String clientId = "";
    private boolean isFristConnected=false;
    Timer timer = new Timer();
    VideoView mVideo;
    WebView mWeb;
    ImageView mImg;
    int nPicDownloadPos; //下载图片的游标
    int nShowPicPos=-1;
    WebProc mLoadCommand;
    //MSD设备的信息
    String msdClientID;
    //
    ArrayList<String> v_list = new ArrayList<>();
    ArrayList<String> p_list = new ArrayList<>();
    ArrayList<String> p_list_download_ok = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //注册EventBus
        EventBus.getDefault().register(this);
        //MSD设备
        msdClientID=(int)(Math.random()*100000)+"";
        //主动连接MQTT
        timer.schedule(timerTask,1000,5000);//延时1s，每隔5000毫秒执行一次run方法

        //界面初始化
        mVideo=(VideoView)findViewById(R.id.videoView);
        mWeb=(WebView) findViewById(R.id.webView);
        mImg=(ImageView) findViewById(R.id.imageView);
        mVideo.setVisibility(View.GONE);
        mWeb.setVisibility(View.GONE);

        //加载命令
        mLoadCommand=new WebProc();
        mLoadCommand.addListener(jsonCommand);
        webCommandInfo();
    }

    private void webCommandInfo()
    {
        mLoadCommand.getHtml(mCommandUrl,"");
    }
    private void webCommandInfoDelay()
    {
        //2秒后重新获取
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                webCommandInfo();
            }
        };
        timer.schedule(task, 2000);//此处的Delay
    }
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1){

                boolean isConnected=MqttManager.getInstance().isConnected();

                Log.d("mqtt","当前连接状态:"+isConnected);
                if(!isConnected) {
                    //连接MQTT
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean b = MqttManager.getInstance().creatConnect(URL, userName, password, clientId);
                            if(b) {
                                Log.d("mqtt", "连接成功!");
                                isFristConnected=true;
                            }
                        }
                    }).start();
                }
                if(isFristConnected) {
                    isFristConnected=false;
                    //订阅标题
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //MSD搜索
                            MqttManager.getInstance().subscribe("MSD/A", 2);
                            //MSD设备控制
                            MqttManager.getInstance().subscribe(msdClientID, 2);
                        }
                    }).start();
                }


                showPicList();
            }
            super.handleMessage(msg);
        }
    };

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);

        }
    };

    //初始化界面
    void initFrmLayout()
    {
        mVideo.setVisibility(View.GONE);
        mWeb.setVisibility(View.GONE);
        mImg.setVisibility(View.GONE);
    }

    //播放本地视频
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initLocalVideo(String filename) {
        //设置有进度条可以拖动快进
        MediaController localMediaController = new MediaController(this);
        mVideo.setMediaController(localMediaController);
        String uri = ("android.resource://" + getPackageName() + "/"+filename);
        mVideo.setVideoURI(Uri.parse(uri));
        mVideo.start();
    }

    //播放网络视频
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initNetVideo(String url) {
        //设置有进度条可以拖动快进
        MediaController localMediaController = new MediaController(this);
        mVideo.setMediaController(localMediaController);
        mVideo.setVideoPath(url);
        mVideo.start();
    }

    /**
     * 订阅接收到的消息
     * 这里的Event类型可以根据需要自定义, 这里只做基础的演示
     *
     */
    @Subscribe
    public void onEvent(EBusContent ebc) {
        if (ebc.ebusType == EBusType.ebusMQTTContent)
        {
            //输出test订阅信息的内容
            MqttMessage message = (MqttMessage) ebc.content;
            Logger.d(message.toString());
            try {

                //MSD/A设备分两段，全局广播格式为字符串分三段，每段用英文逗号分隔
                //xmap过来的是纯json
                String mqttdata = new String(message.getPayload(), "UTF-8");

                //判断是否为XMAP过来的控制信息
                if(mqttdata.charAt(0)=='{')
                {
                    jsonCommandCtrl(mqttdata);
                }
                else {
                    String[] strArr = mqttdata.split(",");
                    if (2 != strArr.length && 3 != strArr.length) {
                        //判断是否为2和3段内容，否则返回
                        return;
                    }
                    if (strArr[0].equals("FMSD")) //MSD设备搜索服务
                    {
                        rebackMSD_B();
                    } else //if(strArr[0].equals("playscreen"))
                    {
                        String msd_a_json = strArr[2];
                        jsonCommandCtrl(msd_a_json);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else if (ebc.ebusType == EBusType.ebusMQTTDisconnect)
        {}
        else if (ebc.ebusType == EBusType.ebusMQTTPubDone)
        {}
    }

    public WebProcListener jsonCommand = new WebProcListener() {
        @Override
        public void cookies(String url, String cookie) {

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void success_html(String url, String html) {

            try {
                JSONObject person = new JSONObject(html);
                String cmd = person.getString("cmd");

                //重设界面
                initFrmLayout();

                if(cmd.equals("video")) {

                    JSONObject person2 = person.getJSONObject("info");
                    //获取信息
                    JSONArray ary = person2.getJSONArray("v_list");
                    v_list.clear();
                    for(int i=0;i<ary.length();i++) {
                        v_list.add(ary.get(i).toString());
                    }
                    String videoUrl=ary.get(0).toString();
                    //
                    //initLocalVideo();
                    initNetVideo(videoUrl);
                    //
                    mVideo.setVisibility(View.VISIBLE);

                }
                else if(cmd.equals("web")) {
                    JSONObject person2 = person.getJSONObject("info");
                    //获取信息
                    String webUrl = person2.getString("w_url");
                    mWeb.loadUrl(webUrl);
                    //
                    mWeb.setVisibility(View.VISIBLE);
                }
                else if(cmd.equals("pic")) {
                    JSONObject person2 = person.getJSONObject("info");
                    //获取信息
                    JSONArray ary = person2.getJSONArray("p_list");
                    p_list.clear();
                    for(int i=0;i<ary.length();i++) {
                        p_list.add(ary.get(i).toString());
                    }
                    //下载所有图片
                    getPicListBegin();
                    //
                }
                else{
                    //重新获取
                    webCommandInfoDelay();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //
                Toast.makeText(MainActivity.this, "错误：JSON数据解析异常", Toast.LENGTH_SHORT).show();
                //重新获取
                webCommandInfoDelay();
            }

        }

        @Override
        public void fail(String url, String errMsg) {
            Toast.makeText(MainActivity.this, "错误：Web接口未能成功连接", Toast.LENGTH_SHORT).show();
            //重新获取
            webCommandInfoDelay();
        }
    };

    //--------------------------------------------------------------
    //MSD设备
    // 创建JSONObject对象
    private JSONObject createMSDJSONObject() {
        JSONObject result = new JSONObject();
        try {
            result.put("f", "playscreen");
            result.put("n", "PlayScreen-"+msdClientID);
            result.put("s", msdClientID);
            result.put("p", msdClientID+"_");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
    void rebackMSD_B()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String rdata="RMSD,"+msdClientID+","+createMSDJSONObject();
                MqttManager.getInstance().publish("MSD/B", 2, rdata.getBytes());
            }
        }).start();
    }

    //--------------------------------------------------------------
    //指令执行
    void jsonCommandCtrl(String json)
    {
        JSONObject person = null;
        try {
            person = new JSONObject(json);

            String mqttCmd = person.getString("playscreen-cmd");
            if (mqttCmd != null && mqttCmd.equals("reload")) {
                mCommandUrl=mCommandUrl0;
                //重新加载显示数据
                webCommandInfo();
            }
            else if (mqttCmd != null && mqttCmd.equals("mode1")) {
                mCommandUrl=mCommandUrl1;
                //重新加载显示数据
                webCommandInfo();
            }
            else if (mqttCmd != null && mqttCmd.equals("mode2")) {
                mCommandUrl=mCommandUrl2;
                //重新加载显示数据
                webCommandInfo();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //--------------------------------------------------------------
    //
    void showPicList()
    {
        if(nShowPicPos!=-1) {
            //如果图片层非隐藏状态就显示不同的图片
            if (mImg.getVisibility() == View.VISIBLE) {
                //显示图片
                Bitmap bm = BitmapFactory.decodeFile(p_list_download_ok.get(nShowPicPos));
                //将图片显示到ImageView中
                mImg.setImageBitmap(bm);
                nShowPicPos++;
                nShowPicPos %= p_list_download_ok.size();
            }
        }
    }
    //下载播放的图片
    void getPicListBegin()
    {
        nShowPicPos=-1;
        nPicDownloadPos=0;
        p_list_download_ok.clear();
        getPicList();
    }
    void getPicList() {
        if(nPicDownloadPos<p_list.size()) {
            DownloadPic dp = new DownloadPic(this);
            dp.addListener(dpl);
            dp.download(p_list.get(nPicDownloadPos), "pic", nPicDownloadPos+".jpg");
        }
        else if(p_list.size()==nPicDownloadPos)
        {
            //下载完成,在定时器里显示图片
            //
            nShowPicPos=0;
            mImg.setVisibility(View.VISIBLE);
        }
    }
    DownloadPicListener dpl = new DownloadPicListener() {

        @Override
        public void success(String url, String album_filename) {
            Toast.makeText(MainActivity.this, "图片下载成功"+url+"\n"+album_filename, Toast.LENGTH_SHORT).show();
            p_list_download_ok.add(album_filename);
            nPicDownloadPos++;
            getPicList();
        }

        @Override
        public void connect_fail(String url) {
            getPicList();
        }

        @Override
        public void save_pic_fail(String url) {
            Toast.makeText(MainActivity.this, "错误：图片无法保存到本地", Toast.LENGTH_SHORT).show();
        }
    };
}
