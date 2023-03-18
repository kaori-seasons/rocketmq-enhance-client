package org.apache.rocketmq.sdk.api.exactlyonce;

import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.MessageSelector;
import org.apache.rocketmq.sdk.api.bean.Subscription;
import org.apache.rocketmq.sdk.api.exception.RMQClientException;
import java.util.Map;
import java.util.Properties;

public class ExactlyOnceConsumerBean implements ExactlyOnceConsumer {
    private Properties properties;
    private Map<Subscription, MessageListener> subscriptionTable;
    private ExactlyOnceConsumer exactlyOnceConsumer;

    @Override 
    public boolean isStarted() {
        return this.exactlyOnceConsumer.isStarted();
    }

    @Override 
    public boolean isClosed() {
        return this.exactlyOnceConsumer.isClosed();
    }

    @Override
    public void start() {
        if (null == this.properties) {
            throw new RMQClientException("properties not set");
        } else if (null == this.subscriptionTable) {
            throw new RMQClientException("subscriptionTable not set");
        } else {
            this.exactlyOnceConsumer = ExactlyOnceRMQFactory.createExactlyOnceConsumer(this.properties);
            for (Map.Entry<Subscription, MessageListener> next : this.subscriptionTable.entrySet()) {
                subscribe(next.getKey().getTopic(), next.getKey().getExpression(), next.getValue());
            }
            this.exactlyOnceConsumer.start();
        }
    }

    @Override 
    public void updateCredential(Properties credentialProperties) {
        if (this.exactlyOnceConsumer != null) {
            this.exactlyOnceConsumer.updateCredential(credentialProperties);
        }
    }

    @Override 
    public void shutdown() {
        if (this.exactlyOnceConsumer != null) {
            this.exactlyOnceConsumer.shutdown();
        }
    }

    @Override 
    public void subscribe(String topic, String subExpression, MessageListener listener) {
        if (null == this.exactlyOnceConsumer) {
            throw new RMQClientException("subscribe must be called after ExactlyOnceConsumerBean started");
        }
        this.exactlyOnceConsumer.subscribe(topic, subExpression, listener);
    }

    @Override 
    public void subscribe(String topic, MessageSelector selector, MessageListener listener) {
        if (null == this.exactlyOnceConsumer) {
            throw new RMQClientException("subscribe must be called after ExactlyOnceConsumerBean started");
        }
        this.exactlyOnceConsumer.subscribe(topic, selector, listener);
    }

    public Properties getProperties() {
        return this.properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<Subscription, MessageListener> getSubscriptionTable() {
        return this.subscriptionTable;
    }

    public void setSubscriptionTable(Map<Subscription, MessageListener> subscriptionTable) {
        this.subscriptionTable = subscriptionTable;
    }
}
