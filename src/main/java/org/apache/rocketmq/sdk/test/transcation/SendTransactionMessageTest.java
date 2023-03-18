package org.apache.rocketmq.sdk.test.transcation;

import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.RMQFactory;
import org.apache.rocketmq.sdk.api.transaction.TransactionProducer;
import org.apache.rocketmq.sdk.shade.client.log.ClientLogger;
import org.apache.rocketmq.sdk.shade.client.producer.LocalTransactionState;
import org.apache.rocketmq.sdk.shade.client.producer.TransactionListener;
import org.apache.rocketmq.sdk.shade.common.message.Message;
import org.apache.rocketmq.sdk.shade.common.message.MessageExt;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SendTransactionMessageTest {
    private static final InternalLogger log = ClientLogger.getLog();

    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_trans");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, 120000);
        TransactionProducer producer = RMQFactory.createTransactionProducer(properties, new TransactionListener() {
            @Override 
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override 
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        });
        producer.start();
        org.apache.rocketmq.sdk.api.Message msg = new org.apache.rocketmq.sdk.api.Message("topic_trans", "xyzs3", "xyzs3", "hi 3".getBytes());
        try {
            SendResult sendResult = producer.send(msg, null);
            System.out.println("send transaction message succeed. msg=" + msg.toString());
            System.out.println("msgId:->" + sendResult.getMessageId());
        } catch (Exception e) {
            System.out.println(new Date() + " Send mq message failed. Topic is:" + msg.getTopic());
            e.printStackTrace();
        }
        TimeUnit.MILLISECONDS.sleep(2147483647L);
        producer.shutdown();
    }
}
