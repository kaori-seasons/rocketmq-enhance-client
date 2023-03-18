package org.apache.rocketmq.sdk.shade.common.protocol.body;

import org.apache.rocketmq.sdk.shade.common.UtilAll;
import org.springframework.beans.PropertyAccessor;

public class ProcessQueueInfo {
    private long commitOffset;
    private long cachedMsgMinOffset;
    private long cachedMsgMaxOffset;
    private int cachedMsgCount;
    private int cachedMsgSizeInMiB;
    private long transactionMsgMinOffset;
    private long transactionMsgMaxOffset;
    private int transactionMsgCount;
    private boolean locked;
    private long tryUnlockTimes;
    private long lastLockTimestamp;
    private boolean droped;
    private long lastPullTimestamp;
    private long lastConsumeTimestamp;

    public long getCommitOffset() {
        return this.commitOffset;
    }

    public void setCommitOffset(long commitOffset) {
        this.commitOffset = commitOffset;
    }

    public long getCachedMsgMinOffset() {
        return this.cachedMsgMinOffset;
    }

    public void setCachedMsgMinOffset(long cachedMsgMinOffset) {
        this.cachedMsgMinOffset = cachedMsgMinOffset;
    }

    public long getCachedMsgMaxOffset() {
        return this.cachedMsgMaxOffset;
    }

    public void setCachedMsgMaxOffset(long cachedMsgMaxOffset) {
        this.cachedMsgMaxOffset = cachedMsgMaxOffset;
    }

    public int getCachedMsgCount() {
        return this.cachedMsgCount;
    }

    public void setCachedMsgCount(int cachedMsgCount) {
        this.cachedMsgCount = cachedMsgCount;
    }

    public long getTransactionMsgMinOffset() {
        return this.transactionMsgMinOffset;
    }

    public void setTransactionMsgMinOffset(long transactionMsgMinOffset) {
        this.transactionMsgMinOffset = transactionMsgMinOffset;
    }

    public long getTransactionMsgMaxOffset() {
        return this.transactionMsgMaxOffset;
    }

    public void setTransactionMsgMaxOffset(long transactionMsgMaxOffset) {
        this.transactionMsgMaxOffset = transactionMsgMaxOffset;
    }

    public int getTransactionMsgCount() {
        return this.transactionMsgCount;
    }

    public void setTransactionMsgCount(int transactionMsgCount) {
        this.transactionMsgCount = transactionMsgCount;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getTryUnlockTimes() {
        return this.tryUnlockTimes;
    }

    public void setTryUnlockTimes(long tryUnlockTimes) {
        this.tryUnlockTimes = tryUnlockTimes;
    }

    public long getLastLockTimestamp() {
        return this.lastLockTimestamp;
    }

    public void setLastLockTimestamp(long lastLockTimestamp) {
        this.lastLockTimestamp = lastLockTimestamp;
    }

    public boolean isDroped() {
        return this.droped;
    }

    public void setDroped(boolean droped) {
        this.droped = droped;
    }

    public long getLastPullTimestamp() {
        return this.lastPullTimestamp;
    }

    public void setLastPullTimestamp(long lastPullTimestamp) {
        this.lastPullTimestamp = lastPullTimestamp;
    }

    public long getLastConsumeTimestamp() {
        return this.lastConsumeTimestamp;
    }

    public void setLastConsumeTimestamp(long lastConsumeTimestamp) {
        this.lastConsumeTimestamp = lastConsumeTimestamp;
    }

    public int getCachedMsgSizeInMiB() {
        return this.cachedMsgSizeInMiB;
    }

    public void setCachedMsgSizeInMiB(int cachedMsgSizeInMiB) {
        this.cachedMsgSizeInMiB = cachedMsgSizeInMiB;
    }

    public String toString() {
        return "ProcessQueueInfo [commitOffset=" + this.commitOffset + ", cachedMsgMinOffset=" + this.cachedMsgMinOffset + ", cachedMsgMaxOffset=" + this.cachedMsgMaxOffset + ", cachedMsgCount=" + this.cachedMsgCount + ", cachedMsgSizeInMiB=" + this.cachedMsgSizeInMiB + ", transactionMsgMinOffset=" + this.transactionMsgMinOffset + ", transactionMsgMaxOffset=" + this.transactionMsgMaxOffset + ", transactionMsgCount=" + this.transactionMsgCount + ", locked=" + this.locked + ", tryUnlockTimes=" + this.tryUnlockTimes + ", lastLockTimestamp=" + UtilAll.timeMillisToHumanString(this.lastLockTimestamp) + ", droped=" + this.droped + ", lastPullTimestamp=" + UtilAll.timeMillisToHumanString(this.lastPullTimestamp) + ", lastConsumeTimestamp=" + UtilAll.timeMillisToHumanString(this.lastConsumeTimestamp) + PropertyAccessor.PROPERTY_KEY_SUFFIX;
    }
}
