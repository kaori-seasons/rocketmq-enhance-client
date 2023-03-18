package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxDataSource;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxStatement;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;

import java.sql.Statement;

public class MQStatement extends AbstractMQTxStatement {
    public MQStatement(AbstractMQTxDataSource dataSource, MQTxConnection connection, Statement statement) {
        super(dataSource, connection, statement);
    }
}
