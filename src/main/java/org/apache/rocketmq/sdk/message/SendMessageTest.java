package org.apache.rocketmq.sdk.message;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class SendMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.ProducerId, "PID_test0001");
        properties.put("AccessKey", "1111111111111111111111110001");
        properties.put("SecretKey", "1111111111111111111111110011001");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put("NAMESRV_ADDR", "10.154.0.136:9876");
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, 120000);
        Producer traceProducer = RMQFactory.createProducer(properties);
        traceProducer.start();
        Message message = new Message("topic_test0001", "TagA", "Key_ccc", "hello rocketmq".getBytes());
        for (int j = 0; j < 1; j++) {
            for (int i = 0; i < 1000; i++) {
                String.valueOf("biz_" + (i % 10));
                System.out.println("msgId:->" + traceProducer.send(message).getMessageId());
            }
        }
        traceProducer.shutdown();
    }
}
