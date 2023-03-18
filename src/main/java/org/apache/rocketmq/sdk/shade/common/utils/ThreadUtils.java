package org.apache.rocketmq.sdk.shade.common.utils;

import org.apache.rocketmq.sdk.shade.common.constant.LoggerName;
import org.apache.rocketmq.sdk.shade.logging.InternalLogger;
import org.apache.rocketmq.sdk.shade.logging.InternalLoggerFactory;

import java.lang.Thread;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadUtils {
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.TOOLS_LOGGER_NAME);

    public static ExecutorService newThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, String processName, boolean isDaemon) {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, newThreadFactory(processName, isDaemon));
    }

    public static ExecutorService newSingleThreadExecutor(String processName, boolean isDaemon) {
        return Executors.newSingleThreadExecutor(newThreadFactory(processName, isDaemon));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String processName, boolean isDaemon) {
        return Executors.newSingleThreadScheduledExecutor(newThreadFactory(processName, isDaemon));
    }

    public static ScheduledExecutorService newFixedThreadScheduledPool(int nThreads, String processName, boolean isDaemon) {
        return Executors.newScheduledThreadPool(nThreads, newThreadFactory(processName, isDaemon));
    }

    public static ThreadFactory newThreadFactory(String processName, boolean isDaemon) {
        return newGenericThreadFactory("Remoting-" + processName, isDaemon);
    }

    public static ThreadFactory newGenericThreadFactory(String processName) {
        return newGenericThreadFactory(processName, false);
    }

    public static ThreadFactory newGenericThreadFactory(String processName, int threads) {
        return newGenericThreadFactory(processName, threads, false);
    }

    public static ThreadFactory newGenericThreadFactory(final String processName, final boolean isDaemon) {
        return new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, String.format("%s_%d", processName, Integer.valueOf(this.threadIndex.incrementAndGet())));
                thread.setDaemon(isDaemon);
                return thread;
            }
        };
    }

    public static ThreadFactory newGenericThreadFactory(final String processName, final int threads, final boolean isDaemon) {
        return new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, String.format("%s_%d_%d", processName, Integer.valueOf(threads), Integer.valueOf(this.threadIndex.incrementAndGet())));
                thread.setDaemon(isDaemon);
                return thread;
            }
        };
    }

    public static Thread newThread(String name, Runnable runnable, boolean daemon) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(daemon);
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                ThreadUtils.log.error("Uncaught exception in thread '" + t.getName() + "':", e);
            }
        });
        return thread;
    }

    public static void shutdownGracefully(Thread t) {
        shutdownGracefully(t, 0);
    }

    public static void shutdownGracefully(Thread t, long millis) {
        if (t != null) {
            while (t.isAlive()) {
                try {
                    t.interrupt();
                    t.join(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static void shutdownGracefully(ExecutorService executor, long timeout, TimeUnit timeUnit) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, timeUnit)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(timeout, timeUnit)) {
                    log.warn(String.format("%s didn't terminate!", executor));
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ThreadUtils() {
    }
}
