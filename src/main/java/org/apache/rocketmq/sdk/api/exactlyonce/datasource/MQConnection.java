package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import com.alibaba.druid.pool.DruidPooledConnection;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxDataSource;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;

public class MQConnection extends MQTxConnection {
    public MQConnection(DruidPooledConnection connection, AbstractMQTxDataSource dataSource) {
        super(dataSource, connection);
    }
}
