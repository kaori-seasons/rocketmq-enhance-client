package org.apache.rocketmq.sdk.api.exactlyonce.manager.database;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxRecord;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;

public abstract class AbstractDBAccessor {
    protected abstract PreparedStatement queryAckedRecordStatement(Connection connection, LoadRecordDo loadRecordDo) throws Exception;

    protected abstract PreparedStatement queryExpiredRecordStatement(Connection connection, Long l, int i) throws Exception;

    protected abstract String queryExpirededRecordSql();

    protected abstract String queryAckedRecordSql();

    protected abstract String queryAckedRecordWithoutCheckIdSql();

    protected abstract String queryRecordCountByMsgIdSql();

    protected abstract String insertRecordSql();

    protected abstract String deleteRecordByMsgIdSql();

    public List<Long> queryAckedRecord(Connection connection, LoadRecordDo loadRecordDo) throws Exception {
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            try {
                statement = queryAckedRecordStatement(connection, loadRecordDo);
                resultSet = statement.executeQuery();
                List<Long> recordList = new ArrayList<>();
                while (resultSet.next()) {
                    recordList.add(Long.valueOf(resultSet.getLong("id")));
                }
                return recordList;
            } catch (Exception e) {
                throw e;
            }
        } finally {
            closeResource(resultSet, statement, connection);
        }
    }

    public List<Long> queryExpiredRecord(Connection connection, Long timestamp, int count) throws Exception {
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        try {
            try {
                statement = queryExpiredRecordStatement(connection, timestamp, count);
                resultSet = statement.executeQuery();
                List<Long> recordList = new ArrayList<>();
                while (resultSet.next()) {
                    recordList.add(Long.valueOf(resultSet.getLong("id")));
                }
                return recordList;
            } catch (Exception e) {
                throw e;
            }
        } finally {
            closeResource(resultSet, statement, connection);
        }
    }

    public Long queryRecordCountByMsgId(Connection connection, DataSourceConfig config, String messageId) throws Exception {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            try {
                statement = connection.prepareStatement(queryRecordCountByMsgIdSql());
                statement.setString(1, messageId);
                resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return null;
                }
                Long valueOf = Long.valueOf(resultSet.getLong(1));
                closeResource(resultSet, statement, connection);
                return valueOf;
            } catch (Exception e) {
                throw e;
            }
        } finally {
            closeResource(resultSet, statement, connection);
        }
    }

    public void insertRecord(Connection connection, MQTxRecord record, boolean needCloseConn) throws Exception {
        PreparedStatement statement = null;
        try {
            try {
                statement = connection.prepareStatement(insertRecordSql());
                statement.setString(1, record.getTopicName());
                statement.setString(2, record.getConsumerGroup());
                statement.setString(3, record.getBrokerName());
                statement.setInt(4, record.getQid());
                statement.setLong(5, record.getOffset());
                statement.setString(6, record.getMessageId());
                statement.setLong(7, record.getCreateTime().longValue());
                statement.executeUpdate();
                closeResource(null, statement, connection, needCloseConn);
            } catch (Exception e) {
                throw e;
            }
        } catch (Throwable th) {
            closeResource(null, statement, connection, needCloseConn);
            throw th;
        }
    }

    public void deleteRecordById(Connection connection, List<Long> ids) throws Exception {
        try {
            try {
                Statement statement = connection.createStatement();
                if (statement == null) {
                    throw new Exception("create deleteRecordById statement fail");
                }
                statement.execute(String.format(deleteRecordByMsgIdSql(), StringUtils.join(ids, StringArrayPropertyEditor.DEFAULT_SEPARATOR)));
                closeResource(null, statement, connection);
            } catch (Exception e) {
                throw e;
            }
        } catch (Throwable th) {
            closeResource(null, null, connection);
            throw th;
        }
    }

    private void closeResource(ResultSet resultSet, Statement statement, Connection connection) {
        closeResource(resultSet, statement, connection, true);
    }

    private void closeResource(ResultSet resultSet, Statement statement, Connection connection, boolean needCloseConn) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception e2) {
            }
        }
        if (needCloseConn && connection != null) {
            try {
                connection.close();
            } catch (Exception e3) {
            }
        }
    }
}
