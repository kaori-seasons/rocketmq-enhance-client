package org.apache.rocketmq.sdk.shade.remoting.protocol;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.rocketmq.sdk.shade.client.Validators;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.CommandCustomHeader;
import org.apache.rocketmq.sdk.shade.remoting.annotation.CFNotNull;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import org.apache.rocketmq.sdk.shade.remoting.exception.RemotingCommandException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.PropertyAccessor;

public class RemotingCommand {
    public static final String SERIALIZE_TYPE_PROPERTY = "rocketmq.serialize.type";
    public static final String SERIALIZE_TYPE_ENV = "ROCKETMQ_SERIALIZE_TYPE";
    public static final String REMOTING_VERSION_KEY = "rocketmq.remoting.version";
    private static final int RPC_TYPE = 0;
    private static final int RPC_ONEWAY = 1;
    private static SerializeType serializeTypeConfigInThisServer;
    private int code;
    private String remark;
    private HashMap<String, String> extFields;
    private transient CommandCustomHeader customHeader;
    private transient byte[] body;
    private String namespaceId;
    private static final InternalLogger log = InternalLoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);
    private static final Map<Class<? extends CommandCustomHeader>, Field[]> CLASS_HASH_MAP = new HashMap();
    private static final Map<Class, String> CANONICAL_NAME_CACHE = new HashMap();
    private static final Map<Field, Boolean> NULLABLE_FIELD_CACHE = new HashMap();
    private static final String STRING_CANONICAL_NAME = String.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_1 = Double.class.getCanonicalName();
    private static final String DOUBLE_CANONICAL_NAME_2 = Double.TYPE.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_1 = Integer.class.getCanonicalName();
    private static final String INTEGER_CANONICAL_NAME_2 = Integer.TYPE.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_1 = Long.class.getCanonicalName();
    private static final String LONG_CANONICAL_NAME_2 = Long.TYPE.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_1 = Boolean.class.getCanonicalName();
    private static final String BOOLEAN_CANONICAL_NAME_2 = Boolean.TYPE.getCanonicalName();
    private static volatile int configVersion = -1;
    private static AtomicInteger requestId = new AtomicInteger(0);
    private LanguageCode language = LanguageCode.JAVA;
    private int version = 0;
    private int opaque = requestId.getAndIncrement();
    private int flag = 0;
    private SerializeType serializeTypeCurrentRPC = serializeTypeConfigInThisServer;

    static {
        serializeTypeConfigInThisServer = SerializeType.JSON;
        String protocol = System.getProperty(SERIALIZE_TYPE_PROPERTY, System.getenv(SERIALIZE_TYPE_ENV));
        if (!isBlank(protocol)) {
            try {
                serializeTypeConfigInThisServer = SerializeType.valueOf(protocol);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("parser specified protocol error. protocol=" + protocol, e);
            }
        }
    }

    public static RemotingCommand createRequestCommand(int code, CommandCustomHeader customHeader) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.setCode(code);
        cmd.customHeader = customHeader;
        setCmdVersion(cmd);
        return cmd;
    }

    private static void setCmdVersion(RemotingCommand cmd) {
        if (configVersion >= 0) {
            cmd.setVersion(configVersion);
            return;
        }
        String v = System.getProperty(REMOTING_VERSION_KEY);
        if (v != null) {
            int value = Integer.parseInt(v);
            cmd.setVersion(value);
            configVersion = value;
        }
    }

    public static RemotingCommand createResponseCommand(Class<? extends CommandCustomHeader> classHeader) {
        return createResponseCommand(1, "not set any response code", classHeader);
    }

    public static RemotingCommand createResponseCommand(int code, String remark, Class<? extends CommandCustomHeader> classHeader) {
        RemotingCommand cmd = new RemotingCommand();
        cmd.markResponseType();
        cmd.setCode(code);
        cmd.setRemark(remark);
        setCmdVersion(cmd);
        if (classHeader != null) {
            try {
                cmd.customHeader = (CommandCustomHeader) classHeader.newInstance();
            } catch (IllegalAccessException e) {
                return null;
            } catch (InstantiationException e2) {
                return null;
            }
        }
        return cmd;
    }

    public static RemotingCommand createResponseCommand(int code, String remark) {
        return createResponseCommand(code, remark, null);
    }

    public static RemotingCommand decode(byte[] array) {
        return decode(ByteBuffer.wrap(array));
    }

    public static RemotingCommand decode(ByteBuffer byteBuffer) {
        int length = byteBuffer.limit();
        int oriHeaderLen = byteBuffer.getInt();
        int headerLength = getHeaderLength(oriHeaderLen);
        byte[] headerData = new byte[headerLength];
        byteBuffer.get(headerData);
        RemotingCommand cmd = headerDecode(headerData, getProtocolType(oriHeaderLen));
        int bodyLength = (length - 4) - headerLength;
        byte[] bodyData = null;
        if (bodyLength > 0) {
            bodyData = new byte[bodyLength];
            byteBuffer.get(bodyData);
        }
        cmd.body = bodyData;
        return cmd;
    }

    public static int getHeaderLength(int length) {
        return length & 16777215;
    }

    private static RemotingCommand headerDecode(byte[] headerData, SerializeType type) {
        switch (type) {
            case JSON:
                RemotingCommand resultJson = (RemotingCommand) RemotingSerializable.decode(headerData, RemotingCommand.class);
                resultJson.setSerializeTypeCurrentRPC(type);
                return resultJson;
            case ROCKETMQ:
                RemotingCommand resultRMQ = RocketMQSerializable.rocketMQProtocolDecode(headerData);
                resultRMQ.setSerializeTypeCurrentRPC(type);
                return resultRMQ;
            default:
                return null;
        }
    }

    public static SerializeType getProtocolType(int source) {
        return SerializeType.valueOf((byte) ((source >> 24) & Validators.CHARACTER_MAX_LENGTH));
    }

    public static int createNewRequestId() {
        return requestId.incrementAndGet();
    }

    public static SerializeType getSerializeTypeConfigInThisServer() {
        return serializeTypeConfigInThisServer;
    }

    private static boolean isBlank(String str) {
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

    public static byte[] markProtocolType(int source, SerializeType type) {
        return new byte[]{type.getCode(), (byte) ((source >> 16) & Validators.CHARACTER_MAX_LENGTH), (byte) ((source >> 8) & Validators.CHARACTER_MAX_LENGTH), (byte) (source & Validators.CHARACTER_MAX_LENGTH)};
    }

    public void markResponseType() {
        this.flag |= 1;
    }

    public CommandCustomHeader readCustomHeader() {
        return this.customHeader;
    }

    public void writeCustomHeader(CommandCustomHeader customHeader) {
        this.customHeader = customHeader;
    }

    public CommandCustomHeader decodeCommandCustomHeader(Class<? extends CommandCustomHeader> classHeader) throws RemotingCommandException {
        Object valueParsed;
        try {
            CommandCustomHeader objectHeader = (CommandCustomHeader) classHeader.newInstance();
            if (this.extFields != null) {
                Field[] fields = getClazzFields(classHeader);
                for (Field field : fields) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        String fieldName = field.getName();
                        if (!fieldName.startsWith("this")) {
                            try {
                                String value = this.extFields.get(fieldName);
                                if (null != value) {
                                    field.setAccessible(true);
                                    String type = getCanonicalName(field.getType());
                                    if (type.equals(STRING_CANONICAL_NAME)) {
                                        valueParsed = value;
                                    } else if (type.equals(INTEGER_CANONICAL_NAME_1) || type.equals(INTEGER_CANONICAL_NAME_2)) {
                                        valueParsed = Integer.valueOf(Integer.parseInt(value));
                                    } else if (type.equals(LONG_CANONICAL_NAME_1) || type.equals(LONG_CANONICAL_NAME_2)) {
                                        valueParsed = Long.valueOf(Long.parseLong(value));
                                    } else if (type.equals(BOOLEAN_CANONICAL_NAME_1) || type.equals(BOOLEAN_CANONICAL_NAME_2)) {
                                        valueParsed = Boolean.valueOf(Boolean.parseBoolean(value));
                                    } else if (type.equals(DOUBLE_CANONICAL_NAME_1) || type.equals(DOUBLE_CANONICAL_NAME_2)) {
                                        valueParsed = Double.valueOf(Double.parseDouble(value));
                                    } else {
                                        throw new RemotingCommandException("the custom field <" + fieldName + "> type is not supported");
                                    }
                                    field.set(objectHeader, valueParsed);
                                } else if (!isFieldNullable(field)) {
                                    throw new RemotingCommandException("the custom field <" + fieldName + "> is null");
                                }
                            } catch (Throwable e) {
                                log.error("Failed field [{}] decoding", fieldName, e);
                            }
                        } else {
                            continue;
                        }
                    }
                }
                objectHeader.checkFields();
            }
            return objectHeader;
        } catch (IllegalAccessException e2) {
            return null;
        } catch (InstantiationException e3) {
            return null;
        }
    }

    private Field[] getClazzFields(Class<? extends CommandCustomHeader> classHeader) {
        Field[] field = CLASS_HASH_MAP.get(classHeader);
        if (field == null) {
            field = classHeader.getDeclaredFields();
            synchronized (CLASS_HASH_MAP) {
                CLASS_HASH_MAP.put(classHeader, field);
            }
        }
        return field;
    }

    private boolean isFieldNullable(Field field) {
        if (!NULLABLE_FIELD_CACHE.containsKey(field)) {
            Annotation annotation = field.getAnnotation(CFNotNull.class);
            synchronized (NULLABLE_FIELD_CACHE) {
                NULLABLE_FIELD_CACHE.put(field, Boolean.valueOf(annotation == null));
            }
        }
        return NULLABLE_FIELD_CACHE.get(field).booleanValue();
    }

    private String getCanonicalName(Class clazz) {
        String name = CANONICAL_NAME_CACHE.get(clazz);
        if (name == null) {
            name = clazz.getCanonicalName();
            synchronized (CANONICAL_NAME_CACHE) {
                CANONICAL_NAME_CACHE.put(clazz, name);
            }
        }
        return name;
    }

    public ByteBuffer encode() {
        byte[] headerData = headerEncode();
        int length = 4 + headerData.length;
        if (this.body != null) {
            length += this.body.length;
        }
        ByteBuffer result = ByteBuffer.allocate(4 + length);
        result.putInt(length);
        result.put(markProtocolType(headerData.length, this.serializeTypeCurrentRPC));
        result.put(headerData);
        if (this.body != null) {
            result.put(this.body);
        }
        result.flip();
        return result;
    }

    private byte[] headerEncode() {
        makeCustomHeaderToNet();
        if (SerializeType.ROCKETMQ == this.serializeTypeCurrentRPC) {
            return RocketMQSerializable.rocketMQProtocolEncode(this);
        }
        return RemotingSerializable.encode(this);
    }

    public void makeCustomHeaderToNet() {
        if (this.customHeader != null) {
            Field[] fields = getClazzFields(this.customHeader.getClass());
            if (null == this.extFields) {
                this.extFields = new HashMap<>();
            }
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    String name = field.getName();
                    if (!name.startsWith("this")) {
                        Object value = null;
                        try {
                            field.setAccessible(true);
                            value = field.get(this.customHeader);
                        } catch (Exception e) {
                            log.error("Failed to access field [{}]", name, e);
                        }
                        if (value != null) {
                            this.extFields.put(name, value.toString());
                        }
                    }
                }
            }
        }
    }

    public ByteBuffer encodeHeader() {
        return encodeHeader(this.body != null ? this.body.length : 0);
    }

    public ByteBuffer encodeHeader(int bodyLength) {
        byte[] headerData = headerEncode();
        int length = 4 + headerData.length + bodyLength;
        ByteBuffer result = ByteBuffer.allocate((4 + length) - bodyLength);
        result.putInt(length);
        result.put(markProtocolType(headerData.length, this.serializeTypeCurrentRPC));
        result.put(headerData);
        result.flip();
        return result;
    }

    public void markOnewayRPC() {
        this.flag |= 2;
    }

    @JSONField(serialize = false)
    public boolean isOnewayRPC() {
        return (this.flag & 2) == 2;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @JSONField(serialize = false)
    public RemotingCommandType getType() {
        if (isResponseType()) {
            return RemotingCommandType.RESPONSE_COMMAND;
        }
        return RemotingCommandType.REQUEST_COMMAND;
    }

    @JSONField(serialize = false)
    public boolean isResponseType() {
        return (this.flag & 1) == 1;
    }

    public LanguageCode getLanguage() {
        return this.language;
    }

    public void setLanguage(LanguageCode language) {
        this.language = language;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getOpaque() {
        return this.opaque;
    }

    public void setOpaque(int opaque) {
        this.opaque = opaque;
    }

    public int getFlag() {
        return this.flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public HashMap<String, String> getExtFields() {
        return this.extFields;
    }

    public void setExtFields(HashMap<String, String> extFields) {
        this.extFields = extFields;
    }

    public void addExtField(String key, String value) {
        if (null == this.extFields) {
            this.extFields = new HashMap<>();
        }
        this.extFields.put(key, value);
    }

    public String toString() {
        return "RemotingCommand [code=" + this.code + ", language=" + this.language + ", version=" + this.version + ", opaque=" + this.opaque + ", flag(B)=" + Integer.toBinaryString(this.flag) + ", remark=" + this.remark + ", extFields=" + this.extFields + ", serializeTypeCurrentRPC=" + this.serializeTypeCurrentRPC + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }

    public SerializeType getSerializeTypeCurrentRPC() {
        return this.serializeTypeCurrentRPC;
    }

    public void setSerializeTypeCurrentRPC(SerializeType serializeTypeCurrentRPC) {
        this.serializeTypeCurrentRPC = serializeTypeCurrentRPC;
    }

    public String getNamespaceId() {
        return this.namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }
}
