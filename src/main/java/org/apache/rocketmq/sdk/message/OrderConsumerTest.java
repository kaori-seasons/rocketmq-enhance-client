package org.apache.rocketmq.sdk.message;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.api.order.ConsumeOrderContext;
import org.apache.rocketmq.sdk.api.order.MessageOrderListener;
import org.apache.rocketmq.sdk.api.order.OrderAction;
import org.apache.rocketmq.sdk.api.order.OrderConsumer;
import java.util.Properties;
import org.springframework.util.backoff.ExponentialBackOff;

public class OrderConsumerTest {
    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.ConsumerId, "CID_test0001");
        properties.put("AccessKey", "1111111111111111111111110001");
        properties.put("SecretKey", "1111111111111111111111110011001");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put("NAMESRV_ADDR", "10.154.0.233:9876");
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        OrderConsumer consumer = RMQFactory.createOrderedConsumer(properties);
        consumer.subscribe("topic_test0001", "*", new MessageOrderListener() {
            @Override
            public OrderAction consume(Message message, ConsumeOrderContext context) {
                System.out.println("msgId" + message.getMsgID() + "  body" + new String(message.getBody()));
                return OrderAction.Success;
            }
        });
        consumer.start();
        Thread.sleep(ExponentialBackOff.DEFAULT_MAX_INTERVAL);
    }
}
