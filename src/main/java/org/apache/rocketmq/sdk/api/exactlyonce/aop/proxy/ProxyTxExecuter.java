package org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.proxy.impl.ProxyTxExecuterImpl;

public class ProxyTxExecuter implements TxExecuter {
    private ProxyTxExecuterImpl proxyTxExecuterImpl = new ProxyTxExecuterImpl();

    @Override
    public Object excute(InternalCallback callback) {
        return this.proxyTxExecuterImpl.excute(callback);
    }

    @Override
    public Object excute(InternalCallback callback, Object object) {
        return this.proxyTxExecuterImpl.excute(callback, object);
    }

    @Override
    public void begin() {
        this.proxyTxExecuterImpl.begin();
    }

    @Override
    public void begin(int timeout) {
        this.proxyTxExecuterImpl.begin(timeout);
    }

    @Override
    public void commit() {
        this.proxyTxExecuterImpl.commit();
    }

    @Override
    public void rollback() {
        this.proxyTxExecuterImpl.rollback();
    }

    public static final ProxyTxExecuter getInstance() {
        return ProxyTxExecuterHolder.INSTANCE;
    }

    public static class ProxyTxExecuterHolder {
        private static final ProxyTxExecuter INSTANCE = new ProxyTxExecuter();

        private ProxyTxExecuterHolder() {
        }
    }
}
