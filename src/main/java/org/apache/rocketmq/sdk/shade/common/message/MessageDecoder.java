package org.apache.rocketmq.sdk.shade.common.message;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageDecoder {
    public static final int MSG_ID_LENGTH = 16;
    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    public static final int MESSAGE_MAGIC_CODE_POSTION = 4;
    public static final int MESSAGE_FLAG_POSTION = 16;
    public static final int MESSAGE_PHYSIC_OFFSET_POSTION = 28;
    public static final int MESSAGE_STORE_TIMESTAMP_POSTION = 56;
    public static final int MESSAGE_MAGIC_CODE = -626843481;
    public static final char NAME_VALUE_SEPARATOR = 1;
    public static final char PROPERTY_SEPARATOR = 2;
    public static final int PHY_POS_POSITION = 28;
    public static final int QUEUE_OFFSET_POSITION = 20;
    public static final int SYSFLAG_POSITION = 36;
    public static final int BODY_SIZE_POSITION = 84;

    public static String createMessageId(ByteBuffer input, ByteBuffer addr, long offset) {
        input.flip();
        input.limit(addr.limit() == 8 ? 16 : 28);
        input.put(addr);
        input.putLong(offset);
        return UtilAll.bytes2string(input.array());
    }

    public static String createMessageId(SocketAddress socketAddress, long transactionIdhashCode) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        byteBuffer.put(inetSocketAddress.getAddress().getAddress());
        byteBuffer.putInt(inetSocketAddress.getPort());
        byteBuffer.putLong(transactionIdhashCode);
        byteBuffer.flip();
        return UtilAll.bytes2string(byteBuffer.array());
    }

    public static MessageId decodeMessageId(String msgId) throws UnknownHostException {
        int ipLength = msgId.length() == 32 ? 8 : 32;
        return new MessageId(new InetSocketAddress(InetAddress.getByAddress(UtilAll.string2bytes(msgId.substring(0, ipLength))), ByteBuffer.wrap(UtilAll.string2bytes(msgId.substring(ipLength, ipLength + 8))).getInt(0)), ByteBuffer.wrap(UtilAll.string2bytes(msgId.substring(ipLength + 8, ipLength + 8 + 16))).getLong(0));
    }

    public static Map<String, String> decodeProperties(ByteBuffer byteBuffer) {
        int sysFlag = byteBuffer.getInt(36);
        int bodySizePosition = 48 + ((sysFlag & 16) == 0 ? 8 : 20) + 8 + ((sysFlag & 32) == 0 ? 8 : 20) + 4 + 8;
        int topicLengthPosition = bodySizePosition + 4 + byteBuffer.getInt(bodySizePosition);
        byte topicLength = byteBuffer.get(topicLengthPosition);
        int i = byteBuffer.getShort(topicLengthPosition + 1 + topicLength);
        byteBuffer.position(topicLengthPosition + 1 + topicLength + 2);
        if (i <= 0) {
            return null;
        }
        byte[] properties = new byte[i];
        byteBuffer.get(properties);
        return string2messageProperties(new String(properties, CHARSET_UTF8));
    }

    public static MessageExt decode(ByteBuffer byteBuffer) {
        return decode(byteBuffer, true, true, false);
    }

    public static MessageExt clientDecode(ByteBuffer byteBuffer, boolean readBody) {
        return decode(byteBuffer, readBody, true, true);
    }

    public static MessageExt decode(ByteBuffer byteBuffer, boolean readBody) {
        return decode(byteBuffer, readBody, true, false);
    }

    public static byte[] encode(MessageExt messageExt, boolean needCompress) throws Exception {
        ByteBuffer byteBuffer;
        byte[] body = messageExt.getBody();
        byte[] topics = messageExt.getTopic().getBytes(CHARSET_UTF8);
        byte topicLen = (byte) topics.length;
        byte[] propertiesBytes = messageProperties2String(messageExt.getProperties()).getBytes(CHARSET_UTF8);
        short propertiesLength = (short) propertiesBytes.length;
        int sysFlag = messageExt.getSysFlag();
        int bornhostLength = (sysFlag & 16) == 0 ? 8 : 20;
        int storehostAddressLength = (sysFlag & 32) == 0 ? 8 : 20;
        byte[] newBody = messageExt.getBody();
        if (needCompress && (sysFlag & 1) == 1) {
            newBody = UtilAll.compress(body, 5);
        }
        int bodyLength = newBody.length;
        int storeSize = messageExt.getStoreSize();
        if (storeSize > 0) {
            byteBuffer = ByteBuffer.allocate(storeSize);
        } else {
            storeSize = 48 + bornhostLength + 8 + storehostAddressLength + 4 + 8 + 4 + bodyLength + 1 + topicLen + 2 + propertiesLength + 0;
            byteBuffer = ByteBuffer.allocate(storeSize);
        }
        byteBuffer.putInt(storeSize);
        byteBuffer.putInt(MESSAGE_MAGIC_CODE);
        byteBuffer.putInt(messageExt.getBodyCRC());
        byteBuffer.putInt(messageExt.getQueueId());
        byteBuffer.putInt(messageExt.getFlag());
        byteBuffer.putLong(messageExt.getQueueOffset());
        byteBuffer.putLong(messageExt.getCommitLogOffset());
        byteBuffer.putInt(sysFlag);
        byteBuffer.putLong(messageExt.getBornTimestamp());
        InetSocketAddress bornHost = (InetSocketAddress) messageExt.getBornHost();
        byteBuffer.put(bornHost.getAddress().getAddress());
        byteBuffer.putInt(bornHost.getPort());
        byteBuffer.putLong(messageExt.getStoreTimestamp());
        InetSocketAddress serverHost = (InetSocketAddress) messageExt.getStoreHost();
        byteBuffer.put(serverHost.getAddress().getAddress());
        byteBuffer.putInt(serverHost.getPort());
        byteBuffer.putInt(messageExt.getReconsumeTimes());
        byteBuffer.putLong(messageExt.getPreparedTransactionOffset());
        byteBuffer.putInt(bodyLength);
        byteBuffer.put(newBody);
        byteBuffer.put(topicLen);
        byteBuffer.put(topics);
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);
        return byteBuffer.array();
    }

    public static MessageExt decode(ByteBuffer byteBuffer, boolean readBody, boolean deCompressBody) {
        return decode(byteBuffer, readBody, deCompressBody, false);
    }

    public static MessageExt decode(ByteBuffer byteBuffer, boolean readBody, boolean deCompressBody, boolean isClient) {
        MessageExt msgExt;
        try {
            if (isClient) {
                msgExt = new MessageClientExt();
            } else {
                msgExt = new MessageExt();
            }
            msgExt.setStoreSize(byteBuffer.getInt());
            byteBuffer.getInt();
            msgExt.setBodyCRC(byteBuffer.getInt());
            msgExt.setQueueId(byteBuffer.getInt());
            msgExt.setFlag(byteBuffer.getInt());
            msgExt.setQueueOffset(byteBuffer.getLong());
            msgExt.setCommitLogOffset(byteBuffer.getLong());
            int sysFlag = byteBuffer.getInt();
            msgExt.setSysFlag(sysFlag);
            msgExt.setBornTimestamp(byteBuffer.getLong());
            int bornhostIPLength = (sysFlag & 16) == 0 ? 4 : 16;
            byte[] bornHost = new byte[bornhostIPLength];
            byteBuffer.get(bornHost, 0, bornhostIPLength);
            msgExt.setBornHost(new InetSocketAddress(InetAddress.getByAddress(bornHost), byteBuffer.getInt()));
            msgExt.setStoreTimestamp(byteBuffer.getLong());
            int storehostIPLength = (sysFlag & 32) == 0 ? 4 : 16;
            byte[] storeHost = new byte[storehostIPLength];
            byteBuffer.get(storeHost, 0, storehostIPLength);
            msgExt.setStoreHost(new InetSocketAddress(InetAddress.getByAddress(storeHost), byteBuffer.getInt()));
            msgExt.setReconsumeTimes(byteBuffer.getInt());
            msgExt.setPreparedTransactionOffset(byteBuffer.getLong());
            int bodyLen = byteBuffer.getInt();
            if (bodyLen > 0) {
                if (readBody) {
                    byte[] body = new byte[bodyLen];
                    byteBuffer.get(body);
                    if (deCompressBody && (sysFlag & 1) == 1) {
                        body = UtilAll.uncompress(body);
                    }
                    msgExt.setBody(body);
                } else {
                    byteBuffer.position(byteBuffer.position() + bodyLen);
                }
            }
            byte[] topic = new byte[byteBuffer.get()];
            byteBuffer.get(topic);
            msgExt.setTopic(new String(topic, CHARSET_UTF8));
            int i = byteBuffer.getShort();
            if (i > 0) {
                byte[] properties = new byte[i];
                byteBuffer.get(properties);
                msgExt.setProperties(string2messageProperties(new String(properties, CHARSET_UTF8)));
            }
            String msgId = createMessageId(ByteBuffer.allocate(storehostIPLength + 4 + 8), msgExt.getStoreHostBytes(), msgExt.getCommitLogOffset());
            msgExt.setMsgId(msgId);
            if (isClient) {
                ((MessageClientExt) msgExt).setOffsetMsgId(msgId);
                MessageAccessor.putProperty(msgExt, MessageConst.PROPERTY_DECODED_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                MessageAccessor.clearProperty(msgExt, MessageConst.PROPERTY_CONSUME_START_TIMESTAMP);
                MessageAccessor.putProperty(msgExt, MessageConst.PROPERTY_FQN_TOPIC, msgExt.getTopic());
            }
            return msgExt;
        } catch (Exception e) {
            byteBuffer.position(byteBuffer.limit());
            return null;
        }
    }

    public static List<MessageExt> decodes(ByteBuffer byteBuffer) {
        return decodes(byteBuffer, true);
    }

    public static List<MessageExt> decodesBatch(ByteBuffer byteBuffer, boolean readBody, boolean decompressBody, boolean isClient) {
        MessageExt msgExt;
        List<MessageExt> msgExts = new ArrayList<>();
        while (byteBuffer.hasRemaining() && null != (msgExt = decode(byteBuffer, readBody, decompressBody, isClient))) {
            msgExts.add(msgExt);
        }
        return msgExts;
    }

    public static List<MessageExt> decodes(ByteBuffer byteBuffer, boolean readBody) {
        MessageExt msgExt;
        List<MessageExt> msgExts = new ArrayList<>();
        while (byteBuffer.hasRemaining() && null != (msgExt = clientDecode(byteBuffer, readBody))) {
            msgExts.add(msgExt);
        }
        return msgExts;
    }

    public static String messageProperties2String(Map<String, String> properties) {
        if (properties == null) {
            return "";
        }
        int len = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (value != null) {
                if (name != null) {
                    len += name.length();
                }
                len = len + value.length() + 2;
            }
        }
        StringBuilder sb = new StringBuilder(len);
        if (properties != null) {
            for (Map.Entry<String, String> entry2 : properties.entrySet()) {
                String name2 = entry2.getKey();
                String value2 = entry2.getValue();
                if (value2 != null) {
                    sb.append(name2);
                    sb.append((char) 1);
                    sb.append(value2);
                    sb.append((char) 2);
                }
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        return sb.toString();
    }

    public static Map<String, String> string2messageProperties(String properties) {
        int newIndex;
        int kvSepIndex;
        Map<String, String> map = new HashMap<>();
        if (properties != null) {
            int len = properties.length();
            for (int index = 0; index < len; index = newIndex + 1) {
                newIndex = properties.indexOf(2, index);
                if (newIndex < 0) {
                    newIndex = len;
                }
                if (newIndex - index >= 3 && (kvSepIndex = properties.indexOf(1, index)) > index && kvSepIndex < newIndex - 1) {
                    map.put(properties.substring(index, kvSepIndex), properties.substring(kvSepIndex + 1, newIndex));
                }
            }
        }
        return map;
    }

    public static byte[] encodeMessage(Message message) {
        byte[] body = message.getBody();
        int bodyLen = body.length;
        byte[] propertiesBytes = messageProperties2String(message.getProperties()).getBytes(CHARSET_UTF8);
        int propsLen = propertiesBytes.length;
        if (propsLen > 32767) {
            throw new RuntimeException(String.format("Properties size of message exceeded, properties size: {}, maxSize: {}.", Integer.valueOf(propsLen), Short.MAX_VALUE));
        }
        short propertiesLength = (short) propsLen;
        message.getFlag();
        int storeSize = 20 + bodyLen + 2 + propertiesLength;
        ByteBuffer byteBuffer = ByteBuffer.allocate(storeSize);
        byteBuffer.putInt(storeSize);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(message.getFlag());
        byteBuffer.putInt(bodyLen);
        byteBuffer.put(body);
        byteBuffer.putShort(propertiesLength);
        byteBuffer.put(propertiesBytes);
        return byteBuffer.array();
    }

    public static Message decodeMessage(ByteBuffer byteBuffer) throws Exception {
        Message message = new Message();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        message.setFlag(byteBuffer.getInt());
        byte[] body = new byte[byteBuffer.getInt()];
        byteBuffer.get(body);
        message.setBody(body);
        byte[] propertiesBytes = new byte[byteBuffer.getShort()];
        byteBuffer.get(propertiesBytes);
        message.setProperties(string2messageProperties(new String(propertiesBytes, CHARSET_UTF8)));
        return message;
    }

    public static byte[] encodeMessages(List<Message> messages) {
        List<byte[]> encodedMessages = new ArrayList<>(messages.size());
        int allSize = 0;
        for (Message message : messages) {
            byte[] tmp = encodeMessage(message);
            encodedMessages.add(tmp);
            allSize += tmp.length;
        }
        byte[] allBytes = new byte[allSize];
        int pos = 0;
        for (byte[] bytes : encodedMessages) {
            System.arraycopy(bytes, 0, allBytes, pos, bytes.length);
            pos += bytes.length;
        }
        return allBytes;
    }

    public static List<Message> decodeMessages(ByteBuffer byteBuffer) throws Exception {
        List<Message> msgs = new ArrayList<>();
        while (byteBuffer.hasRemaining()) {
            msgs.add(decodeMessage(byteBuffer));
        }
        return msgs;
    }
}
