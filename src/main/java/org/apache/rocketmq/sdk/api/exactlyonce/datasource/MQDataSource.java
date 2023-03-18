package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.AbstractMQTxDataSource;

import javax.sql.DataSource;

public class MQDataSource extends AbstractMQTxDataSource {
    public MQDataSource(DataSource druidDataSource) {
        super(druidDataSource);
    }

    public MQDataSource() {
    }

    public MQDataSource(String url, String userName, String password, String driverClass) {
        setUrl(url);
        setUsername(userName);
        setPassword(password);
        setDriverClass(driverClass);
    }
}
