package org.apache.rocketmq.sdk.test.batch;

import org.apache.rocketmq.sdk.shade.client.Validators;
import org.apache.rocketmq.sdk.shade.client.exception.MQClientException;
import org.apache.rocketmq.sdk.shade.client.producer.DefaultMQProducer;
import org.apache.rocketmq.sdk.shade.client.producer.SendCallback;
import org.apache.rocketmq.sdk.shade.client.producer.SendResult;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageBatch;
import org.apache.rocketmq.sdk.shade.common.message.MessageClientIDSetter;
import org.apache.rocketmq.sdk.shade.common.protocol.NamespaceUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BatchAsyncSendMessageTest {
    public BatchAsyncSendMessageTest() {
    }

    public static void main(String[] args) throws Exception {
        List<Message> messages = new LinkedList();
        Message message = new Message("topic_test03", "tagA", "Key_ccc", "hello rocketmq".getBytes());

        for(int i = 0; i < 5; ++i) {
            messages.add(message);
        }

        final DefaultMQProducer producer = new DefaultMQProducer("MQ_INST_xxx", "PID_test01");
        producer.setSendMsgTimeout(12000);
        producer.setNamesrvAddr("xxx.xxx.xxx.xxx:9876");
        producer.start();
        MessageBatch messageBatch = batch(messages, producer);
        producer.send(messageBatch, new SendCallback() {
            public void onSuccess(SendResult sendResult) {
                System.out.println(sendResult);
                producer.shutdown();
            }

            public void onException(Throwable e) {
                System.out.println(e);
                e.printStackTrace();
                producer.shutdown();
            }
        });
    }

    private static MessageBatch batch(Collection<Message> msgs, DefaultMQProducer defaultMQProducer) throws MQClientException {
        MessageBatch msgBatch;
        try {
            msgBatch = MessageBatch.generateFromList(msgs);
            Iterator var3 = msgBatch.iterator();

            while(true) {
                if (!var3.hasNext()) {
                    msgBatch.setBody(msgBatch.encode());
                    break;
                }

                Message message = (Message)var3.next();
                Validators.checkMessage(message, defaultMQProducer);
                MessageClientIDSetter.setUniqID(message);
                message.setTopic(NamespaceUtil.wrapNamespace(defaultMQProducer.getNamespace(), message.getTopic()));
            }
        } catch (Exception var5) {
            throw new MQClientException("Failed to initiate the MessageBatch", var5);
        }

        msgBatch.setTopic(NamespaceUtil.wrapNamespace(defaultMQProducer.getNamespace(), msgBatch.getTopic()));
        return msgBatch;
    }
}
