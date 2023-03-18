package org.apache.rocketmq.sdk.shade.common.filter.impl;

public class Operator extends Op {
    public static final Operator LEFTPARENTHESIS = new Operator("(", 30, false);
    public static final Operator RIGHTPARENTHESIS = new Operator(")", 30, false);
    public static final Operator AND = new Operator("&&", 20, true);
    public static final Operator OR = new Operator("||", 15, true);
    private int priority;
    private boolean compareable;

    private Operator(String symbol, int priority, boolean compareable) {
        super(symbol);
        this.priority = priority;
        this.compareable = compareable;
    }

    public static Operator createOperator(String operator) {
        if (LEFTPARENTHESIS.getSymbol().equals(operator)) {
            return LEFTPARENTHESIS;
        }
        if (RIGHTPARENTHESIS.getSymbol().equals(operator)) {
            return RIGHTPARENTHESIS;
        }
        if (AND.getSymbol().equals(operator)) {
            return AND;
        }
        if (OR.getSymbol().equals(operator)) {
            return OR;
        }
        throw new IllegalArgumentException("unsupport operator " + operator);
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isCompareable() {
        return this.compareable;
    }

    public int compare(Operator operator) {
        if (this.priority > operator.priority) {
            return 1;
        }
        if (this.priority == operator.priority) {
            return 0;
        }
        return -1;
    }

    public boolean isSpecifiedOp(String operator) {
        return getSymbol().equals(operator);
    }
}
