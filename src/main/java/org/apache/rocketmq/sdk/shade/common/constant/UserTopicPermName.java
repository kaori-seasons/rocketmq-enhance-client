package org.apache.rocketmq.sdk.shade.common.constant;

public class UserTopicPermName {
    public static final int USER_ARREARAGE = 0;
    public static final int USER_NORMAL = 1;
    public static final int PERM_PUBLISH = 2;
    public static final int PERM_SUBSCRIBE = 4;

    public static boolean isSubscribable(int perm) {
        return (perm & 4) == 4;
    }

    public static boolean isPublishable(int perm) {
        return (perm & 2) == 2;
    }

    public static boolean isUserStatusOK(int perm) {
        return (perm & 1) == 1;
    }

    public static int calPerm(int userStatus, int topicUserPerm) {
        return topicUserPerm | userStatus;
    }
}
