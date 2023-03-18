package org.apache.rocketmq.sdk.test.order;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.api.order.ConsumeOrderContext;
import org.apache.rocketmq.sdk.api.order.MessageOrderListener;
import org.apache.rocketmq.sdk.api.order.OrderAction;
import org.apache.rocketmq.sdk.api.order.OrderConsumer;
import java.util.Properties;

public class ConsumeOrderMessageTest {
    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_order");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        OrderConsumer consumer = RMQFactory.createOrderedConsumer(properties);
        consumer.subscribe("topic_order", "*", new MessageOrderListener() {
            @Override
            public OrderAction consume(Message message, ConsumeOrderContext context) {
                System.out.println("msgId" + message.getMsgID() + ", body " + new String(message.getBody()));
                return OrderAction.Success;
            }
        });
        consumer.start();
    }
}
