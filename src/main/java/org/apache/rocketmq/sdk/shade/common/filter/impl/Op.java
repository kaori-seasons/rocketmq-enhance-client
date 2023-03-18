package org.apache.rocketmq.sdk.shade.common.filter.impl;

public abstract class Op {
    private String symbol;

    public Op(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public String toString() {
        return this.symbol;
    }
}
