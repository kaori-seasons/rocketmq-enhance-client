package org.apache.rocketmq.sdk.test.exactlyonce;

import org.apache.rocketmq.sdk.api.Action;
import org.apache.rocketmq.sdk.api.ConsumeContext;
import org.apache.rocketmq.sdk.api.Message;
import org.apache.rocketmq.sdk.api.MessageListener;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.MQDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class SimpleListener implements MessageListener {
    private MQDataSource dataSource = new MQDataSource("jdbc:mysql://xxx.xxx.xxx.xxx::3306/exactlyonce?useUnicode=true&characterEncoding=UTF-8", "test", "Tuxedo12#$", "com.mysql.jdbc.Driver");

    @Override
    public Action consume(Message message, ConsumeContext context) {
        Connection connection = null;
        PreparedStatement statement = null;

        Action failed;
        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement("INSERT INTO app(msg, ctime) VALUES(?, ?)");
            statement.setString(1, new String(message.getBody()));
            statement.setLong(2, System.currentTimeMillis());
            statement.execute();
            System.out.println("consume message success， msgId is " + message.getMsgID());
            Action reconsumeLater = Action.ReconsumeLater;
            return reconsumeLater;
        } catch (Throwable var20) {
            System.out.println("consume message fail:" + var20.getMessage());
            failed = Action.ReconsumeLater;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception var19) {
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception var18) {
                }
            }

        }

        return failed;
    }

}
