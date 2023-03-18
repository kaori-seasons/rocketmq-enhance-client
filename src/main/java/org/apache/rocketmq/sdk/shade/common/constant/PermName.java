package org.apache.rocketmq.sdk.shade.common.constant;

public class PermName {
    public static final int PERM_PRIORITY = 8;
    public static final int PERM_READ = 4;
    public static final int PERM_WRITE = 2;
    public static final int PERM_INHERIT = 1;

    public static String perm2String(int perm) {
        StringBuffer sb = new StringBuffer("---");
        if (isReadable(perm)) {
            sb.replace(0, 1, "R");
        }
        if (isWriteable(perm)) {
            sb.replace(1, 2, "W");
        }
        if (isInherited(perm)) {
            sb.replace(2, 3, "X");
        }
        return sb.toString();
    }

    public static boolean isReadable(int perm) {
        return (perm & 4) == 4;
    }

    public static boolean isWriteable(int perm) {
        return (perm & 2) == 2;
    }

    public static boolean isInherited(int perm) {
        return (perm & 1) == 1;
    }
}
