package org.apache.rocketmq.sdk.test.common;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.OnExceptionContext;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.SendCallback;
import org.apache.rocketmq.sdk.api.SendResult;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class AsyncSendMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_0301");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c31847af");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290aaf");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, 300000);
        Producer traceProducer = RMQFactory.createProducer(properties);
        traceProducer.start();
        Message message = new Message("topic_0301", "TagA", "Key_ccc", "hello rocketmq".getBytes());
        for (int i = 0; i < 5; i++) {
            traceProducer.sendAsync(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    System.out.println("send message success. topic=" + sendResult.getTopic() + ", msgId=" + sendResult.getMessageId());
                }

                @Override
                public void onException(OnExceptionContext context) {
                    System.out.println("send message failed. topic=" + context.getTopic() + ", msgId=" + context.getMessageId());
                }
            });
        }
    }
}
