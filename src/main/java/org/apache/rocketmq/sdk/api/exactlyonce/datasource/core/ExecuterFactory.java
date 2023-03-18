package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.LocalTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.TransactionManager;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.LogUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.MetricsUtil;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ExecuterFactory {
    private static final InternalLogger LOGGER = ClientLoggerUtil.getClientLogger();

    public static <T> ExecResult<T> execute(AbstractMQTxStatement statement, String sql, SQLExecuteType type, Object args) throws SQLException {
        ExecResult<T> result;
        MQTxContext context = LocalTxContext.get();
        DataSourceConfig dsConfig = statement.getTxDataSource().getDataSourceConfig();
        context.setDataSourceConfig(dsConfig);
        TransactionManager.updateConsumeSessionMap(dsConfig, context.getConsumerGroup());
        boolean isAutoCommit = context.isAutoCommit() && statement.getConnection().getAutoCommit();
        MetricsUtil.recordPersistenceTimestamp(context);
        if (isAutoCommit) {
            result = executeWithAutoCommit(context, statement, sql, type, args);
        } else {
            result = executeWithoutAutoCommit(context, statement, sql, type, args);
        }
        MetricsUtil.recordAfterPersistenceTimestamp(context);
        return result;
    }

    private static <T> ExecResult<T> executeWithAutoCommit(MQTxContext context, AbstractMQTxStatement statement, String sql, SQLExecuteType type, Object args) throws SQLException {
        MQTxConnection txConn = null;
        try {
            try {
                txConn = statement.getTxConnection();
                txConn.setAutoCommit(false);
                ExecResult<T> result = executeSql(statement, sql, type, args);
                txConn.commit();
                context.setInTxEnv(false);
                txConn.setAutoCommit(true);
                return result;
            } catch (SQLException e1) {
                LogUtil.error(LOGGER, "executeWithAutoCommit fail, context:{}, SQLException:{}", context, e1.getMessage());
                txConn.rollback();
                throw e1;
            } catch (Throwable e2) {
                LogUtil.error(LOGGER, "executeWithAutoCommit fail, context:{}, Exception:{}", context, e2.getMessage());
                txConn.rollback();
                throw new SQLException(e2);
            }
        } catch (Throwable th) {
            context.setInTxEnv(false);
            txConn.setAutoCommit(true);
            throw th;
        }
    }

    private static <T> ExecResult<T> executeWithoutAutoCommit(MQTxContext context, AbstractMQTxStatement statement, String sql, SQLExecuteType type, Object args) throws SQLException {
        try {
            statement.getConnection().setAutoCommit(false);
            return executeSql(statement, sql, type, args);
        } catch (SQLException e1) {
            LogUtil.error(LOGGER, "executeWithoutAutoCommit fail, context:{}, SQLException:{}", context, e1.getMessage());
            throw e1;
        } catch (Throwable e2) {
            LogUtil.error(LOGGER, "executeWithoutAutoCommit fail, context:{}, Exception:{}", context, e2.getMessage());
            throw new SQLException(e2);
        }
    }

    public static ExecResult executeSql(AbstractMQTxStatement statement, String sql, SQLExecuteType type, Object args) throws SQLException {
        try {
            switch (type) {
                case EXECUTE_STRING:
                    return new ExecResult(Boolean.valueOf(statement.getTargetStatement().execute(sql)));
                case EXECUTE_STRING_INT:
                    return new ExecResult(Boolean.valueOf(statement.getTargetStatement().execute(sql, ((Integer) args).intValue())));
                case EXECUTE_STRING_INTARRAY:
                    return new ExecResult(Boolean.valueOf(statement.getTargetStatement().execute(sql, (int[]) args)));
                case EXECUTE_STRING_STRINGARRAY:
                    return new ExecResult(Boolean.valueOf(statement.getTargetStatement().execute(sql, (String[]) args)));
                case EXECUTEUPDATE_STRING:
                    return new ExecResult(Integer.valueOf(statement.getTargetStatement().executeUpdate(sql)));
                case EXECUTEUPDATE_STRING_INT:
                    return new ExecResult(Integer.valueOf(statement.getTargetStatement().executeUpdate(sql, ((Integer) args).intValue())));
                case EXECUTEUPDATE_STRING_INTARRAY:
                    return new ExecResult(Integer.valueOf(statement.getTargetStatement().executeUpdate(sql, (int[]) args)));
                case EXECUTEUPDATE_STRING_STRINGARRAY:
                    return new ExecResult(Integer.valueOf(statement.getTargetStatement().executeUpdate(sql, (String[]) args)));
                case EXECUTEBATCH_VOID:
                    return new ExecResult(statement.getTargetStatement().executeBatch());
                case PREPARED_EXECUTE_VOID:
                    return new ExecResult(Boolean.valueOf(((PreparedStatement) statement.getTargetStatement()).execute()));
                case PREPARED_EXECUTEUPDATE_VOID:
                    return new ExecResult(Integer.valueOf(((PreparedStatement) statement.getTargetStatement()).executeUpdate()));
                case PREPARED_EXECUTEBATCH_VOID:
                    return new ExecResult(((PreparedStatement) statement.getTargetStatement()).executeBatch());
                default:
                    LogUtil.error(LOGGER, "Invalid SQL type:{}", type.getFullName());
                    return null;
            }
        } catch (SQLException e1) {
            LogUtil.error(LOGGER, "Execute SQL fail, type:{}, sql:{}, err:{}", type.getFullName(), sql, e1.getMessage());
            throw e1;
        }
    }
}
