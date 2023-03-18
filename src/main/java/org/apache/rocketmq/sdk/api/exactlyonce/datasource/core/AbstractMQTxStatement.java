package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

import org.apache.rocketmq.sdk.api.exactlyonce.MQUnsupportException;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.LocalTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.SQLUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.MetricService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public abstract class AbstractMQTxStatement implements Statement {
    protected AbstractMQTxDataSource dataSource;
    protected MQTxConnection conn;
    protected Statement targetStatement;
    protected String targetSql;

    public AbstractMQTxStatement(AbstractMQTxDataSource dataSource, MQTxConnection connection, Statement statement) {
        this.dataSource = dataSource;
        this.conn = connection;
        this.targetStatement = statement;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        this.targetSql = sql;
        return this.targetStatement.executeQuery(sql);
    }

    public int executeUpdate(String sql) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv() && SQLUtil.isWriteSql(sql)) {
            MetricService.getInstance().incWrite();
            ExecResult<Integer> result = ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTEUPDATE_STRING, (Object)null);
            return (Integer)result.getResult();
        } else {
            return this.targetStatement.executeUpdate(sql);
        }
    }

    public void close() throws SQLException {
        this.targetStatement.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return this.targetStatement.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        this.targetStatement.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return this.targetStatement.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        this.targetStatement.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        this.targetStatement.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return this.targetStatement.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        this.targetStatement.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        this.targetStatement.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return this.targetStatement.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        this.targetStatement.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        this.targetStatement.setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            if (SQLUtil.isReadSql(sql)) {
                this.executeQuery(sql);
                return true;
            }

            if (SQLUtil.isWriteSql(sql)) {
                MetricService.getInstance().incWrite();
                ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTE_STRING, (Object)null);
                return false;
            }
        }

        return this.targetStatement.execute(sql);
    }

    public ResultSet getResultSet() throws SQLException {
        return this.targetStatement.getResultSet();
    }

    public int getUpdateCount() throws SQLException {
        return this.targetStatement.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return this.targetStatement.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        this.targetStatement.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return this.targetStatement.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        this.targetStatement.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return this.targetStatement.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return this.targetStatement.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return this.targetStatement.getResultSetType();
    }

    public void addBatch(String sql) throws SQLException {
        this.targetStatement.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        this.targetStatement.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            MetricService.getInstance().incWrite();
            ExecResult<int[]> result = ExecuterFactory.execute(this, (String)null, SQLExecuteType.EXECUTEBATCH_VOID, (Object)null);
            return (int[])result.getResult();
        } else {
            return this.targetStatement.executeBatch();
        }
    }

    public Connection getConnection() throws SQLException {
        return this.targetStatement.getConnection();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return this.targetStatement.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return this.targetStatement.getGeneratedKeys();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv() && SQLUtil.isWriteSql(sql)) {
            MetricService.getInstance().incWrite();
            ExecResult<Integer> result = ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTEUPDATE_STRING_INT, autoGeneratedKeys);
            return (Integer)result.getResult();
        } else {
            return this.targetStatement.executeUpdate(sql, autoGeneratedKeys);
        }
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv() && SQLUtil.isWriteSql(sql)) {
            MetricService.getInstance().incWrite();
            ExecResult<Integer> result = ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTEUPDATE_STRING_INTARRAY, columnIndexes);
            return (Integer)result.getResult();
        } else {
            return this.targetStatement.executeUpdate(sql, columnIndexes);
        }
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv() && SQLUtil.isWriteSql(sql)) {
            MetricService.getInstance().incWrite();
            ExecResult<Integer> result = ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTEUPDATE_STRING_STRINGARRAY, columnNames);
            return (Integer)result.getResult();
        } else {
            return this.targetStatement.executeUpdate(sql, columnNames);
        }
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            if (SQLUtil.isReadSql(sql)) {
                this.executeQuery(sql);
                return true;
            }

            if (SQLUtil.isWriteSql(sql)) {
                MetricService.getInstance().incWrite();
                ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTE_STRING_INT, autoGeneratedKeys);
                return false;
            }
        }

        return this.targetStatement.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            if (SQLUtil.isReadSql(sql)) {
                this.executeQuery(sql);
                return true;
            }

            if (SQLUtil.isWriteSql(sql)) {
                MetricService.getInstance().incWrite();
                ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTE_STRING_INTARRAY, columnIndexes);
                return false;
            }
        }

        return this.targetStatement.execute(sql, columnIndexes);
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        this.targetSql = sql;
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            if (SQLUtil.isReadSql(sql)) {
                this.executeQuery(sql);
                return true;
            }

            if (SQLUtil.isWriteSql(sql)) {
                MetricService.getInstance().incWrite();
                ExecuterFactory.execute(this, sql, SQLExecuteType.EXECUTE_STRING_STRINGARRAY, columnNames);
                return false;
            }
        }

        return this.targetStatement.execute(sql, columnNames);
    }

    public int getResultSetHoldability() throws SQLException {
        return this.targetStatement.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return this.targetStatement.isClosed();
    }

    public void setPoolable(boolean poolable) throws SQLException {
        this.targetStatement.setPoolable(poolable);
    }

    public boolean isPoolable() throws SQLException {
        return this.targetStatement.isPoolable();
    }

    public void closeOnCompletion() throws SQLException {
        throw new MQUnsupportException();
    }

    public boolean isCloseOnCompletion() throws SQLException {
        throw new MQUnsupportException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.targetStatement.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.targetStatement.isWrapperFor(iface);
    }

    public Statement getTargetStatement() {
        return this.targetStatement;
    }

    public AbstractMQTxDataSource getTxDataSource() {
        return this.dataSource;
    }

    public MQTxConnection getTxConnection() {
        return this.conn;
    }
}
