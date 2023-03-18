package org.apache.rocketmq.sdk.test.order;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.api.order.OrderProducer;
import org.apache.rocketmq.sdk.shade.logging.inner.Level;

import java.util.Date;
import java.util.Properties;

public class SendOrderMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_order");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, Integer.valueOf((int) Level.WARN_INT));
        OrderProducer producer = RMQFactory.createOrderProducer(properties);
        producer.start();
        for (int i = 0; i < 50; i++) {
            String orderId = "biz_" + (i % 20);
            Message msg = new Message("topic_order", orderId, ("send order global msg:" + i).getBytes());
            msg.setKey(orderId);
            try {
                SendResult sendResult = producer.send(msg, "aaa");
                if (sendResult != null) {
                    System.out.println("Send mq message success. Topic:" + msg.getTopic() + " msgId is" + sendResult.getMessageId());
                }
            } catch (Exception e) {
                System.out.println(new Date() + " Send mq message failed. Topic:" + msg.getTopic());
                e.printStackTrace();
            }
        }
    }
}
