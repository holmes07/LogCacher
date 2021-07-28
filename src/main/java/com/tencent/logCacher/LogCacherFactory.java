package com.tencent.logCacher;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kennyjiang on 2017/5/22.
 */
public class LogCacherFactory {
    private static Map<Long, LogCacher> logCacherMap = new HashMap<Long, LogCacher>();

    /**
     * 获取指定打印周期的LogCacher
     *
     * @param timeoutInterval 时间周期单位:ms
     * @param maxCacheKeySize 指定LogCacher最大能缓存的条目数，主要用于限制Cacher占用空间大小，如果key数量超出指定大小时，即使logEntry未满足打印条件也会被强制删除打印以回收空间
     * @return
     */
    public static LogCacher getLogCacher(long timeoutInterval, int maxCacheKeySize) {
        if (!logCacherMap.containsKey(timeoutInterval)) {
            synchronized (LogCacherFactory.class) {
                if (!logCacherMap.containsKey(timeoutInterval)) {
                    logCacherMap.put(timeoutInterval, new LogCacher(timeoutInterval, maxCacheKeySize));
                }
            }
        }
        return logCacherMap.get(timeoutInterval);
    }

    /**
     * 进程退出前，调用shutdown可以强制输出所有LogCacher当前未满足打印条件的logEntry
     */
    public static void shutdown() {
        for (LogCacher logCacher : logCacherMap.values()) {
            logCacher.printEntireCacheNow();
        }
    }

    public static void main(String[] args) {
        try {
            LogCacher cacher_interval_5s = LogCacherFactory.getLogCacher(5000, 10000);
            LogCacher cacher_interval_10s = LogCacherFactory.getLogCacher(10000, 10000);
            for (long i = 0; i < 1000; i++) {
                cacher_interval_5s.printIfConditionMeet("" + i, "5s", 10);
                cacher_interval_10s.printIfConditionMeet("" + i, "10s", 10);
            }
            LogCacherFactory.shutdown();
            //add dev branch   rebase 作用。。。推送到不同分支
            // add 1
            // add 2
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
