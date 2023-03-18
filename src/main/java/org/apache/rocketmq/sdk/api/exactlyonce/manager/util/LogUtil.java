package org.apache.rocketmq.sdk.api.exactlyonce.manager.util;

import org.apache.rocketmq.sdk.api.exactlyonce.TxConstant;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;

public class LogUtil {
    public static void debug(InternalLogger logger, String var1) {
        logger.debug(TxConstant.EXACTLYONCELOG_PREFIX + var1);
    }

    public static void debug(InternalLogger logger, String var1, Object var2) {
        logger.debug(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void debug(InternalLogger logger, String var1, Object var2, Object var3) {
        logger.debug(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2, var3);
    }

    public static void debug(InternalLogger logger, String var1, Object... var2) {
        logger.debug(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void debug(InternalLogger logger, String var1, Throwable var2) {
        logger.debug(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void info(InternalLogger logger, String var1) {
        logger.info(TxConstant.EXACTLYONCELOG_PREFIX + var1, var1);
    }

    public static void info(InternalLogger logger, String var1, Object var2) {
        logger.info(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void info(InternalLogger logger, String var1, Object var2, Object var3) {
        logger.info(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2, var3);
    }

    public static void info(InternalLogger logger, String var1, Object... var2) {
        logger.info(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void info(InternalLogger logger, String var1, Throwable var2) {
        logger.info(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void warn(InternalLogger logger, String var1) {
        logger.warn(TxConstant.EXACTLYONCELOG_PREFIX + var1);
    }

    public static void warn(InternalLogger logger, String var1, Object var2) {
        logger.warn(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void warn(InternalLogger logger, String var1, Object... var2) {
        logger.warn(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void warn(InternalLogger logger, String var1, Object var2, Object var3) {
        logger.warn(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2, var3);
    }

    public static void warn(InternalLogger logger, String var1, Throwable var2) {
        logger.warn(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void error(InternalLogger logger, String var1) {
        logger.error(TxConstant.EXACTLYONCELOG_PREFIX + var1);
    }

    public static void error(InternalLogger logger, String var1, Object var2) {
        logger.error(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void error(InternalLogger logger, String var1, Object var2, Object var3) {
        logger.error(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2, var3);
    }

    public static void error(InternalLogger logger, String var1, Object... var2) {
        logger.error(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }

    public static void error(InternalLogger logger, String var1, Throwable var2) {
        logger.error(TxConstant.EXACTLYONCELOG_PREFIX + var1, var2);
    }
}
