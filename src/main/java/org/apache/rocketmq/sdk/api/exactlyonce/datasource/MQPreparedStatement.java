package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxDataSource;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxPreparedStatement;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;

import java.sql.PreparedStatement;

public class MQPreparedStatement extends AbstractMQTxPreparedStatement {
    public MQPreparedStatement(AbstractMQTxDataSource dataSource, MQTxConnection connection, PreparedStatement statement, String sql) {
        super(dataSource, connection, statement, sql);
    }
}
