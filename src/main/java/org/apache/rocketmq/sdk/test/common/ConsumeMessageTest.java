package org.apache.rocketmq.sdk.test.common;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Consumer;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.PropertyValueConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.shade.logging.inner.Level;

import java.util.Properties;

public class ConsumeMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_0608");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c31847a4");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290aa4");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("NAMESRV_ADDR", "xxx.xxx.xxx.xxx:9876");
        properties.put(PropertyKeyConst.ConsumeTimeout, Integer.valueOf((int) Level.WARN_INT));
        properties.put(PropertyKeyConst.MaxReconsumeTimes, 3);
        properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
        Consumer traceConsumer = RMQFactory.createConsumer(properties);
        traceConsumer.subscribe("topic_0608", "*", new MessageListener() {
            @Override
            public Action consume(Message message, ConsumeContext context) {
                System.out.println("Receive message " + message.getMsgID() + ", msgBody: " + new String(message.getBody()));
                return Action.CommitMessage;
            }
        });
        traceConsumer.start();
        System.out.println("consumer start success");
    }
}
