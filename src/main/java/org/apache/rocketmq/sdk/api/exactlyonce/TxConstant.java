package org.apache.rocketmq.sdk.api.exactlyonce;

public class TxConstant {
    public static final int DEFAULT_RECORD_RETENTION_SECOND = 180;
    public static final int MAX_RECORD_RETENTION_MILLISECOND = 259200000;
    public static final int DEFAULT_RECORD_MANAGER_CLEAN_INTERVAL_MILLISECOND = 10000;
    public static final int DEFAULT_CLEAN_UNACTIVE_INTERVAL_SECOND = 86400;
    public static final int DEFAULT_CLEAN_RECORD_QUEUE_SIZE = 10;
    public static final int MIN_EXPIRED_COUNT_FOR_QUEUE = 50;
    public static final int EXPIRED_TXRECORD_LOAD_STEP = 200;
    public static final int TRANSACTION_TIMEOUT_LOWER_BOUND = 0;
    public static final int TRANSACTION_TIMEOUT_UPPER_BOUND = 600000;
    public static final int CONSUME_STATUS_STATISTICS_INTERVAL_SECOND = 60;
    public static final int QUERY_TIMEOUT_SECOND = 1;
    public static final String COMMITQPS_KEY = "commitQps";
    public static final String ROLLBACKQPS_KEY = "rollbackQps";
    public static final String AVERAGEPROCESSTIME_KEY = "averageProcessTime";
    public static final String AVERAGEPERSISTENCETIME_KEY = "averagePersistenceTime";
    public static final String TOTALFORBIDDUPLICATION_KEY = "totalForbidDuplication";
    public static final String AVERAGECONSUMETIME_KEY = "averageConsumeTime";
    public static final String DB_QUERYEXPIRED_KEY = "dbQueryExpired";
    public static final String DB_QUERYACKED_KEY = "dbQueryAcked";
    public static final String DB_QUERYMSGIDCOUNT_KEY = "dbQueryMsgIdCount";
    public static final String DB_INSERTRECORD_KEY = "dbInsertRecord";
    public static final String DB_DELETERECORD_KEY = "dbDeleteRecord";
    public static final String DB_READ_KEY = "dbRead";
    public static final String DB_WRITE_KEY = "dbWrite";
    public static final String DB_QUERYEXPIRED_RT_KEY = "dbQueryExpiredRt";
    public static final String DB_QUERYACKED_RT_KEY = "dbQueryAckedRt";
    public static final String DB_QUERYMSGIDCOUNT_RT_KEY = "dbQueryMsgIdCountRt";
    public static final String DB_INSERTRECORD_RT_KEY = "dbInsertRecordRt";
    public static final String DB_DELETERECORD_RT_KEY = "dbDeleteRecordRt";
    public static final String INTERNAL_MSGID_SEPARATOR = "#";
    public static final char DATASOURCE_KEY_SPLITOR = 1;
    public static final String EXACTLYONCELOG_PREFIX = "ExactlyOnce ";
}
