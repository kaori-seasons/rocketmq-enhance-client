package org.apache.rocketmq.sdk.shade.common.sysflag;

public class TopicSysFlag {
    private static final int FLAG_UNIT = 1;
    private static final int FLAG_UNIT_SUB = 2;

    public static int buildSysFlag(boolean unit, boolean hasUnitSub) {
        int sysFlag = 0;
        if (unit) {
            sysFlag = 0 | 1;
        }
        if (hasUnitSub) {
            sysFlag |= 2;
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

    public static int setUnitSubFlag(int sysFlag) {
        return sysFlag | 2;
    }

    public static int clearUnitSubFlag(int sysFlag) {
        return sysFlag & -3;
    }

    public static boolean hasUnitSubFlag(int sysFlag) {
        return (sysFlag & 2) == 2;
    }

    public static void main(String[] args) {
    }
}
