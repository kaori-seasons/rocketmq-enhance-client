package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

import com.alibaba.druid.pool.DruidPooledConnection;
import org.apache.rocketmq.sdk.api.exactlyonce.MQUnsupportException;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.LocalTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy.ProxyTxExecuter;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.MQCallableStatement;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.MQPreparedStatement;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.MQStatement;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.TransactionManager;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.DBAccessUtil;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class MQTxConnection implements Connection {
    private DruidPooledConnection targetConn;
    private AbstractMQTxDataSource dateSource;

    public MQTxConnection(AbstractMQTxDataSource dataSource, DruidPooledConnection connection) {
        this.dateSource = dataSource;
        this.targetConn = connection;
    }

    public AbstractMQTxDataSource getTxDateSource() {
        return this.dateSource;
    }

    public Connection getTargetConnection() {
        return this.targetConn;
    }

    @Override 
    public Statement createStatement() throws SQLException {
        return new MQStatement(this.dateSource, this, this.targetConn.createStatement());
    }

    @Override 
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new MQPreparedStatement(this.dateSource, this, this.targetConn.prepareStatement(sql), sql);
    }

    @Override 
    public CallableStatement prepareCall(String sql) throws SQLException {
        return new MQCallableStatement(this.dateSource, this, this.targetConn.prepareCall(sql), sql);
    }

    @Override 
    public String nativeSQL(String sql) throws SQLException {
        return this.targetConn.nativeSQL(sql);
    }

    @Override 
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        MQTxContext txContext = LocalTxContext.get();
        if (txContext != null && txContext.isInTxEnv()) {
            txContext.setAutoCommit(autoCommit);
        }
        this.targetConn.setAutoCommit(autoCommit);
    }

    @Override 
    public boolean getAutoCommit() throws SQLException {
        return this.targetConn.getAutoCommit();
    }

    @Override 
    public void commit() throws SQLException {
        MQTxContext context = LocalTxContext.get();
        if (context == null || !context.isInTxEnv()) {
            this.targetConn.commit();
            return;
        }
        try {
            TransactionManager.flushTxRecord(this, context);
            this.targetConn.commit();
            ProxyTxExecuter.getInstance().commit();
            context.setInTxEnv(false);
        } catch (Exception e) {
            if (!DBAccessUtil.isRecordDupException(context, e)) {
                throw new SQLException(e);
            }
            this.targetConn.rollback();
            context.setDup(true);
            ProxyTxExecuter.getInstance().rollback();
            context.setInTxEnv(false);
        }
    }

    @Override 
    public void rollback() throws SQLException {
        MQTxContext context = LocalTxContext.get();
        if (context == null || !context.isInTxEnv()) {
            this.targetConn.rollback();
            return;
        }
        try {
            context.setInTxEnv(false);
            this.targetConn.rollback();
            ProxyTxExecuter.getInstance().rollback();
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        this.targetConn.close();
    }

    @Override 
    public boolean isClosed() throws SQLException {
        return this.targetConn.isClosed();
    }

    @Override 
    public DatabaseMetaData getMetaData() throws SQLException {
        return this.targetConn.getMetaData();
    }

    @Override 
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.targetConn.setReadOnly(readOnly);
    }

    @Override 
    public boolean isReadOnly() throws SQLException {
        return this.targetConn.isReadOnly();
    }

    @Override 
    public void setCatalog(String catalog) throws SQLException {
        this.targetConn.setCatalog(catalog);
    }

    @Override 
    public String getCatalog() throws SQLException {
        return this.targetConn.getCatalog();
    }

    @Override 
    public void setTransactionIsolation(int level) throws SQLException {
        this.targetConn.setTransactionIsolation(level);
    }

    @Override 
    public int getTransactionIsolation() throws SQLException {
        return this.targetConn.getTransactionIsolation();
    }

    @Override 
    public SQLWarning getWarnings() throws SQLException {
        return this.targetConn.getWarnings();
    }

    @Override 
    public void clearWarnings() throws SQLException {
        this.targetConn.clearWarnings();
    }

    @Override 
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MQStatement(this.dateSource, this, this.targetConn.createStatement(resultSetType, resultSetConcurrency));
    }

    @Override 
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MQPreparedStatement(this.dateSource, this, this.targetConn.prepareStatement(sql, resultSetType, resultSetConcurrency), sql);
    }

    @Override 
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MQCallableStatement(this.dateSource, this, this.targetConn.prepareCall(sql, resultSetType, resultSetConcurrency), sql);
    }

    @Override 
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return this.targetConn.getTypeMap();
    }

    @Override 
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.targetConn.setTypeMap(map);
    }

    @Override 
    public void setHoldability(int holdability) throws SQLException {
        this.targetConn.setHoldability(holdability);
    }

    @Override 
    public int getHoldability() throws SQLException {
        return this.targetConn.getHoldability();
    }

    @Override 
    public Savepoint setSavepoint() throws SQLException {
        return this.targetConn.setSavepoint();
    }

    @Override 
    public Savepoint setSavepoint(String name) throws SQLException {
        return this.targetConn.setSavepoint(name);
    }

    @Override 
    public void rollback(Savepoint savepoint) throws SQLException {
        this.targetConn.rollback(savepoint);
    }

    @Override 
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.targetConn.releaseSavepoint(savepoint);
    }

    @Override 
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MQStatement(this.dateSource, this, this.targetConn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override 
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MQPreparedStatement(this.dateSource, this, this.targetConn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
    }

    @Override 
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MQCallableStatement(this.dateSource, this, this.targetConn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql);
    }

    @Override 
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new MQPreparedStatement(this.dateSource, this, this.targetConn.prepareStatement(sql, autoGeneratedKeys), sql);
    }

    @Override 
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new MQPreparedStatement(this.dateSource, this, this.targetConn.prepareStatement(sql, columnIndexes), sql);
    }

    @Override 
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new MQPreparedStatement(this.dateSource, this, this.targetConn.prepareStatement(sql, columnNames), sql);
    }

    @Override 
    public Clob createClob() throws SQLException {
        return this.targetConn.createClob();
    }

    @Override 
    public Blob createBlob() throws SQLException {
        return this.targetConn.createBlob();
    }

    @Override 
    public NClob createNClob() throws SQLException {
        return this.targetConn.createNClob();
    }

    @Override 
    public SQLXML createSQLXML() throws SQLException {
        return this.targetConn.createSQLXML();
    }

    @Override 
    public boolean isValid(int timeout) throws SQLException {
        return this.targetConn.isValid(timeout);
    }

    @Override 
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        this.targetConn.setClientInfo(name, value);
    }

    @Override 
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.targetConn.setClientInfo(properties);
    }

    @Override 
    public String getClientInfo(String name) throws SQLException {
        return this.targetConn.getClientInfo(name);
    }

    @Override 
    public Properties getClientInfo() throws SQLException {
        return this.targetConn.getClientInfo();
    }

    @Override 
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return this.targetConn.createArrayOf(typeName, elements);
    }

    @Override 
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return this.targetConn.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        throw new MQUnsupportException();
    }

    public String getSchema() throws SQLException {
        throw new MQUnsupportException();
    }

    public void abort(Executor executor) throws SQLException {
        throw new MQUnsupportException();
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new MQUnsupportException();
    }

    public int getNetworkTimeout() throws SQLException {
        throw new MQUnsupportException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return (T) this.targetConn.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.targetConn.isWrapperFor(iface);
    }
}
