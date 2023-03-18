package org.apache.rocketmq.sdk.shade.client.exception;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;

public class MQClientException extends Exception {
    private static final long serialVersionUID = -5758410930844185841L;
    private int responseCode;
    private String errorMessage;

    public MQClientException(String errorMessage, Throwable cause) {
        super(FAQUrl.attachDefaultURL(errorMessage), cause);
        this.responseCode = -1;
        this.errorMessage = errorMessage;
    }

    public MQClientException(int responseCode, String errorMessage) {
        super(FAQUrl.attachDefaultURL("CODE: " + UtilAll.responseCode2String(responseCode) + "  DESC: " + errorMessage));
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public MQClientException setResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
