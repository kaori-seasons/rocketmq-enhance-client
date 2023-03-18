package org.apache.rocketmq.sdk.shade.common.sysflag;

public class PullSysFlag {
    private static final int FLAG_COMMIT_OFFSET = 1;
    private static final int FLAG_SUSPEND = 2;
    private static final int FLAG_SUBSCRIPTION = 4;
    private static final int FLAG_CLASS_FILTER = 8;

    public static int buildSysFlag(boolean commitOffset, boolean suspend, boolean subscription, boolean classFilter) {
        int flag = 0;
        if (commitOffset) {
            flag = 0 | 1;
        }
        if (suspend) {
            flag |= 2;
        }
        if (subscription) {
            flag |= 4;
        }
        if (classFilter) {
            flag |= 8;
        }
        return flag;
    }

    public static int clearCommitOffsetFlag(int sysFlag) {
        return sysFlag & -2;
    }

    public static boolean hasCommitOffsetFlag(int sysFlag) {
        return (sysFlag & 1) == 1;
    }

    public static boolean hasSuspendFlag(int sysFlag) {
        return (sysFlag & 2) == 2;
    }

    public static boolean hasSubscriptionFlag(int sysFlag) {
        return (sysFlag & 4) == 4;
    }

    public static boolean hasClassFilterFlag(int sysFlag) {
        return (sysFlag & 8) == 8;
    }
}
