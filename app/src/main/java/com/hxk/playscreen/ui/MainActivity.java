package com.hxk.playscreen.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.hxk.playscreen.R;
import com.hxk.playscreen.ebus.EBusContent;
import com.hxk.playscreen.ebus.EBusType;
import com.hxk.playscreen.mqtt.MqttManager;
import com.lichfaker.log.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    public static final String URL = "tcp://192.168.1.101:1883";
    private String userName = "";
    private String password = "";
    private String clientId = "clientId";
    private boolean isFristConnected=false;
    Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //注册EventBus
        EventBus.getDefault().register(this);
        //主动连接MQTT
        timer.schedule(timerTask,1000,5000);//延时1s，每隔5000毫秒执行一次run方法

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MqttManager.getInstance().publish("test", 2, "hello".getBytes());
                    }
                }).start();
            }
        });

        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MqttManager.getInstance().subscribe("test1", 2);
                    }
                }).start();
            }
        });

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MqttManager.getInstance().disConnect();
                        } catch (MqttException e) {

                        }
                    }
                }).start();

            }
        });
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
                            MqttManager.getInstance().subscribe("test", 2);
                        }
                    }).start();
                }
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

    /**
     * 订阅接收到的消息
     * 这里的Event类型可以根据需要自定义, 这里只做基础的演示
     *
     * @param message
     */
    @Subscribe
    public void onEvent(EBusContent ebc) {
        if (ebc.ebusType == EBusType.ebusMQTTContent)
        {
            //输出test订阅信息的内容
            MqttMessage message = (MqttMessage) ebc.content;
            Logger.d(message.toString());
        }
        else if (ebc.ebusType == EBusType.ebusMQTTDisconnect)
        {}
        else if (ebc.ebusType == EBusType.ebusMQTTPubDone)
        {}
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
