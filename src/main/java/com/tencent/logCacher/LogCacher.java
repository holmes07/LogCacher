package com.tencent.logCacher;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by kennyjiang on 2017/5/19. update 5
 */
public class LogCacher {
    private static final Logger LOG = LoggerFactory.getLogger(LogCacher.class);

    private static RemovalListener<String, LogEntry> removalListener = new RemovalListener<String, LogEntry>() {

        public void onRemoval(RemovalNotification<String, LogEntry> removal) {
            LOG.info(removal.toString());
        }

    };
    Cache<String, LogEntry> cache;

    public LogCacher(long timeoutInterval, int maxCacheKeySize) {
        cache = CacheBuilder.newBuilder()
                .maximumSize(maxCacheKeySize)
                .initialCapacity(maxCacheKeySize)
                .concurrencyLevel(10)
                .expireAfterWrite(timeoutInterval, TimeUnit.MILLISECONDS)
                .removalListener(removalListener).build();
    }

    /**
     * 该方法根据key收敛日志，未满足收敛次数triggerCount之前，当前日志不会输出（时间周期条件满足的情况例外）
     * 相同key对应的value值必须是相同的，因为一次收敛只会取第一次记录的value值，因此同一个key如果在一次收敛的过程中出现了多个value，除了第一个value外的值均会被丢弃
     * @param key
     * @param value
     * @param triggerCount  收敛次数
     */
    public void printIfConditionMeet(String key, String value, long triggerCount) {
        try {
            LogEntry logEntry = cache.getIfPresent(key);
            if (null == logEntry) {
                logEntry = new LogEntry(key, value, triggerCount);
                LogEntry originalLogEntry = cache.asMap().putIfAbsent(key, logEntry);
                if (originalLogEntry != null) {
                    logEntry = originalLogEntry;
                }
            }
            if (logEntry.getCurrentCount().incrementAndGet() >= triggerCount) {
                cache.invalidate(key);
            }
        } catch (Exception e) {
            LOG.info("invoke printIfConditionMeet method error", e);
        }
    }

    public void printEntireCacheNow(){
        cache.invalidateAll();
    }

    public void cleanUp(){
        cache.cleanUp();
    }

}
