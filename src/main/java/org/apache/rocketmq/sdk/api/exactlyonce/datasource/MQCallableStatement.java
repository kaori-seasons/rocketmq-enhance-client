package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxCallableStatement;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxDataSource;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;

import java.sql.PreparedStatement;

public class MQCallableStatement extends AbstractMQTxCallableStatement {
    public MQCallableStatement(AbstractMQTxDataSource dataSource, MQTxConnection connection, PreparedStatement preparedStatement, String sql) {
        super(dataSource, connection, preparedStatement, sql);
    }
}
