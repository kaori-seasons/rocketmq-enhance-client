package org.apache.rocketmq.sdk.api.exactlyonce.manager.impl;

import org.apache.rocketmq.sdk.api.exactlyonce.TxConstant;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.LogUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.MetricsUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.util.TxContextUtil;
import org.apache.rocketmq.sdk.api.impl.util.ClientLoggerUtil;
import org.apache.rocketmq.sdk.shade.common.utils.ThreadUtils;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MetricServiceImpl {
    private static final InternalLogger LOGGER = ClientLoggerUtil.getClientLogger();
    private volatile int commitQps;
    private volatile int rollbackQps;
    private volatile long averageProcessTime;
    private volatile long averagePersistenceTime;
    private volatile long averageConsumeTime;
    private volatile int queryAckedPre;
    private volatile int queryExpiredPre;
    private volatile int queryMsgIdCountPre;
    private volatile int insertRecordPre;
    private volatile int deleteRecordPre;
    private volatile int dbReadPre;
    private volatile int dbWritePre;
    private volatile long queryAckedRt;
    private volatile long queryExpiredRt;
    private volatile long queryMsgIdCountRt;
    private volatile long insertRecordRt;
    private volatile long deleteRecordRt;
    private volatile AtomicInteger commitTotal = new AtomicInteger(0);
    private volatile AtomicInteger rollbackTotal = new AtomicInteger(0);
    private volatile AtomicLong processTimeTotal = new AtomicLong(0);
    private volatile AtomicLong persistenceTimeTotal = new AtomicLong(0);
    private volatile AtomicLong consumeTimeTotal = new AtomicLong(0);
    private volatile AtomicLong forbidDupTimes = new AtomicLong(0);
    private volatile AtomicLong queryAckedTimeUsed = new AtomicLong(0);
    private volatile AtomicLong queryExpiredTimeUsed = new AtomicLong(0);
    private volatile AtomicLong queryMsgIdCountTimeUsed = new AtomicLong(0);
    private volatile AtomicLong insertRecordTimeUsed = new AtomicLong(0);
    private volatile AtomicLong deleteRecordTimeUsed = new AtomicLong(0);
    private volatile AtomicInteger queryAckedTimes = new AtomicInteger(0);
    private volatile AtomicInteger queryExpiredTimes = new AtomicInteger(0);
    private volatile AtomicInteger queryMsgIdCountTimes = new AtomicInteger(0);
    private volatile AtomicInteger insertRecordTimes = new AtomicInteger(0);
    private volatile AtomicInteger deleteRecordTimes = new AtomicInteger(0);
    private volatile AtomicInteger dbRead = new AtomicInteger(0);
    private volatile AtomicInteger dbWrite = new AtomicInteger(0);
    private ScheduledThreadPoolExecutor refreshExecuter = new ScheduledThreadPoolExecutor(1, ThreadUtils.newThreadFactory("ExactlyOnceConsumer", false));

    public void record(MQTxContext context) {
        if (context != null) {
            record(context.getMessageId(), MetricsUtil.getProcessTime(context), MetricsUtil.getPersistenceTime(context), MetricsUtil.getConsumeTime(context), System.currentTimeMillis(), TxContextUtil.isTxContextCommitted(context), context.isDup());
        }
    }

    public void record(String messageId, long processTime, long persistenceTime, long consumeTime, long timestamp, boolean isCommit, boolean isDup) {
        this.processTimeTotal.addAndGet(processTime);
        this.persistenceTimeTotal.addAndGet(persistenceTime);
        this.consumeTimeTotal.addAndGet(consumeTime);
        if (isDup) {
            this.forbidDupTimes.incrementAndGet();
        }
        if (isCommit) {
            this.commitTotal.incrementAndGet();
        } else {
            this.rollbackTotal.incrementAndGet();
        }
        LogUtil.debug(LOGGER, "record consume status, msgId:{}, processTime:{}, persistenceTime:{}, consumeTime:{} recordTimestamp:{}, commit:{}, dup:{}", messageId, Long.valueOf(processTime), Long.valueOf(persistenceTime), Long.valueOf(consumeTime), Long.valueOf(timestamp), Boolean.valueOf(isCommit), Boolean.valueOf(isDup));
    }

    public void computeTxStatsData() {
        try {
            int consumeCount = this.commitTotal.get() + this.rollbackTotal.get();
            this.commitQps = this.commitTotal.getAndSet(0) / 60;
            this.rollbackQps = this.rollbackTotal.getAndSet(0) / 60;
            this.averageProcessTime = consumeCount == 0 ? 0 : this.processTimeTotal.getAndSet(0) / ((long) consumeCount);
            this.averagePersistenceTime = consumeCount == 0 ? 0 : this.persistenceTimeTotal.getAndSet(0) / ((long) consumeCount);
            this.averageConsumeTime = consumeCount == 0 ? 0 : this.consumeTimeTotal.getAndSet(0) / ((long) consumeCount);
            this.queryAckedPre = this.queryAckedTimes.getAndSet(0);
            this.queryExpiredPre = this.queryExpiredTimes.getAndSet(0);
            this.queryMsgIdCountPre = this.queryMsgIdCountTimes.getAndSet(0);
            this.insertRecordPre = this.insertRecordTimes.getAndSet(0);
            this.deleteRecordPre = this.deleteRecordTimes.getAndSet(0);
            this.dbReadPre = this.dbRead.getAndSet(0);
            this.dbWritePre = this.dbWrite.getAndSet(0);
            this.queryAckedRt = this.queryAckedPre == 0 ? 0 : this.queryAckedTimeUsed.getAndSet(0) / ((long) this.queryAckedPre);
            this.queryExpiredRt = this.queryExpiredPre == 0 ? 0 : this.queryExpiredTimeUsed.getAndSet(0) / ((long) this.queryExpiredPre);
            this.queryMsgIdCountRt = this.queryMsgIdCountPre == 0 ? 0 : this.queryMsgIdCountTimeUsed.getAndSet(0) / ((long) this.queryMsgIdCountPre);
            this.insertRecordRt = this.insertRecordPre == 0 ? 0 : this.insertRecordTimeUsed.getAndSet(0) / ((long) this.insertRecordPre);
            this.deleteRecordRt = this.deleteRecordPre == 0 ? 0 : this.deleteRecordTimeUsed.getAndSet(0) / ((long) this.deleteRecordPre);
            LogUtil.info(LOGGER, "Metrics forbidDupCount:{}, commitQps:{}, rollbackQps:{}, averageProcessTime:{}ms, averagePersistenceTime:{}ms, averageConsumeTime:{}ms queryAcked:{}/min, queryExpired:{}/min, queryMsgCount:{}/min, insertRecord:{}/min, deleteRecord:{}/min, dbRead:{}/min, dbWrite:{}/min queryAckedRt:{}ms, queryExpiredRt:{}ms, queryMsgCountRt:{}ms, insertRecordRt:{}ms, deleteRecordRt:{}ms", this.forbidDupTimes, Integer.valueOf(this.commitQps), Integer.valueOf(this.rollbackQps), Long.valueOf(this.averageProcessTime), Long.valueOf(this.averagePersistenceTime), Long.valueOf(this.averageConsumeTime), Integer.valueOf(this.queryAckedPre), Integer.valueOf(this.queryExpiredPre), Integer.valueOf(this.queryMsgIdCountPre), Integer.valueOf(this.insertRecordPre), Integer.valueOf(this.deleteRecordPre), Integer.valueOf(this.dbReadPre), Integer.valueOf(this.dbWritePre), Long.valueOf(this.queryAckedRt), Long.valueOf(this.queryExpiredRt), Long.valueOf(this.queryMsgIdCountRt), Long.valueOf(this.insertRecordRt), Long.valueOf(this.deleteRecordRt));
        } catch (Throwable e) {
            LogUtil.error(LOGGER, "computeTxStats fail:{}", e.getMessage());
        }
    }

    public Map<String, String> getCurrentConsumeStatus() {
        Map<String, String> statusMap = new HashMap<>(13);
        statusMap.put(TxConstant.COMMITQPS_KEY, String.valueOf(this.commitQps));
        statusMap.put(TxConstant.ROLLBACKQPS_KEY, String.valueOf(this.rollbackQps));
        statusMap.put(TxConstant.AVERAGECONSUMETIME_KEY, String.valueOf(this.averageConsumeTime));
        statusMap.put(TxConstant.AVERAGEPROCESSTIME_KEY, String.valueOf(this.averageProcessTime));
        statusMap.put(TxConstant.AVERAGEPERSISTENCETIME_KEY, String.valueOf(this.averagePersistenceTime));
        statusMap.put(TxConstant.TOTALFORBIDDUPLICATION_KEY, String.valueOf(this.forbidDupTimes.get()));
        statusMap.put(TxConstant.DB_QUERYACKED_KEY, String.valueOf(this.queryAckedPre));
        statusMap.put(TxConstant.DB_QUERYEXPIRED_KEY, String.valueOf(this.queryExpiredPre));
        statusMap.put(TxConstant.DB_QUERYMSGIDCOUNT_KEY, String.valueOf(this.queryMsgIdCountPre));
        statusMap.put(TxConstant.DB_INSERTRECORD_KEY, String.valueOf(this.insertRecordPre));
        statusMap.put(TxConstant.DB_DELETERECORD_KEY, String.valueOf(this.deleteRecordPre));
        statusMap.put(TxConstant.DB_READ_KEY, String.valueOf(this.dbReadPre));
        statusMap.put(TxConstant.DB_WRITE_KEY, String.valueOf(this.dbWrite));
        statusMap.put(TxConstant.DB_QUERYACKED_RT_KEY, String.valueOf(this.queryAckedRt));
        statusMap.put(TxConstant.DB_QUERYEXPIRED_RT_KEY, String.valueOf(this.queryExpiredRt));
        statusMap.put(TxConstant.DB_QUERYMSGIDCOUNT_RT_KEY, String.valueOf(this.queryMsgIdCountRt));
        statusMap.put(TxConstant.DB_INSERTRECORD_RT_KEY, String.valueOf(this.insertRecordRt));
        statusMap.put(TxConstant.DB_DELETERECORD_RT_KEY, String.valueOf(this.deleteRecordRt));
        return statusMap;
    }

    public void incQueryExpired(long begin) {
        this.queryExpiredTimes.incrementAndGet();
        this.dbRead.incrementAndGet();
        this.queryExpiredTimeUsed.addAndGet(System.currentTimeMillis() - begin);
    }

    public void incQueryAcked(long begin) {
        this.queryAckedTimes.incrementAndGet();
        this.dbRead.incrementAndGet();
        this.queryAckedTimeUsed.addAndGet(System.currentTimeMillis() - begin);
    }

    public void incQueryMsgIdCount(long begin) {
        this.queryMsgIdCountTimes.incrementAndGet();
        this.dbRead.incrementAndGet();
        this.queryMsgIdCountTimeUsed.addAndGet(System.currentTimeMillis() - begin);
    }

    public void incInsertRecord(long begin) {
        this.insertRecordTimes.incrementAndGet();
        this.dbWrite.incrementAndGet();
        this.insertRecordTimeUsed.addAndGet(System.currentTimeMillis() - begin);
    }

    public void incDeleteRecord(long begin) {
        this.deleteRecordTimes.incrementAndGet();
        this.dbWrite.incrementAndGet();
        this.deleteRecordTimeUsed.addAndGet(System.currentTimeMillis() - begin);
    }

    public void incRead() {
        this.dbRead.incrementAndGet();
    }

    public void incWrite() {
        this.dbWrite.incrementAndGet();
    }

    public void start() {
        this.refreshExecuter.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                MetricServiceImpl.this.computeTxStatsData();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        ThreadUtils.shutdownGracefully(this.refreshExecuter, 10000, TimeUnit.MILLISECONDS);
    }
}
