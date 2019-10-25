package android.hxk.playscreen.mqtt;

import android.hxk.playscreen.ebus.EBusContent;
import android.hxk.playscreen.ebus.EBusType;
import com.lichfaker.log.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

public class MqttCallbackBus implements MqttCallback {

    @Override
    public void connectionLost(Throwable cause) {
        //断开连接
        EBusContent bc=new EBusContent();
        bc.ebusType= EBusType.ebusMQTTDisconnect;
        EventBus.getDefault().post(bc);
        //Logger.e(cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        //接收到的消息弄到event  bus里面去
        EBusContent bc=new EBusContent();
        bc.ebusType= EBusType.ebusMQTTContent;
        bc.content=message;
        EventBus.getDefault().post(bc);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //发布消息完成
        EBusContent bc=new EBusContent();
        bc.ebusType= EBusType.ebusMQTTPubDone;
        EventBus.getDefault().post(bc);
    }


}
