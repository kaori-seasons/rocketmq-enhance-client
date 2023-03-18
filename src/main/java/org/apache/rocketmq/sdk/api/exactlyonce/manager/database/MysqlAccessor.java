package org.apache.rocketmq.sdk.api.exactlyonce.manager.database;

import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class MysqlAccessor extends AbstractDBAccessor {
    private MysqlAccessor() {
    }

    @Override
    protected PreparedStatement queryAckedRecordStatement(Connection connection, LoadRecordDo loadRecordDo) throws Exception {
        MessageQueue mq = loadRecordDo.getMessageQueue();
        PreparedStatement statement = connection.prepareStatement(queryAckedRecordWithoutCheckIdSql());
        statement.setString(1, loadRecordDo.getConsumerGroup());
        statement.setString(2, mq.getTopic());
        statement.setString(3, mq.getBrokerName());
        statement.setInt(4, mq.getQueueId());
        statement.setLong(5, loadRecordDo.getOffset().longValue());
        statement.setLong(6, loadRecordDo.getTimestamp().longValue());
        statement.setInt(7, loadRecordDo.getCount());
        return statement;
    }

    @Override 
    protected PreparedStatement queryExpiredRecordStatement(Connection connection, Long timestamp, int count) throws Exception {
        PreparedStatement statement = connection.prepareStatement(queryExpirededRecordSql());
        statement.setLong(1, timestamp.longValue());
        statement.setInt(2, count);
        return statement;
    }

    @Override 
    protected String queryExpirededRecordSql() {
        return "SELECT id FROM transaction_record WHERE ctime<? ORDER BY ctime ASC LIMIT ?";
    }

    @Override 
    protected String queryAckedRecordSql() {
        return "SELECT id FROM transaction_record WHERE consumer_group=? AND topic_name=? AND broker_name=? AND queue_id=? AND offset<? AND ctime<? AND id>? ORDER BY id ASC LIMIT ?";
    }

    @Override 
    protected String queryAckedRecordWithoutCheckIdSql() {
        return "SELECT id FROM transaction_record WHERE consumer_group=? AND topic_name=? AND broker_name=? AND queue_id=? AND offset<? AND ctime<? LIMIT ?";
    }

    @Override 
    protected String queryRecordCountByMsgIdSql() {
        return "SELECT id FROM transaction_record WHERE message_id=?";
    }

    @Override 
    protected String insertRecordSql() {
        return "INSERT INTO transaction_record (topic_name, consumer_group, broker_name, queue_id, offset, message_id, ctime) VALUES(?,?,?,?,?,?,?)";
    }

    @Override 
    protected String deleteRecordByMsgIdSql() {
        return "DELETE FROM transaction_record WHERE id in (%s)";
    }

    public static AbstractDBAccessor getInstance() {
        return MySqlAccessorHolder.INSTANCE;
    }

    public static class MySqlAccessorHolder {
        private static final AbstractDBAccessor INSTANCE = new MysqlAccessor();

        private MySqlAccessorHolder() {
        }
    }
}
