package org.apache.rocketmq.sdk.shade.remoting.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class RocketMQSerializable {
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public static byte[] rocketMQProtocolEncode(RemotingCommand cmd) {
        byte[] remarkBytes = null;
        int remarkLen = 0;
        if (cmd.getRemark() != null && cmd.getRemark().length() > 0) {
            remarkBytes = cmd.getRemark().getBytes(CHARSET_UTF8);
            remarkLen = remarkBytes.length;
        }
        byte[] extFieldsBytes = null;
        int extLen = 0;
        if (cmd.getExtFields() != null && !cmd.getExtFields().isEmpty()) {
            extFieldsBytes = mapSerialize(cmd.getExtFields());
            if (null != extFieldsBytes) {
                extLen = extFieldsBytes.length;
            }
        }
        ByteBuffer headerBuffer = ByteBuffer.allocate(calTotalLen(remarkLen, extLen));
        headerBuffer.putShort((short) cmd.getCode());
        headerBuffer.put(cmd.getLanguage().getCode());
        headerBuffer.putShort((short) cmd.getVersion());
        headerBuffer.putInt(cmd.getOpaque());
        headerBuffer.putInt(cmd.getFlag());
        if (remarkBytes != null) {
            headerBuffer.putInt(remarkBytes.length);
            headerBuffer.put(remarkBytes);
        } else {
            headerBuffer.putInt(0);
        }
        if (extFieldsBytes != null) {
            headerBuffer.putInt(extFieldsBytes.length);
            headerBuffer.put(extFieldsBytes);
        } else {
            headerBuffer.putInt(0);
        }
        return headerBuffer.array();
    }

    public static byte[] mapSerialize(HashMap<String, String> map) {
        if (null == map || map.isEmpty()) {
            return null;
        }
        int totalLength = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!(entry.getKey() == null || entry.getValue() == null)) {
                totalLength += 2 + entry.getKey().getBytes(CHARSET_UTF8).length + 4 + entry.getValue().getBytes(CHARSET_UTF8).length;
            }
        }
        ByteBuffer content = ByteBuffer.allocate(totalLength);
        for (Map.Entry<String, String> entry2 : map.entrySet()) {
            if (!(entry2.getKey() == null || entry2.getValue() == null)) {
                byte[] key = entry2.getKey().getBytes(CHARSET_UTF8);
                byte[] val = entry2.getValue().getBytes(CHARSET_UTF8);
                content.putShort((short) key.length);
                content.put(key);
                content.putInt(val.length);
                content.put(val);
            }
        }
        return content.array();
    }

    private static int calTotalLen(int remark, int ext) {
        return 17 + remark + 4 + ext;
    }

    public static RemotingCommand rocketMQProtocolDecode(byte[] headerArray) {
        RemotingCommand cmd = new RemotingCommand();
        ByteBuffer headerBuffer = ByteBuffer.wrap(headerArray);
        cmd.setCode(headerBuffer.getShort());
        cmd.setLanguage(LanguageCode.valueOf(headerBuffer.get()));
        cmd.setVersion(headerBuffer.getShort());
        cmd.setOpaque(headerBuffer.getInt());
        cmd.setFlag(headerBuffer.getInt());
        int remarkLength = headerBuffer.getInt();
        if (remarkLength > 0) {
            byte[] remarkContent = new byte[remarkLength];
            headerBuffer.get(remarkContent);
            cmd.setRemark(new String(remarkContent, CHARSET_UTF8));
        }
        int extFieldsLength = headerBuffer.getInt();
        if (extFieldsLength > 0) {
            byte[] extFieldsBytes = new byte[extFieldsLength];
            headerBuffer.get(extFieldsBytes);
            cmd.setExtFields(mapDeserialize(extFieldsBytes));
        }
        return cmd;
    }

    public static HashMap<String, String> mapDeserialize(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        HashMap<String, String> map = new HashMap<>();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        while (byteBuffer.hasRemaining()) {
            byte[] keyContent = new byte[byteBuffer.getShort()];
            byteBuffer.get(keyContent);
            byte[] valContent = new byte[byteBuffer.getInt()];
            byteBuffer.get(valContent);
            map.put(new String(keyContent, CHARSET_UTF8), new String(valContent, CHARSET_UTF8));
        }
        return map;
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
