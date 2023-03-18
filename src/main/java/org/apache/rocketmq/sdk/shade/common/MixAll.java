package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.common.annotation.ImportantField;
import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.common.help.FAQUrl;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;

public class MixAll {
    public static final String ROCKETMQ_HOME_ENV = "ROCKETMQ_HOME";
    public static final String ROCKETMQ_HOME_PROPERTY = "rocketmq.home.dir";
    public static final String NAMESRV_ADDR_ENV = "NAMESRV_ADDR";
    public static final String NAMESRV_ADDR_PROPERTY = "rocketmq.namesrv.addr";
    public static final String MESSAGE_COMPRESS_LEVEL = "rocketmq.message.compressLevel";
    public static final String AUTO_CREATE_TOPIC_KEY_TOPIC = "TBW102";
    public static final String BENCHMARK_TOPIC = "BenchmarkTest";
    public static final String DEFAULT_PRODUCER_GROUP = "DEFAULT_PRODUCER";
    public static final String DEFAULT_CONSUMER_GROUP = "DEFAULT_CONSUMER";
    public static final String TOOLS_CONSUMER_GROUP = "TOOLS_CONSUMER";
    public static final String FILTERSRV_CONSUMER_GROUP = "FILTERSRV_CONSUMER";
    public static final String MONITOR_CONSUMER_GROUP = "__MONITOR_CONSUMER";
    public static final String CLIENT_INNER_PRODUCER_GROUP = "CLIENT_INNER_PRODUCER";
    public static final String SELF_TEST_PRODUCER_GROUP = "SELF_TEST_P_GROUP";
    public static final String SELF_TEST_CONSUMER_GROUP = "SELF_TEST_C_GROUP";
    public static final String SELF_TEST_TOPIC = "SELF_TEST_TOPIC";
    public static final String OFFSET_MOVED_EVENT = "OFFSET_MOVED_EVENT";
    public static final String ONS_HTTP_PROXY_GROUP = "CID_ONS-HTTP-PROXY";
    public static final String CID_ONSAPI_PERMISSION_GROUP = "CID_ONSAPI_PERMISSION";
    public static final String CID_ONSAPI_OWNER_GROUP = "CID_ONSAPI_OWNER";
    public static final String CID_ONSAPI_PULL_GROUP = "CID_ONSAPI_PULL";
    public static final String CID_RMQ_SYS_PREFIX = "CID_RMQ_SYS_";
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final long MASTER_ID = 0;
    public static final String RETRY_GROUP_TOPIC_PREFIX = "%RETRY%";
    public static final String DLQ_GROUP_TOPIC_PREFIX = "%DLQ%";
    public static final String SYSTEM_TOPIC_PREFIX = "rmq_sys_";
    public static final String UNIQUE_MSG_QUERY_FLAG = "_UNIQUE_KEY_QUERY";
    public static final String DEFAULT_TRACE_REGION_ID = "DefaultRegion";
    public static final String CONSUME_CONTEXT_TYPE = "ConsumeContextType";
    public static final String RMQ_SYS_TRACE_TOPIC = "RMQ_SYS_TRACE_TOPIC";
    public static final String RMQ_SYS_TRANS_HALF_TOPIC = "RMQ_SYS_TRANS_HALF_TOPIC";
    public static final String RMQ_SYS_TRANS_OP_HALF_TOPIC = "RMQ_SYS_TRANS_OP_HALF_TOPIC";
    public static final String CID_SYS_RMQ_TRANS = "CID_RMQ_SYS_TRANS";
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    public static final String DEFAULT_NAMESRV_ADDR_LOOKUP = "jmenv.tbsite.net";
    public static final String WS_DOMAIN_NAME = System.getProperty("rocketmq.namesrv.domain", DEFAULT_NAMESRV_ADDR_LOOKUP);
    public static final String WS_DOMAIN_SUBGROUP = System.getProperty("rocketmq.namesrv.domain.subgroup", "nsaddr");
    public static final List<String> LOCAL_INET_ADDRESS = getLocalInetAddress();
    public static final String LOCALHOST = localhost();
    public static final long CURRENT_JVM_PID = getPID();

    public static String getWSAddr() {
        String wsDomainName = System.getProperty("rocketmq.namesrv.domain", DEFAULT_NAMESRV_ADDR_LOOKUP);
        String wsDomainSubgroup = System.getProperty("rocketmq.namesrv.domain.subgroup", "nsaddr");
        String wsAddr = "http://" + wsDomainName + ":8080/rocketmq/" + wsDomainSubgroup;
        if (wsDomainName.indexOf(":") > 0) {
            wsAddr = "http://" + wsDomainName + "/rocketmq/" + wsDomainSubgroup;
        }
        return wsAddr;
    }

    public static String getRetryTopic(String consumerGroup) {
        return RETRY_GROUP_TOPIC_PREFIX + consumerGroup;
    }

    public static boolean isSysConsumerGroup(String consumerGroup) {
        return consumerGroup.startsWith(CID_RMQ_SYS_PREFIX);
    }

    public static boolean isSystemTopic(String topic) {
        return topic.startsWith(SYSTEM_TOPIC_PREFIX);
    }

    public static String getDLQTopic(String consumerGroup) {
        return DLQ_GROUP_TOPIC_PREFIX + consumerGroup;
    }

    public static String brokerVIPChannel(boolean isChange, String brokerAddr) {
        if (!isChange) {
            return brokerAddr;
        }
        String[] ipAndPort = brokerAddr.split(":");
        return ipAndPort[0] + ":" + (Integer.parseInt(ipAndPort[1]) - 2);
    }

    public static long getPID() {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if (processName == null || processName.length() <= 0) {
            return 0;
        }
        try {
            return Long.parseLong(processName.split("@")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void string2File(String str, String fileName) throws IOException {
        String tmpFile = fileName + DiskFileUpload.postfix;
        string2FileNotSafe(str, tmpFile);
        String bakFile = fileName + ".bak";
        String prevContent = file2String(fileName);
        if (prevContent != null) {
            string2FileNotSafe(prevContent, bakFile);
        }
        new File(fileName).delete();
        new File(tmpFile).renameTo(new File(fileName));
    }

    public static void string2FileNotSafe(String str, String fileName) throws IOException {
        File file = new File(fileName);
        File fileParent = file.getParentFile();
        if (fileParent != null) {
            fileParent.mkdirs();
        }
        FileWriter fileWriter = null;
        try {
            try {
                fileWriter = new FileWriter(file);
                fileWriter.write(str);
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                throw e;
            }
        } catch (Throwable th) {
            if (fileWriter != null) {
                fileWriter.close();
            }
            throw th;
        }
    }

    public static String file2String(String fileName) throws IOException {
        return file2String(new File(fileName));
    }

    public static String file2String(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        byte[] data = new byte[(int) file.length()];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            if (inputStream.read(data) == data.length) {
                return new String(data);
            }
            return null;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static String file2String(URL url) {
        InputStream in = null;
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setUseCaches(false);
            in = urlConnection.getInputStream();
            int len = in.available();
            byte[] data = new byte[len];
            in.read(data, 0, len);
            String str = new String(data, "UTF-8");
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            return str;
        } catch (IOException e2) {
            if (null == in) {
                return null;
            }
            try {
                in.close();
                return null;
            } catch (IOException e3) {
                return null;
            }
        } catch (Throwable th) {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public static void printObjectProperties(InternalLogger logger, Object object) {
        printObjectProperties(logger, object, false);
    }

    public static void printObjectProperties(InternalLogger logger, Object object, boolean onlyImportantField) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;
                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                        if (null == value) {
                            value = "";
                        }
                    } catch (IllegalAccessException e) {
                        log.error("Failed to obtain object properties", (Throwable) e);
                    }
                    if ((!onlyImportantField || null != field.getAnnotation(ImportantField.class)) && logger != null) {
                        logger.info(name + "=" + value);
                    }
                }
            }
        }
    }

    public static String properties2String(Properties properties) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(entry.getKey().toString() + "=" + entry.getValue().toString() + "\n");
            }
        }
        return sb.toString();
    }

    public static Properties string2Properties(String str) {
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(str.getBytes("UTF-8")));
            return properties;
        } catch (Exception e) {
            log.error("Failed to handle properties", (Throwable) e);
            return null;
        }
    }

    public static Properties object2Properties(Object object) {
        Properties properties = new Properties();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;
                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                    } catch (IllegalAccessException e) {
                        log.error("Failed to handle properties", (Throwable) e);
                    }
                    if (value != null) {
                        properties.setProperty(name, value.toString());
                    }
                }
            }
        }
        return properties;
    }

    public static void properties2Object(Properties p, Object object) {
        Class<?>[] pt;
        Object arg = null;
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith(BeanDefinitionParserDelegate.SET_ELEMENT)) {
                try {
                    String property = p.getProperty(mn.substring(3, 4).toLowerCase() + mn.substring(4));
                    if (!(property == null || (pt = method.getParameterTypes()) == null || pt.length <= 0)) {
                        String cn = pt[0].getSimpleName();
                        if (cn.equals("int") || cn.equals("Integer")) {
                            arg = Integer.valueOf(Integer.parseInt(property));
                        } else if (cn.equals("long") || cn.equals("Long")) {
                            arg = Long.valueOf(Long.parseLong(property));
                        } else if (cn.equals("double") || cn.equals("Double")) {
                            arg = Double.valueOf(Double.parseDouble(property));
                        } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                            arg = Boolean.valueOf(Boolean.parseBoolean(property));
                        } else if (cn.equals("float") || cn.equals("Float")) {
                            arg = Float.valueOf(Float.parseFloat(property));
                        } else if (cn.equals("String")) {
                            arg = property;
                        }
                        method.invoke(object, arg);
                    }
                } catch (Throwable th) {
                }
            }
        }
    }

    public static boolean isPropertiesEqual(Properties p1, Properties p2) {
        return p1.equals(p2);
    }

    public static List<String> getLocalInetAddress() {
        List<String> inetAddressList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                Enumeration<InetAddress> addrs = enumeration.nextElement().getInetAddresses();
                while (addrs.hasMoreElements()) {
                    inetAddressList.add(addrs.nextElement().getHostAddress());
                }
            }
            return inetAddressList;
        } catch (SocketException e) {
            throw new RuntimeException("get local inet address fail", e);
        }
    }

    private static String localhost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Throwable e) {
            try {
                String candidatesHost = getLocalhostByNetworkInterface();
                if (candidatesHost != null) {
                    return candidatesHost;
                }
            } catch (Exception e2) {
            }
            throw new RuntimeException("InetAddress java.net.InetAddress.getLocalHost() throws UnknownHostException" + FAQUrl.suggestTodo("http://rocketmq.apache.org/docs/faq/"), e);
        }
    }

    public static String getLocalhostByNetworkInterface() throws SocketException {
        List<String> candidatesHost = new ArrayList<>();
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (!"docker0".equals(networkInterface.getName()) && networkInterface.isUp()) {
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress address = addrs.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (!(address instanceof Inet6Address)) {
                            return address.getHostAddress();
                        }
                        candidatesHost.add(address.getHostAddress());
                    }
                }
                continue;
            }
        }
        if (!candidatesHost.isEmpty()) {
            return candidatesHost.get(0);
        }
        return null;
    }

    public static boolean compareAndIncreaseOnly(AtomicLong target, long value) {
        long prev = target.get();
        while (value > prev) {
            if (target.compareAndSet(prev, value)) {
                return true;
            }
            prev = target.get();
        }
        return false;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < ((long) unit)) {
            return bytes + " B";
        }
        int exp = (int) (Math.log((double) bytes) / Math.log((double) unit));
        return String.format("%.1f %sB", Double.valueOf(((double) bytes) / Math.pow((double) unit, (double) exp)), (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i"));
    }
}
