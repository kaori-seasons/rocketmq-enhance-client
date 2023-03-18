package org.apache.rocketmq.sdk.shade.common.protocol.header;

import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNullable;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;

public class SendMessageRequestHeaderV2 implements CommandCustomHeader {
    @CFNotNull
    private String a;
    @CFNotNull
    private String b;
    @CFNotNull
    private String c;
    @CFNotNull
    private Integer d;
    @CFNotNull
    private Integer e;
    @CFNotNull
    private Integer f;
    @CFNotNull
    private Long g;
    @CFNotNull
    private Integer h;
    @CFNullable
    private String i;
    @CFNullable
    private Integer j;
    @CFNullable
    private boolean k;
    private Integer l;
    @CFNullable
    private boolean m;

    public static SendMessageRequestHeader createSendMessageRequestHeaderV1(SendMessageRequestHeaderV2 v2) {
        SendMessageRequestHeader v1 = new SendMessageRequestHeader();
        v1.setProducerGroup(v2.a);
        v1.setTopic(v2.b);
        v1.setDefaultTopic(v2.c);
        v1.setDefaultTopicQueueNums(v2.d);
        v1.setQueueId(v2.e);
        v1.setSysFlag(v2.f);
        v1.setBornTimestamp(v2.g);
        v1.setFlag(v2.h);
        v1.setProperties(v2.i);
        v1.setReconsumeTimes(v2.j);
        v1.setUnitMode(v2.k);
        v1.setMaxReconsumeTimes(v2.l);
        v1.setBatch(v2.m);
        return v1;
    }

    public static SendMessageRequestHeaderV2 createSendMessageRequestHeaderV2(SendMessageRequestHeader v1) {
        SendMessageRequestHeaderV2 v2 = new SendMessageRequestHeaderV2();
        v2.a = v1.getProducerGroup();
        v2.b = v1.getTopic();
        v2.c = v1.getDefaultTopic();
        v2.d = v1.getDefaultTopicQueueNums();
        v2.e = v1.getQueueId();
        v2.f = v1.getSysFlag();
        v2.g = v1.getBornTimestamp();
        v2.h = v1.getFlag();
        v2.i = v1.getProperties();
        v2.j = v1.getReconsumeTimes();
        v2.k = v1.isUnitMode();
        v2.l = v1.getMaxReconsumeTimes();
        v2.m = v1.isBatch();
        return v2;
    }

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getA() {
        return this.a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return this.b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public String getC() {
        return this.c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public Integer getD() {
        return this.d;
    }

    public void setD(Integer d) {
        this.d = d;
    }

    public Integer getE() {
        return this.e;
    }

    public void setE(Integer e) {
        this.e = e;
    }

    public Integer getF() {
        return this.f;
    }

    public void setF(Integer f) {
        this.f = f;
    }

    public Long getG() {
        return this.g;
    }

    public void setG(Long g) {
        this.g = g;
    }

    public Integer getH() {
        return this.h;
    }

    public void setH(Integer h) {
        this.h = h;
    }

    public String getI() {
        return this.i;
    }

    public void setI(String i) {
        this.i = i;
    }

    public Integer getJ() {
        return this.j;
    }

    public void setJ(Integer j) {
        this.j = j;
    }

    public boolean isK() {
        return this.k;
    }

    public void setK(boolean k) {
        this.k = k;
    }

    public Integer getL() {
        return this.l;
    }

    public void setL(Integer l) {
        this.l = l;
    }

    public boolean isM() {
        return this.m;
    }

    public void setM(boolean m) {
        this.m = m;
    }
}
