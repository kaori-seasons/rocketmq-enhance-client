package org.apache.rocketmq.sdk.shade.common.sysflag;

public class SubscriptionSysFlag {
    private static final int FLAG_UNIT = 1;

    public static int buildSysFlag(boolean unit) {
        int sysFlag = 0;
        if (unit) {
            sysFlag = 0 | 1;
        }
        return sysFlag;
    }

    public static int setUnitFlag(int sysFlag) {
        return sysFlag | 1;
    }

    public static int clearUnitFlag(int sysFlag) {
        return sysFlag & -2;
    }

    public static boolean hasUnitFlag(int sysFlag) {
        return (sysFlag & 1) == 1;
    }

    public static void main(String[] args) {
    }
}
