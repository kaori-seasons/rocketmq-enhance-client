package org.apache.rocketmq.sdk.shade.client.consumer.reporter;

import java.util.Map;

public interface ConsumerStatusReporter {
    Map<String, String> reportStatus();
}
