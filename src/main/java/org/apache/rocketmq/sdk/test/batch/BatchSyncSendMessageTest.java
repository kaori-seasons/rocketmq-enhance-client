package org.apache.rocketmq.sdk.test.batch;

import org.apache.rocketmq.sdk.shade.client.exception.MQBrokerException;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingException;

import java.util.LinkedList;
import java.util.List;

public class BatchSyncSendMessageTest {
    public static void main(String[] args) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        List<Message> messages = new LinkedList<>();
        Message message = new Message("topic_1009", "tagA", "Key_ccc", "hello rocketmq".getBytes());
        for (int i = 0; i < 5; i++) {
            messages.add(message);
        }
        DefaultMQProducer producer = new DefaultMQProducer("MQ_INST_xxx", "PID_test01");
        producer.setSendMsgTimeout(12000);
        producer.setNamesrvAddr("xxx.xxx.xxx.xxx:9876");
        producer.start();
        System.out.println(producer.send(messages));
        producer.shutdown();
    }
}
