package org.apache.rocketmq.sdk.test.exactlyonce;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.exactlyonce.ExactlyOnceRMQFactory;
import java.util.Date;
import java.util.Properties;

public class TestExactlyOnceProducer {

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("GROUP_ID", "GID_0606");
        properties.put("INSTANCE_ID", "MMQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c31847a4");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290aa4");
        properties.put("MsgTraceSwitch", true);
        properties.put("AuthenticationRequired", "true");
        properties.put("NAMESRV_ADDR", "xxx.xxx.xxx.xxx:9876");
        properties.put("SendMsgTimeoutMillis", 3000);
        properties.setProperty("exactlyOnceDelivery", "true");
        Producer producer = ExactlyOnceRMQFactory.createProducer(properties);
        producer.start();
        System.out.println("Producer Started");

        for(int i = 0; i < 1; ++i) {
            Message message = new Message("topic_0606", "tagA", "hello world".getBytes());

            try {
                SendResult sendResult = producer.send(message);

                assert sendResult != null;

                System.out.println(new Date() + " Send mq message success! msgId is: " + sendResult.getMessageId());
            } catch (Exception var6) {
                System.out.println("发送失败");
            }
        }

        producer.shutdown();
    }
}
