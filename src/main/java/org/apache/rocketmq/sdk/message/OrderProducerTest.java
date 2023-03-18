package org.apache.rocketmq.sdk.message;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.api.order.OrderProducer;
import java.util.Date;
import java.util.Properties;

public class OrderProducerTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.ProducerId, "PID_test0001");
        properties.put("AccessKey", "1111111111111111111111110001");
        properties.put("SecretKey", "1111111111111111111111110011001");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put("NAMESRV_ADDR", "10.154.0.233:9876");
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        OrderProducer producer = RMQFactory.createOrderProducer(properties);
        producer.start();
        for (int i = 0; i < 50; i++) {
            String orderId = "biz_" + (i % 20);
            Message msg = new Message("topic_test0001", orderId, ("send order global msg:" + i).getBytes());
            msg.setKey(orderId);
            try {
                SendResult sendResult = producer.send(msg, String.valueOf(orderId));
                if (sendResult != null) {
                    System.out.println(new Date() + " Send mq message success. Topic is:" + msg.getTopic() + " msgId is: " + sendResult.getMessageId());
                }
            } catch (Exception e) {
                System.out.println(new Date() + " Send mq message failed. Topic is:" + msg.getTopic());
                e.printStackTrace();
            }
        }
    }
}
