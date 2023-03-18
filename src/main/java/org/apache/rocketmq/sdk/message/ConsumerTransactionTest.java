package org.apache.rocketmq.sdk.message;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class ConsumerTransactionTest {
    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.ConsumerId, "CID_test0005-tran");
        properties.put("AccessKey", "1111111111111111111111110005");
        properties.put("SecretKey", "1111111111111111111111110011005");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put("NAMESRV_ADDR", "10.154.0.233:9876");
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put(PropertyKeyConst.ConsumeTimeout, 120000);
        Consumer traceConsumer = RMQFactory.createConsumer(properties);
        traceConsumer.subscribe("topic_test0005-tran", "*", new MessageListener() {
            @Override
            public Action consume(Message message, ConsumeContext context) {
                System.out.println("Receive: " + message.toString());
                return Action.CommitMessage;
            }
        });
        traceConsumer.start();
    }
}
