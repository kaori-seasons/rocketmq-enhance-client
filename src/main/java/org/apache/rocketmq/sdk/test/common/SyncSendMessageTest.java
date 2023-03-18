package org.apache.rocketmq.sdk.test.common;

import com.alibaba.druid.wall.violation.ErrorCode;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.Producer;
import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.RMQFactory;
import java.util.Properties;

public class SyncSendMessageTest {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.GROUP_ID, "GID_0608");
        properties.put("INSTANCE_ID", "MQ_INST_xxx");
        properties.put("AccessKey", "b4439888fd394c298292a2f7c31847a4");
        properties.put("SecretKey", "d7a79b7a8ca2407c8ea27dd9cf290aa4");
        properties.put(PropertyKeyConst.MsgTraceSwitch, true);
        properties.put(PropertyKeyConst.AuthenticationRequired, "true");
        properties.put("NAMESRV_ADDR", "xxx.xxx.xxx.xxx:9876");
        properties.put(PropertyKeyConst.SendMsgTimeoutMillis, Integer.valueOf((int) ErrorCode.INTO_OUTFILE));
        Producer traceProducer = RMQFactory.createProducer(properties);
        traceProducer.start();
        Message message = new Message("topic_0608", "TagA", "Key_ccc", "hello rocketmq".getBytes());
        for (int i = 0; i < 10; i++) {
            System.out.println("msgId:->" + traceProducer.send(message).getMessageId());
        }
    }
}
