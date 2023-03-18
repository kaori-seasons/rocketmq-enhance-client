package org.apache.rocketmq.sdk.api;

public class MessageSelector {
    private ExpressionType type;
    private String subExpression;

    public static MessageSelector bySql(String subExpression) {
        return new MessageSelector(ExpressionType.SQL92, subExpression);
    }

    public static MessageSelector byTag(String subExpression) {
        return new MessageSelector(ExpressionType.TAG, subExpression);
    }

    private MessageSelector() {
    }

    private MessageSelector(ExpressionType type, String subExpression) {
        this.type = type;
        this.subExpression = subExpression;
    }

    public ExpressionType getType() {
        return this.type;
    }

    public String getSubExpression() {
        return this.subExpression;
    }

    public String getExpressionType() {
        return this.type.name();
    }

    public String getExpression() {
        return this.subExpression;
    }
}
