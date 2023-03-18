package org.apache.rocketmq.sdk.test.transcation;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class ConsumeTransactionMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_trans");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.MaxReconsumeTimes, 3);
        Consumer traceConsumer = RMQFactory.createConsumer(properties);
        traceConsumer.subscribe("topic_trans", "*", new MessageListener() {
            @Override
            public Action consume(Message message, ConsumeContext context) {
                System.out.println("Receive topic_trans message " + message.getMsgID());
                return Action.CommitMessage;
            }
        });
        traceConsumer.start();
        System.out.println("consumer start success");
    }
}
