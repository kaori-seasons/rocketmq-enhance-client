package org.apache.rocketmq.sdk.message;

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

public class TransactionMessageTest {
    private static final InternalLogger log = ClientLogger.getLog();

    public static void main(String[] args) throws InterruptedException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.ProducerId, "PID_test0005-tran");
        properties.put("AccessKey", "1111111111111111111111110005");
        properties.put("SecretKey", "1111111111111111111111110011005");
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("NAMESRV_ADDR", "10.154.0.233:9876");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, 120000);
        TransactionProducer producer = RMQFactory.createTransactionProducer(properties, new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                System.out.println("execute local logic");
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        });
        producer.start();
        org.apache.rocketmq.sdk.api.Message msg = new org.apache.rocketmq.sdk.api.Message("topic_test0005-tran", "xyzs3", "xyzs3", "hi 3".getBytes());
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
