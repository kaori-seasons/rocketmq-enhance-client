package org.apache.rocketmq.sdk.api.exactlyonce.manager.impl;

import org.apache.rocketmq.sdk.api.PropertyKeyConst;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxRecord;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.DataSourceConfig;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.core.MQTxConnection;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.TransactionManager;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.database.LoadRecordDo;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.DBAccessUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.LogUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.OffsetUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.TxContextUtil;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.message.MessageQueue;
import org.apache.rocketmq.sdk.shade.common.utils.ThreadUtils;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import io.netty.util.internal.ConcurrentSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

public class TxRecordManagerImpl {
    private static final InternalLogger LOGGER = ClientLoggerUtil.getClientLogger();
    private ScheduledThreadPoolExecutor txRecordCheckExecutor = new ScheduledThreadPoolExecutor(2, ThreadUtils.newGenericThreadFactory("txRecordCheckExecutor", false));
    private int refreshInterval;
    private static final String LOG_SPLITOR = ",";
    private static final int TIME_EXPIRED = 14400;
    private static final int SECOND_OF_HOUR = 3600;
    private static final int SECOND_OF_DAY = 86400;
    private static final int MAX_UNACTIVE_PROCESS_TIME = 1200000;
    private static final int MAX_SCANACKED_PROCESS_TIME = 9000;
    private static final int MAX_DELETEACKED_PROCESS_TIME = 9000;

    public TxRecordManagerImpl(Properties properties) {
        this.refreshInterval = 10000;
        String refreshIntervalStr = properties.getProperty(PropertyKeyConst.EXACTLYONCE_RM_REFRESHINTERVAL);
        if (!UtilAll.isBlank(refreshIntervalStr)) {
            try {
                this.refreshInterval = Integer.parseInt(refreshIntervalStr);
            } catch (NumberFormatException e) {
            }
        }
    }

    public void start() {
        this.txRecordCheckExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    long before = System.currentTimeMillis();
                    TxRecordManagerImpl.this.refreshExpiredTxRecord();
                    LogUtil.info(TxRecordManagerImpl.LOGGER, "scan expired record use:{} ms", Long.valueOf(System.currentTimeMillis() - before));
                } catch (Throwable e) {
                    LogUtil.error(TxRecordManagerImpl.LOGGER, "scan active record fail, err:{}", e.getMessage());
                }
            }
        }, 0, (long) this.refreshInterval, TimeUnit.MILLISECONDS);
        this.txRecordCheckExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    long before = System.currentTimeMillis();
                    TxRecordManagerImpl.this.deleteUnActiveConsumeRecord();
                    LogUtil.info(TxRecordManagerImpl.LOGGER, "delete unactive record use:{} ms", Long.valueOf(System.currentTimeMillis() - before));
                } catch (Throwable e) {
                    LogUtil.error(TxRecordManagerImpl.LOGGER, "delete unactive record fail, err:{}", e.getMessage());
                }
            }
        }, getInitialDelay(), 86400, TimeUnit.SECONDS);
        LogUtil.info(LOGGER, "start TxRecordManager...");
    }

    public void stop() {
        ThreadUtils.shutdownGracefully(this.txRecordCheckExecutor, 10000, TimeUnit.MILLISECONDS);
    }

    private long getInitialDelay() {
        long initDelay;
        long pastSecond = DateUtils.getFragmentInSeconds(Calendar.getInstance(), 5);
        if (pastSecond < 14400) {
            initDelay = (14400 - pastSecond) + ((long) RandomUtils.nextInt(0, SECOND_OF_HOUR));
        } else {
            initDelay = (86400 - pastSecond) + 14400 + ((long) RandomUtils.nextInt(0, SECOND_OF_HOUR));
        }
        return initDelay;
    }

    public void refreshExpiredTxRecord() {
        long begin = System.currentTimeMillis();
        ConcurrentMap<DataSourceConfig, ConcurrentSet<String>> consumeSessionMap = TransactionManager.getConsumerSessionMap();
        LogUtil.info(LOGGER, "consume session map:{}", StringUtils.join(consumeSessionMap, ","));
        List<DataSourceConfig> dataSourceConfigs = new ArrayList<>(consumeSessionMap.keySet());
        Collections.shuffle(dataSourceConfigs);
        for (DataSourceConfig config : dataSourceConfigs) {
            ConcurrentSet<String> cidSet = consumeSessionMap.get(config);
            if (cidSet != null && !cidSet.isEmpty()) {
                List<String> cidList = new ArrayList<>(cidSet);
                Collections.shuffle(cidList);
                for (String cid : cidList) {
                    if (System.currentTimeMillis() - begin <= 9000) {
                        try {
                            List<MessageQueue> currentQueue = OffsetUtil.getCurrentConsumeQueue(cid);
                            if (currentQueue != null && !currentQueue.isEmpty()) {
                                deleteExpiredRecord(config, currentQueue, cid);
                            }
                        } catch (Exception e) {
                            LogUtil.error(LOGGER, "refresh expired record fail, consumerGroup:{}, err:{}", cid, e.getMessage());
                        }
                    } else {
                        return;
                    }
                }
                continue;
            }
        }
    }

    public void deleteUnActiveConsumeRecord() {
        long begin = System.currentTimeMillis();
        long expiredTime = begin - 259200000;
        List<DataSourceConfig> dataSourceConfigs = new ArrayList<>(TransactionManager.getConsumerSessionMap().keySet());
        Collections.shuffle(dataSourceConfigs);
        for (DataSourceConfig config : dataSourceConfigs) {
            while (System.currentTimeMillis() - begin <= 1200000) {
                List<Long> recordList = DBAccessUtil.queryExpiredRecord(config, Long.valueOf(expiredTime), 200);
                if (recordList != null && !recordList.isEmpty()) {
                    try {
                        DBAccessUtil.deleteRecordById(config, recordList);
                    } catch (Exception e) {
                        LogUtil.error(LOGGER, "delete expire record fail, dataSource:{}, count:{}, err:{}", config, Integer.valueOf(recordList.size()), e.getMessage());
                    }
                }
            }
            return;
        }
    }

    private void deleteExpiredRecord(DataSourceConfig config, List<MessageQueue> mqList, String consumerGroup) {
        long begin = System.currentTimeMillis();
        Collections.shuffle(mqList);
        LogUtil.info(LOGGER, "start delete expired, config:{}, consumerGroup:{}, mqList:{}", config, consumerGroup, StringUtils.join(mqList));
        for (MessageQueue mq : mqList) {
            Long offset = OffsetUtil.getMQSafeOffset(mq, consumerGroup);
            if (offset != null) {
                LoadRecordDo loadRecordDo = new LoadRecordDo(mq, consumerGroup, offset, Long.valueOf(System.currentTimeMillis() - 180000), 200);
                while (System.currentTimeMillis() - begin <= 9000) {
                    List<Long> txRecordList = DBAccessUtil.queryAckedRecord(config, loadRecordDo);
                    if (txRecordList != null && !txRecordList.isEmpty()) {
                        try {
                            DBAccessUtil.deleteRecordById(config, txRecordList);
                        } catch (Exception e) {
                            LogUtil.error(LOGGER, "delete record directly fail, config:{}, id:{}, err:{}", config, StringUtils.join(txRecordList, ","), e.getMessage());
                        }
                        if (txRecordList.size() < 50) {
                            break;
                        }
                    }
                }
                return;
            }
        }
    }

    public void flushTxRecord(MQTxConnection connection, MQTxContext context) throws Exception {
        if (connection == null) {
            throw new Exception("Connection is null");
        }
        MQTxRecord record = TxContextUtil.buildTxRecord(context);
        DataSourceConfig config = connection.getTxDateSource().getDataSourceConfig();
        record.setDataSourceConfig(config);
        DBAccessUtil.insertTxRecord(connection.getTargetConnection(), config, record);
    }
}
