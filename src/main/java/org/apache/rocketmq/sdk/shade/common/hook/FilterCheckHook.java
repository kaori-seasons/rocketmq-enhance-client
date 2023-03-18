package org.apache.rocketmq.sdk.shade.common.hook;

import java.nio.ByteBuffer;

public interface FilterCheckHook {
    String hookName();

    boolean isFilterMatched(boolean z, ByteBuffer byteBuffer);
}
