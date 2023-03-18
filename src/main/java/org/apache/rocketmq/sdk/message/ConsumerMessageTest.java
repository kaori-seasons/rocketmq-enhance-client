package org.apache.rocketmq.sdk.message;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class ConsumerMessageTest {
    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.ConsumerId, "CID_zjd_test0001-2");
        properties.put("AccessKey", "22222222222222222222222001");
        properties.put("SecretKey", "2222222222222222222222233333001");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put("NAMESRV_ADDR", "10.154.0.136:9876");
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put(PropertyKeyConst.ConsumeTimeout, 120000);
        Consumer traceConsumer = RMQFactory.createConsumer(properties);
        traceConsumer.subscribe("topic_zjd_test0001", "*", new MessageListener() {
            @Override
            public Action consume(Message message, ConsumeContext context) {
                System.out.println("Receive: " + message.toString());
                return Action.ReconsumeLater;
            }
        });
        traceConsumer.start();
    }
}
