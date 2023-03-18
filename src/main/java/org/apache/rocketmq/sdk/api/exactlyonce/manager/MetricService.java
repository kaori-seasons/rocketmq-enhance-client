package org.apache.rocketmq.sdk.api.exactlyonce.manager;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.impl.MetricServiceImpl;

import java.util.Map;

public class MetricService {
    private MetricServiceImpl metricServiceImpl;

    private MetricService() {
        this.metricServiceImpl = null;
        this.metricServiceImpl = new MetricServiceImpl();
    }

    public void start() {
        this.metricServiceImpl.start();
    }

    public void stop() {
        this.metricServiceImpl.stop();
    }

    public void record(MQTxContext context) {
        this.metricServiceImpl.record(context);
    }

    public void incQueryExpired(long begin) {
        this.metricServiceImpl.incQueryExpired(begin);
    }

    public void incQueryAcked(long begin) {
        this.metricServiceImpl.incQueryAcked(begin);
    }

    public void incQueryMsgIdCount(long begin) {
        this.metricServiceImpl.incQueryMsgIdCount(begin);
    }

    public void incInsertRecord(long begin) {
        this.metricServiceImpl.incInsertRecord(begin);
    }

    public void incRead() {
        this.metricServiceImpl.incRead();
    }

    public void incWrite() {
        this.metricServiceImpl.incWrite();
    }

    public void incDeleteRecord(long begin) {
        this.metricServiceImpl.incDeleteRecord(begin);
    }

    public Map<String, String> getCurrentConsumeStatus() {
        return this.metricServiceImpl.getCurrentConsumeStatus();
    }

    public static final MetricService getInstance() {
        return MetricServiceHolder.INSTANCE;
    }

    private static class MetricServiceHolder {
        private static final MetricService INSTANCE = new MetricService();

        private MetricServiceHolder() {
        }
    }
}
