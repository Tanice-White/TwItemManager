package io.github.tanice.twItemManager.manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

public abstract class AsyncManager implements IAsyncManager {
    /** 唯一的异步线程池 */
    protected final ExecutorService asyncExecutor;

    public AsyncManager() {
        int coreThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        int maxThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        asyncExecutor = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃旧任务保留新任务
        );
    }

    public void onReload() {

    }

    public void onDisable() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logWarning("异步线程关闭失败, 即将强制关闭: " + e.getMessage());
                asyncExecutor.shutdownNow();
            }
        }
    }

    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) asyncExecutor;
        return String.format("活跃线程: %d, 核心线程: %d, 最大线程: %d, 队列大小: %d",
                tpe.getActiveCount(), tpe.getCorePoolSize(), tpe.getMaximumPoolSize(), tpe.getQueue().size());
    }
}
