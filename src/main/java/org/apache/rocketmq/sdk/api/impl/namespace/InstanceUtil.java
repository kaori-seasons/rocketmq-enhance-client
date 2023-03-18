package org.apache.rocketmq.sdk.api.impl.namespace;

import java.util.regex.Pattern;

public class InstanceUtil {
    public static final String SEPARATOR_V1 = "_";
    public static final String REGEX_INSTANCE_V1 = "MQ_INST_\\w+_\\w{8}";
    public static final String SEPARATOR_V2 = "-";
    public static final String REGEX_INSTANCE_V2 = "mqi-\\w+-\\w+";
    public static final Pattern PATTERN_INSTANCE_V1 = Pattern.compile("^MQ_INST_\\w+_\\w{8}");
    public static final Pattern PATTERN_INSTANCE_V2 = Pattern.compile("^mqi-\\w+-\\w+");

    public static boolean validateInstanceId(String instanceId) {
        return instanceIdVersion(instanceId) > 0;
    }

    private static int instanceIdVersion(String instanceId) {
        if (PATTERN_INSTANCE_V1.matcher(instanceId).matches()) {
            return 1;
        }
        if (PATTERN_INSTANCE_V2.matcher(instanceId).matches()) {
            return 2;
        }
        return 0;
    }

    public static boolean isIndependentNaming(String instanceId) {
        String[] arr;
        return instanceIdVersion(instanceId) != 2 || (arr = instanceId.split("-")) == null || arr.length < 3 || arr[2] == null || arr[2].length() <= 3 || (arr[2].charAt(3) & 1) == 1;
    }
}
