package org.apache.rocketmq.sdk.shade.logging;

import org.apache.rocketmq.sdk.shade.logging.inner.Logger;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;

public class InnerLoggerFactory extends InternalLoggerFactory {
    public InnerLoggerFactory() {
        doRegister();
    }

    @Override
    protected InternalLogger getLoggerInstance(String name) {
        return new InnerLogger(name);
    }

    @Override
    protected String getLoggerType() {
        return InternalLoggerFactory.LOGGER_INNER;
    }

    @Override
    protected void shutdown() {
        Logger.getRepository().shutdown();
    }

    public static class InnerLogger implements InternalLogger {
        private Logger logger;

        public InnerLogger(String name) {
            this.logger = Logger.getLogger(name);
        }

        @Override
        public String getName() {
            return this.logger.getName();
        }

        @Override
        public void debug(String var1) {
            this.logger.debug(var1);
        }

        @Override
        public void debug(String var1, Throwable var2) {
            this.logger.debug(var1, var2);
        }

        @Override
        public void info(String var1) {
            this.logger.info(var1);
        }

        @Override
        public void info(String var1, Throwable var2) {
            this.logger.info(var1, var2);
        }

        @Override
        public void warn(String var1) {
            this.logger.warn(var1);
        }

        @Override
        public void warn(String var1, Throwable var2) {
            this.logger.warn(var1, var2);
        }

        @Override
        public void error(String var1) {
            this.logger.error(var1);
        }

        @Override
        public void error(String var1, Throwable var2) {
            this.logger.error(var1, var2);
        }

        @Override
        public void debug(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            this.logger.debug(format.getMessage(), format.getThrowable());
        }

        @Override
        public void debug(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            this.logger.debug(format.getMessage(), format.getThrowable());
        }

        @Override
        public void debug(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            this.logger.debug(format.getMessage(), format.getThrowable());
        }

        @Override
        public void info(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            this.logger.info(format.getMessage(), format.getThrowable());
        }

        @Override
        public void info(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            this.logger.info(format.getMessage(), format.getThrowable());
        }

        @Override
        public void info(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            this.logger.info(format.getMessage(), format.getThrowable());
        }

        @Override
        public void warn(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            this.logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void warn(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            this.logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void warn(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            this.logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void error(String var1, Object var2) {
            FormattingTuple format = MessageFormatter.format(var1, var2);
            this.logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void error(String var1, Object var2, Object var3) {
            FormattingTuple format = MessageFormatter.format(var1, var2, var3);
            this.logger.warn(format.getMessage(), format.getThrowable());
        }

        @Override
        public void error(String var1, Object... var2) {
            FormattingTuple format = MessageFormatter.arrayFormat(var1, var2);
            this.logger.warn(format.getMessage(), format.getThrowable());
        }

        public Logger getLogger() {
            return this.logger;
        }
    }

    public static class FormattingTuple {
        private String message;
        private Throwable throwable;
        private Object[] argArray;

        public FormattingTuple(String message) {
            this(message, null, null);
        }

        public FormattingTuple(String message, Object[] argArray, Throwable throwable) {
            this.message = message;
            this.throwable = throwable;
            if (throwable == null) {
                this.argArray = argArray;
            } else {
                this.argArray = trimmedCopy(argArray);
            }
        }

        static Object[] trimmedCopy(Object[] argArray) {
            if (argArray == null || argArray.length == 0) {
                throw new IllegalStateException("non-sensical empty or null argument array");
            }
            int trimemdLen = argArray.length - 1;
            Object[] trimmed = new Object[trimemdLen];
            System.arraycopy(argArray, 0, trimmed, 0, trimemdLen);
            return trimmed;
        }

        public String getMessage() {
            return this.message;
        }

        public Object[] getArgArray() {
            return this.argArray;
        }

        public Throwable getThrowable() {
            return this.throwable;
        }
    }

    public static class MessageFormatter {
        public static FormattingTuple format(String messagePattern, Object arg) {
            return arrayFormat(messagePattern, new Object[]{arg});
        }

        public static FormattingTuple format(String messagePattern, Object arg1, Object arg2) {
            return arrayFormat(messagePattern, new Object[]{arg1, arg2});
        }

        static Throwable getThrowableCandidate(Object[] argArray) {
            if (argArray == null || argArray.length == 0) {
                return null;
            }
            Object lastEntry = argArray[argArray.length - 1];
            if (lastEntry instanceof Throwable) {
                return (Throwable) lastEntry;
            }
            return null;
        }

        public static FormattingTuple arrayFormat(String messagePattern, Object[] argArray) {
            int i;
            int i2;
            Throwable throwableCandidate = getThrowableCandidate(argArray);
            if (messagePattern == null) {
                return new FormattingTuple(null, argArray, throwableCandidate);
            }
            if (argArray == null) {
                return new FormattingTuple(messagePattern);
            }
            int i3 = 0;
            StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);
            int len = 0;
            while (len < argArray.length) {
                int j = messagePattern.indexOf("{}", i3);
                if (j != -1) {
                    if (!isEscapeDelimeter(messagePattern, j)) {
                        sbuf.append(messagePattern.substring(i3, j));
                        deeplyAppendParameter(sbuf, argArray[len], null);
                        i2 = j;
                        i = 2;
                    } else if (!isDoubleEscaped(messagePattern, j)) {
                        len--;
                        sbuf.append(messagePattern.substring(i3, j - 1));
                        sbuf.append('{');
                        i2 = j;
                        i = 1;
                    } else {
                        sbuf.append(messagePattern.substring(i3, j - 1));
                        deeplyAppendParameter(sbuf, argArray[len], null);
                        i2 = j;
                        i = 2;
                    }
                    i3 = i2 + i;
                    len++;
                } else if (i3 == 0) {
                    return new FormattingTuple(messagePattern, argArray, throwableCandidate);
                } else {
                    sbuf.append(messagePattern.substring(i3, messagePattern.length()));
                    return new FormattingTuple(sbuf.toString(), argArray, throwableCandidate);
                }
            }
            sbuf.append(messagePattern.substring(i3, messagePattern.length()));
            if (len < argArray.length - 1) {
                return new FormattingTuple(sbuf.toString(), argArray, throwableCandidate);
            }
            return new FormattingTuple(sbuf.toString(), argArray, null);
        }

        static boolean isEscapeDelimeter(String messagePattern, int delimeterStartIndex) {
            return delimeterStartIndex != 0 && messagePattern.charAt(delimeterStartIndex - 1) == '\\';
        }

        static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
            return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == '\\';
        }

        private static void deeplyAppendParameter(StringBuilder sbuf, Object o, Map<Object[], Object> seenMap) {
            if (o == null) {
                sbuf.append(BeanDefinitionParserDelegate.NULL_ELEMENT);
            } else if (!o.getClass().isArray()) {
                safeObjectAppend(sbuf, o);
            } else if (o instanceof boolean[]) {
                booleanArrayAppend(sbuf, (boolean[]) o);
            } else if (o instanceof byte[]) {
                byteArrayAppend(sbuf, (byte[]) o);
            } else if (o instanceof char[]) {
                charArrayAppend(sbuf, (char[]) o);
            } else if (o instanceof short[]) {
                shortArrayAppend(sbuf, (short[]) o);
            } else if (o instanceof int[]) {
                intArrayAppend(sbuf, (int[]) o);
            } else if (o instanceof long[]) {
                longArrayAppend(sbuf, (long[]) o);
            } else if (o instanceof float[]) {
                floatArrayAppend(sbuf, (float[]) o);
            } else if (o instanceof double[]) {
                doubleArrayAppend(sbuf, (double[]) o);
            } else {
                objectArrayAppend(sbuf, (Object[]) o, seenMap);
            }
        }

        private static void safeObjectAppend(StringBuilder sbuf, Object o) {
            try {
                sbuf.append(o.toString());
            } catch (Throwable var3) {
                System.err.println("RocketMQ InnerLogger: Failed toString() invocation on an object of type [" + o.getClass().getName() + PropertyAccessor.PROPERTY_KEY_SUFFIX);
                var3.printStackTrace();
                sbuf.append("[FAILED toString()]");
            }
        }

        private static void objectArrayAppend(StringBuilder sbuf, Object[] a, Map<Object[], Object> seenMap) {
            if (seenMap == null) {
                seenMap = new HashMap();
            }
            sbuf.append('[');
            if (!seenMap.containsKey(a)) {
                seenMap.put(a, null);
                int len = a.length;
                for (int i = 0; i < len; i++) {
                    deeplyAppendParameter(sbuf, a[i], seenMap);
                    if (i != len - 1) {
                        sbuf.append(", ");
                    }
                }
                seenMap.remove(a);
            } else {
                sbuf.append("...");
            }
            sbuf.append(']');
        }

        private static void booleanArrayAppend(StringBuilder sbuf, boolean[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void byteArrayAppend(StringBuilder sbuf, byte[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append((int) a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void charArrayAppend(StringBuilder sbuf, char[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void shortArrayAppend(StringBuilder sbuf, short[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append((int) a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void intArrayAppend(StringBuilder sbuf, int[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void longArrayAppend(StringBuilder sbuf, long[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void floatArrayAppend(StringBuilder sbuf, float[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }

        private static void doubleArrayAppend(StringBuilder sbuf, double[] a) {
            sbuf.append('[');
            int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1) {
                    sbuf.append(", ");
                }
            }
            sbuf.append(']');
        }
    }
}
