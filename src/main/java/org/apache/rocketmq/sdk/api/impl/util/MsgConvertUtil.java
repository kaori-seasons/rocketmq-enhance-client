package org.apache.rocketmq.sdk.api.impl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MsgConvertUtil {
    public static final byte[] emptyBytes = new byte[0];
    public static final String emptyString = "";
    public static final String JMS_MSGMODEL = "jmsMsgModel";
    public static final String COMPATIBLE_FIELD_MSGMODEL = "notifyOutMsgModel";
    public static final String MSGMODEL_TEXT = "textMessage";
    public static final String MSGMODEL_BYTES = "bytesMessage";
    public static final String MSGMODEL_OBJ = "objectMessage";
    public static final String MSG_TOPIC = "msgTopic";
    public static final String MSG_TYPE = "msgType";

    public static byte[] objectSerialize(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();
        baos.close();
        return baos.toByteArray();
    }

    public static Serializable objectDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ois.close();
        bais.close();
        return (Serializable) ois.readObject();
    }

    public static final byte[] string2Bytes(String s, String charset) {
        if (null == s) {
            return emptyBytes;
        }
        byte[] bs = null;
        try {
            bs = s.getBytes(charset);
        } catch (Exception e) {
        }
        return bs;
    }

    public static final String bytes2String(byte[] bs, String charset) {
        if (null == bs) {
            return "";
        }
        String s = null;
        try {
            s = new String(bs, charset);
        } catch (Exception e) {
        }
        return s;
    }
}
