package org.apache.rocketmq.sdk.api.exactlyonce.manager.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxRecord;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.MetricService;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.database.AbstractDBAccessor;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.database.LoadRecordDo;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.database.MysqlAccessor;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

public class DBAccessUtil {
    public static final InternalLogger LOGGER = ClientLoggerUtil.getClientLogger();
    private static final ConcurrentHashMap<String, DataSource> dataSourcePool = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SQLExceptionTranslator> translatorMap = new ConcurrentHashMap<>();
    private static final int INNER_DATASOURCE_MAX_ACTIVE = 2;
    private static final int INNER_DATASOURCE_INIT_SIZE = 1;
    private static final int INNER_EVICT_CONNECTION_MILLIS = 30000;

    private static AbstractDBAccessor getDBAccessor(DataSourceConfig config) throws Exception {
        if (config == null || StringUtils.isEmpty(config.getDriver())) {
            throw new Exception("datasource driver invalid " + config);
        }
        switch (DBType.parseTypeFromDriver(config.getDriver())) {
            case MYSQL:
                return MysqlAccessor.getInstance();
            case SQLSERVER:
            case ORACLE:
            case DB2:
            default:
                throw new Exception("unsupported db type" + config.getDriver());
        }
    }

    private static Connection getInternalConnection(String url, String user, String passwd, String driver) throws SQLException {
        String uniqKey = new StringBuilder(256).append(url).append((char) 1).append(user).append((char) 1).append(passwd).append((char) 1).append(driver).toString();
        DataSource dataSource = dataSourcePool.get(uniqKey);
        if (dataSource == null) {
            try {
                Map<String, String> property = new HashMap<>();
                property.put(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, driver);
                property.put("url", url);
                property.put(DruidDataSourceFactory.PROP_USERNAME, user);
                property.put(DruidDataSourceFactory.PROP_PASSWORD, passwd);
                property.put(DruidDataSourceFactory.PROP_MAXACTIVE, String.valueOf(2));
                property.put(DruidDataSourceFactory.PROP_INITIALSIZE, String.valueOf(1));
                property.put(DruidDataSourceFactory.PROP_MINEVICTABLEIDLETIMEMILLIS, String.valueOf(30000));
                dataSource = DruidDataSourceFactory.createDataSource(property);
                ((DruidDataSource) dataSource).setQueryTimeout(1);
                DataSource dataSourceOld = dataSourcePool.putIfAbsent(uniqKey, dataSource);
                if (dataSourceOld != null) {
                    ((DruidDataSource) dataSource).close();
                    dataSource = dataSourceOld;
                }
            } catch (Exception e) {
                LogUtil.error(LOGGER, "getInternalConnection fail, uniqKey:{}, err:{}", uniqKey, e.getMessage());
                return null;
            }
        }
        return dataSource.getConnection();
    }

    public static List<Long> queryAckedRecord(DataSourceConfig config, LoadRecordDo loadRecordDo) {
        List<Long> recordList = null;
        try {
            AbstractDBAccessor accessor = getDBAccessor(config);
            Connection connection = getInternalConnection(config.getUrl(), config.getUser(), config.getPasswd(), config.getDriver());
            if (!(accessor == null || connection == null)) {
                long begin = System.currentTimeMillis();
                recordList = accessor.queryAckedRecord(connection, loadRecordDo);
                MetricService.getInstance().incQueryAcked(begin);
            }
        } catch (Exception e) {
            LogUtil.error(LOGGER, "query acked record fail, loadRecordDo:{}, err:{}", loadRecordDo, e.getMessage());
        }
        return recordList;
    }

    public static List<Long> queryExpiredRecord(DataSourceConfig config, Long timestamp, int count) {
        List<Long> recordList = null;
        try {
            AbstractDBAccessor accessor = getDBAccessor(config);
            Connection connection = getInternalConnection(config.getUrl(), config.getUser(), config.getPasswd(), config.getDriver());
            if ((accessor != null) && (connection != null)) {
                long begin = System.currentTimeMillis();
                recordList = accessor.queryExpiredRecord(connection, timestamp, count);
                MetricService.getInstance().incQueryExpired(begin);
            }
        } catch (Exception e) {
            LogUtil.error(LOGGER, "query acked record fail, timestamp:{}, count:{}, err:{}", timestamp, Integer.valueOf(count), e.getMessage());
        }
        return recordList;
    }

    public static void insertTxRecord(Connection connection, DataSourceConfig config, MQTxRecord record) throws Exception {
        AbstractDBAccessor accessor = getDBAccessor(config);
        if (accessor == null || connection == null) {
            throw new Exception("access db fail, config:" + config);
        }
        long begin = System.currentTimeMillis();
        accessor.insertRecord(connection, record, false);
        MetricService.getInstance().incInsertRecord(begin);
    }

    public static boolean isRecordExist(MQTxContext context) {
        DataSourceConfig config = context.getDataSourceConfig();
        String messageId = context.getMessageId();
        try {
            AbstractDBAccessor accessor = getDBAccessor(config);
            Connection connection = getInternalConnection(config.getUrl(), config.getUser(), config.getPasswd(), config.getDriver());
            if (accessor == null || connection == null) {
                return false;
            }
            long begin = System.currentTimeMillis();
            Long id = accessor.queryRecordCountByMsgId(connection, config, messageId);
            MetricService.getInstance().incQueryMsgIdCount(begin);
            if (id != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            LogUtil.error(LOGGER, "query isRecordExist fail, msgId:{}, err:{}", context.getMessageId(), e.getMessage());
            return false;
        }
    }

    public static void deleteRecordById(DataSourceConfig config, List<Long> msgIds) throws Exception {
        AbstractDBAccessor accessor = getDBAccessor(config);
        Connection connection = getInternalConnection(config.getUrl(), config.getUser(), config.getPasswd(), config.getDriver());
        if (accessor == null || connection == null) {
            throw new Exception("access db fail, config:" + config);
        }
        long begin = System.currentTimeMillis();
        accessor.deleteRecordById(connection, msgIds);
        MetricService.getInstance().incDeleteRecord(begin);
    }

    public static boolean isRecordDupException(MQTxContext context, Exception e) {
        if (!(e instanceof SQLException)) {
            return false;
        }
        boolean isDup = isDuplicateKeyException(context.getDataSourceConfig().getProductName(), (SQLException) e);
        if (isDup) {
            LogUtil.info(LOGGER, "exception is cased by record duped, context:{}, err:{}", context, e.getMessage());
        }
        return isDup;
    }

    private static boolean isDuplicateKeyException(String productName, SQLException e) {
        SQLExceptionTranslator sqlExceptionTranslator = translatorMap.get(productName);
        if (sqlExceptionTranslator == null) {
            sqlExceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(productName);
            SQLExceptionTranslator oldTranslator = translatorMap.putIfAbsent(productName, sqlExceptionTranslator);
            if (oldTranslator != null) {
                sqlExceptionTranslator = oldTranslator;
            }
        }
        return sqlExceptionTranslator.translate("", "", e) instanceof DuplicateKeyException;
    }

    public enum DBType {
        MYSQL(JdbcConstants.MYSQL, "com.mysql.jdbc.Driver"),
        SQLSERVER(JdbcConstants.SQL_SERVER, JdbcConstants.SQL_SERVER_DRIVER_SQLJDBC4),
        ORACLE(JdbcConstants.ORACLE, JdbcConstants.ORACLE_DRIVER2),
        DB2(JdbcConstants.DB2, "COM.ibm.db2.jdbc.app.DB2Driver");
        
        private String dbType;
        private String driver;

        DBType(String dbType, String driver) {
            this.driver = driver;
        }

        public static DBType parseTypeFromDriver(String driver) {
            if (StringUtils.isEmpty(driver)) {
                return null;
            }
            DBType[] values = values();
            for (DBType type : values) {
                if (type.driver.equals(driver)) {
                    return type;
                }
            }
            return null;
        }
    }
}
