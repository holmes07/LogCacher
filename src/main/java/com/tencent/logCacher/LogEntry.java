package com.tencent.logCacher;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kennyjiang on 2017/5/22.
 */
public class LogEntry {
    private String key;
    private String value;
    private long triggerCount;
    private AtomicLong currentCount = new AtomicLong();

    public LogEntry(String key, String value, long triggerCount) {
        this.key = key;
        this.value = value;
        this.triggerCount = triggerCount;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(long triggerCount) {
        this.triggerCount = triggerCount;
    }

    public AtomicLong getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(AtomicLong currentCount) {
        this.currentCount = currentCount;
    }
    @Override
    public String toString() {
        return "com.tencent.logCacher.LogEntry{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", triggerCount=" + triggerCount +
                ", currentCount=" + currentCount +
                '}';
    }
}
