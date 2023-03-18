package org.apache.rocketmq.sdk.shade.common.acl;

import org.apache.rocketmq.sdk.shade.remoting.protocol.RemotingCommand;
import java.util.Map;
import java.util.SortedMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;

public class AclUtils {
    public static byte[] combineRequestContent(RemotingCommand request, SortedMap<String, String> fieldsMap) {
        try {
            StringBuilder sb = new StringBuilder("");
            for (Map.Entry<String, String> entry : fieldsMap.entrySet()) {
                if (!SessionCredentials.SIGNATURE.equals(entry.getKey())) {
                    sb.append(entry.getValue());
                }
            }
            return combineBytes(sb.toString().getBytes(SessionCredentials.CHARSET), request.getBody());
        } catch (Exception e) {
            throw new RuntimeException("incompatible exception.", e);
        }
    }

    public static byte[] combineBytes(byte[] b1, byte[] b2) {
        byte[] total = new byte[(null != b1 ? b1.length : 0) + (null != b2 ? b2.length : 0)];
        if (null != b1) {
            System.arraycopy(b1, 0, total, 0, b1.length);
        }
        if (null != b2) {
            System.arraycopy(b2, 0, total, null != b1 ? b1.length : 0, b2.length);
        }
        return total;
    }

    public static String calSignature(byte[] data, String secretKey) {
        return AclSigner.calSignature(data, secretKey);
    }

    public static void verify(String netaddress, int index) {
        if (!isScope(netaddress, index)) {
            throw new AclException(String.format("netaddress examine scope Exception netaddress is %s", netaddress));
        }
    }

    public static String[] getAddreeStrArray(String netaddress, String four) {
        String[] fourStrArray = StringUtils.split(four.substring(1, four.length() - 1), StringArrayPropertyEditor.DEFAULT_SEPARATOR);
        String address = netaddress.substring(0, netaddress.indexOf("{"));
        String[] addreeStrArray = new String[fourStrArray.length];
        for (int i = 0; i < fourStrArray.length; i++) {
            addreeStrArray[i] = address + fourStrArray[i];
        }
        return addreeStrArray;
    }

    public static boolean isScope(String num, int index) {
        String[] strArray = StringUtils.split(num, ".");
        if (strArray.length != 4) {
            return false;
        }
        return isScope(strArray, index);
    }

    public static boolean isScope(String[] num, int index) {
        if (num.length <= index) {
        }
        for (int i = 0; i < index; i++) {
            if (!isScope(num[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean isScope(String num) {
        return isScope(Integer.valueOf(num.trim()).intValue());
    }

    public static boolean isScope(int num) {
        return num >= 0 && num <= 255;
    }

    public static boolean isAsterisk(String asterisk) {
        return asterisk.indexOf(42) > -1;
    }

    public static boolean isColon(String colon) {
        return colon.indexOf(44) > -1;
    }

    public static boolean isMinus(String minus) {
        return minus.indexOf(45) > -1;
    }
}
