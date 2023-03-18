package org.apache.rocketmq.sdk.shade.common.protocol.body;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.sdk.shade.common.DataVersion;
import org.apache.rocketmq.sdk.shade.common.TopicConfig;
import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingSerializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class RegisterBrokerBody extends RemotingSerializable {
    private static final InternalLogger LOGGER;
    private TopicConfigSerializeWrapper topicConfigSerializeWrapper = new TopicConfigSerializeWrapper();
    private List<String> filterServerList = new ArrayList();
    static final boolean assertResult;

    static {
        assertResult = !RegisterBrokerBody.class.desiredAssertionStatus();
        LOGGER = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    }

    public byte[] encode(boolean compress) {
        if (!compress) {
            return super.encode();
        }
        long start = System.currentTimeMillis();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream outputStream = new DeflaterOutputStream(byteArrayOutputStream, new Deflater(9));
        DataVersion dataVersion = this.topicConfigSerializeWrapper.getDataVersion();
        ConcurrentMap<String, TopicConfig> topicConfigTable = cloneTopicConfigTable(this.topicConfigSerializeWrapper.getTopicConfigTable());
        if (assertResult || topicConfigTable != null) {
            try {
                byte[] buffer = dataVersion.encode();
                outputStream.write(convertIntToByteArray(buffer.length));
                outputStream.write(buffer);
                outputStream.write(convertIntToByteArray(topicConfigTable.size()));
                for (Map.Entry<String, TopicConfig> next : topicConfigTable.entrySet()) {
                    byte[] buffer2 = next.getValue().encode().getBytes("UTF-8");
                    outputStream.write(convertIntToByteArray(buffer2.length));
                    outputStream.write(buffer2);
                }
                byte[] buffer3 = JSON.toJSONString(this.filterServerList).getBytes("UTF-8");
                outputStream.write(convertIntToByteArray(buffer3.length));
                outputStream.write(buffer3);
                outputStream.finish();
                long interval = System.currentTimeMillis() - start;
                if (interval > 50) {
                    LOGGER.info("Compressing takes {}ms", Long.valueOf(interval));
                }
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                LOGGER.error("Failed to compress RegisterBrokerBody object", (Throwable) e);
                return null;
            }
        } else {
            throw new AssertionError();
        }
    }

    public static RegisterBrokerBody decode(byte[] data, boolean compressed) throws IOException {
        if (!compressed) {
            return (RegisterBrokerBody) decode(data, RegisterBrokerBody.class);
        }
        long start = System.currentTimeMillis();
        InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(data));
        DataVersion dataVersion = (DataVersion) DataVersion.decode(readBytes(inflaterInputStream, readInt(inflaterInputStream)), DataVersion.class);
        RegisterBrokerBody registerBrokerBody = new RegisterBrokerBody();
        registerBrokerBody.getTopicConfigSerializeWrapper().setDataVersion(dataVersion);
        ConcurrentMap<String, TopicConfig> topicConfigTable = registerBrokerBody.getTopicConfigSerializeWrapper().getTopicConfigTable();
        int topicConfigNumber = readInt(inflaterInputStream);
        LOGGER.debug("{} topic configs to extract", Integer.valueOf(topicConfigNumber));
        for (int i = 0; i < topicConfigNumber; i++) {
            byte[] buffer = readBytes(inflaterInputStream, readInt(inflaterInputStream));
            TopicConfig topicConfig = new TopicConfig();
            topicConfig.decode(new String(buffer, "UTF-8"));
            topicConfigTable.put(topicConfig.getTopicName(), topicConfig);
        }
        String filterServerListJson = new String(readBytes(inflaterInputStream, readInt(inflaterInputStream)), "UTF-8");
        List<String> filterServerList = new ArrayList<>();
        try {
            filterServerList = JSON.parseArray(filterServerListJson, String.class);
        } catch (Exception e) {
            LOGGER.error("Decompressing occur Exception {}", filterServerListJson);
        }
        registerBrokerBody.setFilterServerList(filterServerList);
        long interval = System.currentTimeMillis() - start;
        if (interval > 50) {
            LOGGER.info("Decompressing takes {}ms", Long.valueOf(interval));
        }
        return registerBrokerBody;
    }

    private static byte[] convertIntToByteArray(int n) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(n);
        return byteBuffer.array();
    }

    private static byte[] readBytes(InflaterInputStream inflaterInputStream, int length) throws IOException {
        byte[] buffer = new byte[length];
        int bytesRead = 0;
        while (bytesRead < length) {
            int len = inflaterInputStream.read(buffer, bytesRead, length - bytesRead);
            if (len == -1) {
                throw new IOException("End of compressed data has reached");
            }
            bytesRead += len;
        }
        return buffer;
    }

    private static int readInt(InflaterInputStream inflaterInputStream) throws IOException {
        return ByteBuffer.wrap(readBytes(inflaterInputStream, 4)).getInt();
    }

    public TopicConfigSerializeWrapper getTopicConfigSerializeWrapper() {
        return this.topicConfigSerializeWrapper;
    }

    public void setTopicConfigSerializeWrapper(TopicConfigSerializeWrapper topicConfigSerializeWrapper) {
        this.topicConfigSerializeWrapper = topicConfigSerializeWrapper;
    }

    public List<String> getFilterServerList() {
        return this.filterServerList;
    }

    public void setFilterServerList(List<String> filterServerList) {
        this.filterServerList = filterServerList;
    }

    public static ConcurrentMap<String, TopicConfig> cloneTopicConfigTable(ConcurrentMap<String, TopicConfig> topicConfigConcurrentMap) {
        ConcurrentHashMap<String, TopicConfig> result = new ConcurrentHashMap<>();
        if (topicConfigConcurrentMap != null) {
            for (Map.Entry<String, TopicConfig> entry : topicConfigConcurrentMap.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
