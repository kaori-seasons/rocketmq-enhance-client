package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import org.apache.rocketmq.sdk.api.exactlyonce.MQUnsupportException;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.MQConnection;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractMQTxDataSource implements DataSource {
    private String url;
    private String username;
    private String password;
    private String driverClass;
    private DruidDataSource targetDataSource;
    private DataSourceConfig config;

    public AbstractMQTxDataSource() {
        this.config = new DataSourceConfig();
        this.targetDataSource = new DruidDataSource();
    }

    public AbstractMQTxDataSource(DataSource targetDataSource) {
        this.config = new DataSourceConfig();
        if (targetDataSource instanceof DruidDataSource) {
            DruidDataSource druidDataSource = (DruidDataSource) targetDataSource;
            this.targetDataSource = druidDataSource;
            if (StringUtils.isNotEmpty(druidDataSource.getUrl())) {
                setUrl(druidDataSource.getUrl());
            }
            if (StringUtils.isNotEmpty(druidDataSource.getUsername())) {
                setUsername(druidDataSource.getUsername());
            }
            if (StringUtils.isNotEmpty(druidDataSource.getPassword())) {
                setPassword(druidDataSource.getPassword());
            }
            if (StringUtils.isNotEmpty(druidDataSource.getDriverClassName())) {
                setDriverClass(druidDataSource.getDriverClassName());
                return;
            }
            return;
        }
        throw new MQUnsupportException("Unsupported DataSource type");
    }

    @Override 
    public Connection getConnection() throws SQLException {
        DruidPooledConnection druidPooledConnection = this.targetDataSource.getConnection();
        this.config.setProductName(druidPooledConnection.getMetaData().getDatabaseProductName());
        return new MQConnection(druidPooledConnection, this);
    }

    @Override 
    public Connection getConnection(String username, String password) throws SQLException {
        return new MQConnection((DruidPooledConnection) targetDatasource().getConnection(username, password), this);
    }

    public String getUrl() {
        return this.targetDataSource.getUrl();
    }

    public void setUrl(String url) {
        this.url = url;
        this.targetDataSource.setUrl(url);
        this.config.setUrl(url);
    }

    public String getUsername() {
        return this.targetDataSource.getUsername();
    }

    public void setUsername(String username) {
        this.username = username;
        this.targetDataSource.setUsername(username);
        this.config.setUser(username);
    }

    public String getPassword() {
        return this.targetDataSource.getPassword();
    }

    public void setPassword(String password) {
        this.password = password;
        this.targetDataSource.setPassword(password);
        this.config.setPasswd(password);
    }

    public String getDriverClass() {
        return this.targetDataSource.getDriverClassName();
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
        this.targetDataSource.setDriverClassName(driverClass);
        this.config.setDriver(driverClass);
    }

    public String getDriverClassName() {
        return this.targetDataSource.getDriverClassName();
    }

    public void setDriverClassName(String driverClass) {
        this.driverClass = driverClass;
        this.targetDataSource.setDriverClassName(driverClass);
        this.config.setDriver(driverClass);
    }

    public DruidDataSource targetDatasource() {
        return this.targetDataSource;
    }

    public DataSourceConfig getDataSourceConfig() {
        if (this.config == null) {
            this.config = new DataSourceConfig(getUrl(), getUsername(), getPassword(), getDriverClass());
        }
        return this.config;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return (T) this.targetDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.targetDataSource.isWrapperFor(iface);
    }

    @Override 
    public PrintWriter getLogWriter() throws SQLException {
        return this.targetDataSource.getLogWriter();
    }

    @Override 
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.targetDataSource.setLogWriter(out);
    }

    @Override 
    public void setLoginTimeout(int seconds) throws SQLException {
        this.targetDataSource.setLoginTimeout(seconds);
    }

    @Override 
    public int getLoginTimeout() throws SQLException {
        return this.targetDataSource.getLoginTimeout();
    }

    @Override 
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.targetDataSource.getParentLogger();
    }

    public void init() throws SQLException {
        this.targetDataSource.init();
    }

    public void close() {
        this.targetDataSource.close();
    }

    public int getInitialSize() {
        return this.targetDataSource.getInitialSize();
    }

    public void setInitialSize(int initialSize) {
        this.targetDataSource.setInitialSize(initialSize);
    }

    public int getMinIdle() {
        return this.targetDataSource.getMinIdle();
    }

    public void setMinIdle(int value) {
        this.targetDataSource.setMinIdle(value);
    }

    public int getMaxActive() {
        return this.targetDataSource.getMaxActive();
    }

    public void setMaxActive(int maxActive) {
        this.targetDataSource.setMaxActive(maxActive);
    }

    public long getMaxWait() {
        return this.targetDataSource.getMaxWait();
    }

    public void setMaxWait(long maxWaitMillis) {
        this.targetDataSource.setMaxWait(maxWaitMillis);
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return this.targetDataSource.getTimeBetweenEvictionRunsMillis();
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.targetDataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    public long getMinEvictableIdleTimeMillis() {
        return this.targetDataSource.getMinEvictableIdleTimeMillis();
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.targetDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    public String getValidationQuery() {
        return this.targetDataSource.getValidationQuery();
    }

    public void setValidationQuery(String validationQuery) {
        this.targetDataSource.setValidationQuery(validationQuery);
    }

    public boolean isTestWhileIdle() {
        return this.targetDataSource.isTestWhileIdle();
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.targetDataSource.setTestWhileIdle(testWhileIdle);
    }

    public boolean isTestOnBorrow() {
        return this.targetDataSource.isTestOnBorrow();
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.targetDataSource.setTestOnBorrow(testOnBorrow);
    }

    public boolean isTestOnReturn() {
        return this.targetDataSource.isTestOnReturn();
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.targetDataSource.setTestOnReturn(testOnReturn);
    }

    public boolean isPoolPreparedStatements() {
        return this.targetDataSource.isPoolPreparedStatements();
    }

    public int getMaxPoolPreparedStatementPerConnectionSize() {
        return this.targetDataSource.getMaxPoolPreparedStatementPerConnectionSize();
    }

    public void setMaxPoolPreparedStatementPerConnectionSize(int maxPoolPreparedStatementPerConnectionSize) {
        this.targetDataSource.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);
    }

    public String[] getFilterClasses() {
        return this.targetDataSource.getFilterClasses();
    }

    public void setFilters(String filters) throws SQLException {
        this.targetDataSource.setFilters(filters);
    }

    public Properties getConnectProperties() {
        return this.targetDataSource.getConnectProperties();
    }

    public void setConnectionProperties(String connectionProperties) {
        this.targetDataSource.setConnectionProperties(connectionProperties);
    }

    public boolean sameConfig(AbstractMQTxDataSource dataSource) {
        if (this == dataSource) {
            return true;
        }
        if (this.url != null) {
            if (!this.url.equals(dataSource.getUrl())) {
                return false;
            }
        } else if (dataSource.getUsername() != null) {
            return false;
        }
        if (this.username != null) {
            if (!this.username.equals(dataSource.getUsername())) {
                return false;
            }
        } else if (dataSource.getUsername() != null) {
            return false;
        }
        if (this.password != null) {
            if (!this.password.equals(dataSource.getPassword())) {
                return false;
            }
        } else if (dataSource.getPassword() != null) {
            return false;
        }
        return this.driverClass != null ? this.driverClass.equals(dataSource.getDriverClass()) : dataSource.getDriverClass() == null;
    }
}
