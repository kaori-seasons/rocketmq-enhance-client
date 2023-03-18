package org.apache.rocketmq.sdk.test.batch;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.PropertyValueConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.api.batch.BatchConsumer;
import org.apache.rocketmq.sdk.api.batch.BatchMessageListener;
import java.util.List;
import java.util.Properties;

public class BatchConsumeMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_common");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.setProperty(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.MaxReconsumeTimes, 3);
        properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
        properties.setProperty(PropertyKeyConst.ConsumeMessageBatchMaxSize, String.valueOf(10));
        properties.setProperty(PropertyKeyConst.BatchConsumeMaxAwaitDurationInSeconds, String.valueOf(5));
        BatchConsumer consumer = RMQFactory.createBatchConsumer(properties);
        consumer.subscribe("topic_common", "*", new BatchMessageListener() {
            @Override
            public Action consume(List<Message> messages, ConsumeContext context) {
                System.out.printf(Thread.currentThread().getName() + " 批量消息消费数量: %d\n", Integer.valueOf(messages.size()));
                for (Message message : messages) {
                    System.out.println(new String(message.getBody()));
                }
                return Action.CommitMessage;
            }
        });
        consumer.start();
        System.out.println("consumer start success");
    }
}
