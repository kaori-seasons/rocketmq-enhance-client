package org.apache.rocketmq.sdk.shade.common.sysflag;

public class MessageSysFlag {
    public static final int COMPRESSED_FLAG = 1;
    public static final int MULTI_TAGS_FLAG = 2;
    public static final int TRANSACTION_NOT_TYPE = 0;
    public static final int TRANSACTION_PREPARED_TYPE = 4;
    public static final int TRANSACTION_COMMIT_TYPE = 8;
    public static final int TRANSACTION_ROLLBACK_TYPE = 12;
    public static final int BORNHOST_V6_FLAG = 16;
    public static final int STOREHOSTADDRESS_V6_FLAG = 32;

    public static int getTransactionValue(int flag) {
        return flag & 12;
    }

    public static int resetTransactionValue(int flag, int type) {
        return (flag & -13) | type;
    }

    public static int clearCompressedFlag(int flag) {
        return flag & -2;
    }
}
