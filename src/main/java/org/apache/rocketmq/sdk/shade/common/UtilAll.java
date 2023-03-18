package org.apache.rocketmq.sdk.shade.common;

import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;
import org.apache.rocketmq.sdk.shade.remoting.common.RemotingHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.springframework.jdbc.datasource.init.ScriptUtils;

public class UtilAll {
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd#HH:mm:ss:SSS";
    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.COMMON_LOGGER_NAME);
    static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf(64)));
        } catch (Exception e) {
            return -1;
        }
    }

    public static String currentStackTrace() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            sb.append("\n\t");
            sb.append(ste.toString());
        }
        return sb.toString();
    }

    public static String offset2FileName(long offset) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(20);
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(false);
        return nf.format(offset);
    }

    public static long computeEclipseTimeMilliseconds(long beginTime) {
        return System.currentTimeMillis() - beginTime;
    }

    public static boolean isItTimeToDo(String when) {
        String[] whiles = when.split(ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
        if (whiles.length <= 0) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        for (String w : whiles) {
            if (Integer.parseInt(w) == now.get(11)) {
                return true;
            }
        }
        return false;
    }

    public static String timeMillisToHumanString() {
        return timeMillisToHumanString(System.currentTimeMillis());
    }

    public static String timeMillisToHumanString(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return String.format("%04d%02d%02d%02d%02d%02d%03d", Integer.valueOf(cal.get(1)), Integer.valueOf(cal.get(2) + 1), Integer.valueOf(cal.get(5)), Integer.valueOf(cal.get(11)), Integer.valueOf(cal.get(12)), Integer.valueOf(cal.get(13)), Integer.valueOf(cal.get(14)));
    }

    public static long computNextMorningTimeMillis() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(5, 1);
        cal.set(11, 0);
        cal.set(12, 0);
        cal.set(13, 0);
        cal.set(14, 0);
        return cal.getTimeInMillis();
    }

    public static long computNextMinutesTimeMillis() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(5, 0);
        cal.add(11, 0);
        cal.add(12, 1);
        cal.set(13, 0);
        cal.set(14, 0);
        return cal.getTimeInMillis();
    }

    public static long computNextHourTimeMillis() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(5, 0);
        cal.add(11, 1);
        cal.set(12, 0);
        cal.set(13, 0);
        cal.set(14, 0);
        return cal.getTimeInMillis();
    }

    public static long computNextHalfHourTimeMillis() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(5, 0);
        cal.add(11, 1);
        cal.set(12, 30);
        cal.set(13, 0);
        cal.set(14, 0);
        return cal.getTimeInMillis();
    }

    public static String timeMillisToHumanString2(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return String.format("%04d-%02d-%02d %02d:%02d:%02d,%03d", Integer.valueOf(cal.get(1)), Integer.valueOf(cal.get(2) + 1), Integer.valueOf(cal.get(5)), Integer.valueOf(cal.get(11)), Integer.valueOf(cal.get(12)), Integer.valueOf(cal.get(13)), Integer.valueOf(cal.get(14)));
    }

    public static String timeMillisToHumanString3(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return String.format("%04d%02d%02d%02d%02d%02d", Integer.valueOf(cal.get(1)), Integer.valueOf(cal.get(2) + 1), Integer.valueOf(cal.get(5)), Integer.valueOf(cal.get(11)), Integer.valueOf(cal.get(12)), Integer.valueOf(cal.get(13)));
    }

    public static double getDiskPartitionSpaceUsedPercent(String path) {
        if (null == path || path.isEmpty()) {
            return -1.0d;
        }
        try {
            File file = new File(path);
            if (!file.exists()) {
                return -1.0d;
            }
            long totalSpace = file.getTotalSpace();
            if (totalSpace > 0) {
                return ((double) (totalSpace - file.getFreeSpace())) / ((double) totalSpace);
            }
            return -1.0d;
        } catch (Exception e) {
            return -1.0d;
        }
    }

    public static int crc32(byte[] array) {
        if (array != null) {
            return crc32(array, 0, array.length);
        }
        return 0;
    }

    public static int crc32(byte[] array, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(array, offset, length);
        return (int) (crc32.getValue() & 2147483647L);
    }

    public static String bytes2string(byte[] src) {
        char[] hexChars = new char[src.length * 2];
        for (int j = 0; j < src.length; j++) {
            int v = src[j] & 255;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[(j * 2) + 1] = HEX_ARRAY[v & 15];
        }
        return new String(hexChars);
    }

    public static byte[] string2bytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        String hexString2 = hexString.toUpperCase();
        int length = hexString2.length() / 2;
        char[] hexChars = hexString2.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) ((charToByte(hexChars[pos]) << 4) | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static byte[] uncompress(byte[] src) throws IOException {
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src);
        InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(src.length);
        while (true) {
            try {
                try {
                    int len = inflaterInputStream.read(uncompressData, 0, uncompressData.length);
                    if (len <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(uncompressData, 0, len);
                } catch (IOException e) {
                    throw e;
                }
            } finally {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e2) {
                    log.error("Failed to close the stream", (Throwable) e2);
                }
                try {
                    inflaterInputStream.close();
                } catch (IOException e3) {
                    log.error("Failed to close the stream", (Throwable) e3);
                }
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e4) {
                    log.error("Failed to close the stream", (Throwable) e4);
                }
            }
        }
        byteArrayOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] compress(byte[] src, int level) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = null;
        Deflater defeater = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream(src.length);
            defeater = new Deflater(level);
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, defeater);
            try {
                deflaterOutputStream.write(src);
                deflaterOutputStream.finish();
                deflaterOutputStream.close();
                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                defeater.end();
                throw e;
            }
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException e2) {
            }
            defeater.end();
        }
    }

    public static int asInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long asLong(String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String formatDate(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

    public static Date parseDate(String date, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String responseCode2String(int code) {
        return Integer.toString(code);
    }

    public static String frontStringAtLeast(String str, int size) {
        if (str == null || str.length() <= size) {
            return str;
        }
        return str.substring(0, size);
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

    public static String jstack() {
        return jstack(Thread.getAllStackTraces());
    }

    public static String jstack(Map<Thread, StackTraceElement[]> map) {
        StringBuilder result = new StringBuilder();
        try {
            for (Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
                StackTraceElement[] elements = entry.getValue();
                Thread thread = entry.getKey();
                if (elements != null && elements.length > 0) {
                    String threadName = entry.getKey().getName();
                    result.append(String.format("%-40sTID: %d STATE: %s%n", threadName, Long.valueOf(thread.getId()), thread.getState()));
                    for (StackTraceElement el : elements) {
                        result.append(String.format("%-40s%s%n", threadName, el.toString()));
                    }
                    result.append("\n");
                }
            }
        } catch (Throwable e) {
            result.append(RemotingHelper.exceptionSimpleDesc(e));
        }
        return result.toString();
    }

    public static boolean isInternalIP(byte[] ip) {
        if (ip.length != 4) {
            throw new RuntimeException("illegal ipv4 bytes");
        } else if (ip[0] == 10) {
            return true;
        } else {
            if (ip[0] == -84) {
                if (ip[1] < 16 || ip[1] > 31) {
                    return false;
                }
                return true;
            } else if (ip[0] == -64 && ip[1] == -88) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static boolean ipCheck(byte[] ip) {
        if (ip.length != 4) {
            throw new RuntimeException("illegal ipv4 bytes");
        } else if (ip[0] < 1 || ip[0] > 126) {
            if (ip[0] < Byte.MIN_VALUE || ip[0] > -65) {
                if (ip[0] < -64 || ip[0] > -33 || ip[3] == 1 || ip[3] == 0) {
                    return false;
                }
                return true;
            } else if (ip[2] == 1 && ip[3] == 1) {
                return false;
            } else {
                if (ip[2] == 0 && ip[3] == 0) {
                    return false;
                }
                return true;
            }
        } else if (ip[1] == 1 && ip[2] == 1 && ip[3] == 1) {
            return false;
        } else {
            if (ip[1] == 0 && ip[2] == 0 && ip[3] == 0) {
                return false;
            }
            return true;
        }
    }

    public static String ipToIPv4Str(byte[] ip) {
        if (ip.length != 4) {
            return null;
        }
        return (ip[0] & 255) + "." + (ip[1] & 255) + "." + (ip[2] & 255) + "." + (ip[3] & 255);
    }

    public static byte[] getIP() {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            byte[] internalIP = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null && (ip instanceof Inet4Address)) {
                        byte[] ipByte = ip.getAddress();
                        if (ipByte.length == 4 && ipCheck(ipByte)) {
                            if (!isInternalIP(ipByte)) {
                                return ipByte;
                            }
                            if (internalIP == null) {
                                internalIP = ipByte;
                            }
                        }
                    }
                }
            }
            if (internalIP != null) {
                return internalIP;
            }
            throw new RuntimeException("Can not get local ip");
        } catch (Exception e) {
            throw new RuntimeException("Can not get local ip", e);
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                for (File file1 : file.listFiles()) {
                    deleteFile(file1);
                }
                file.delete();
            }
        }
    }
}
