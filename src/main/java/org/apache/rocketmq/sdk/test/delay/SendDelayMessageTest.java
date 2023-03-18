package org.apache.rocketmq.sdk.test.delay;

import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.lang3.time.DateFormatUtils;

public class SendDelayMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_delay");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c318470d");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290ac6");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("PROXY_ADDR", "xxx.xxx.xxx.xxx:8081");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, 300000);
        Producer producer = RMQFactory.createProducer(properties);
        producer.start();
        Message message = new Message("topic_delay", "TagA", "Key_ccc", "hello rocketmq".getBytes());
        message.setStartDeliverTime(System.currentTimeMillis() + 180000);
        for (int i = 0; i < 5; i++) {
            System.out.println("msgId:->" + producer.send(message).getMessageId() + " and sendTime is " + DateFormatUtils.format(new Date(), "yyyy-MM-ss HH:mm:ss"));
        }
        producer.shutdown();
    }
}
