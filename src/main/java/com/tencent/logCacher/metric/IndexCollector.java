//package com.tencent.logCacher.metric;
//
//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import org.apache.pulsar.common.naming.TopicName;
//import org.joda.time.DateTime;
//import org.joda.time.format.DateTimeFormat;
//import org.joda.time.format.DateTimeFormatter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.text.SimpleDateFormat;
//import java.util.BitSet;
//import java.util.Map;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//import java.util.regex.Pattern;
//
//
//public class IndexCollector {
//
//    private static final Logger LOG = LoggerFactory.getLogger(IndexCollector.class);
//    private static final AtomicReference<ConcurrentHashMap<String/**type#topic#partition#group#ip#timeStamp*/, MetricResult>>
//            indexMapRef = new AtomicReference<>();
//    public static DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmm");
//    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
//    private static final long SLEEP_TIME = 60 * 1000;//sleep 1min
//    public static final String METRICE_SEPARATOR = "#";
//    public static final int PRODUCER = 0;
//    public static final int CONSUMER = 1;
//    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
//    //    private static final ConcurrentHashMap<String/**subPartition*/,ConcurrentHashMap<Long/**ledgerId*/,BitSet>> subscriptionMap = new ConcurrentHashMap();
//    private static final Cache<String/**subscription-topicName-ledgerId */, BitSet> lruCache =
//            CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();
//    private static final int MAX_ENTRY_PER_LEDGER = 5000000;
//    private static volatile long reConsumeCount = 0;
//
//
//    static {
//        indexMapRef.set(new ConcurrentHashMap<String, MetricResult>());
//        Runtime.getRuntime().addShutdownHook(new IndexShutDownHook());
//        startCollector();
//    }
//
//    private static void startCollector() {
//        final Thread collectorThread = new Thread("index-collector-daemon") {
//            @Override
//            public void run() {
//                while (true) {
//                    final ReentrantLock lock = new ReentrantLock();
//                    final Condition condition = lock.newCondition();
//                    lock.lock();
//                    try {
//                        condition.await(SLEEP_TIME, TimeUnit.MILLISECONDS);
//                        flushIndex2Log();
//                    } catch (Exception e) {
//                        LOG.error(e.getMessage(), e);
//                    } finally {
//                        lock.unlock();
//                    }
//                }
//            }
//        };
//        collectorThread.setDaemon(true);
//        collectorThread.start();
//    }
//
//    public static void flushIndex2Log() {
//        ConcurrentHashMap<String/*type#topic#group#ip#timeStamp*/, MetricResult> indexMap2Print = null;
//        ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
//        try {
//            if (writeLock.tryLock(5000, TimeUnit.MILLISECONDS)) {
//                indexMap2Print = indexMapRef.get();
//                indexMapRef.set(new ConcurrentHashMap<String, MetricResult>());
//            }
//        } catch (InterruptedException e) {
//            LOG.info("flushIndex2Log exception : ", e);
//        } finally {
//            writeLock.unlock();
//        }
//        if (indexMap2Print != null) {
//            LOG.info("metric num to send : " + indexMap2Print.size());
//            for (Map.Entry<String, MetricResult> entry : indexMap2Print.entrySet()) {
//                String key = entry.getKey();
//                MetricResult metricResult = entry.getValue();
//                if (metricResult != null) {
////                    LOG.info(key + METRICE_SEPARATOR + metricResult.hCnt + METRICE_SEPARATOR
////                            + metricResult.count + METRICE_SEPARATOR + metricResult.size);
////                    send2ClickHouse(entry);
//                }
//            }
//        }
//        LOG.info("reConsumeCount : " + reConsumeCount);
//        LOG.info("lruCache size : " + lruCache.size());
//    }
//
//    public static void record(final int type, final String ip, final String group, final String topic,
//                              String timeStamp, long hCnt, final long count, final long size, final long ledgerId,
//                              final long entryId) {
//        if (type == CONSUMER) {
//            String key = group + METRICE_SEPARATOR + topic + METRICE_SEPARATOR + ledgerId;
//            if (isRecored(key, entryId)) {
//                reConsumeCount++;
//                return;
//            }
//        }
//        int partitionIndex = TopicName.getPartitionIndex(topic);
//        String topicName = null;
//        if (partitionIndex != -1) {
//            topicName = topic.substring(0, topic.lastIndexOf("-partition-"));
//        }
//
//        String key = getKey(type, group, topicName == null ? topic : topicName, partitionIndex, ip, timeStamp);
//
//        ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
//        try {
//            if (readLock.tryLock(5000, TimeUnit.MILLISECONDS)) {
//                ConcurrentHashMap<String, MetricResult> indexMap = indexMapRef.get();
//                MetricResult result = indexMap.get(key);
//                if (result == null) {
//                    AtomicLong newCounter = new AtomicLong(0);
//                    AtomicLong newSize = new AtomicLong(0);
//                    AtomicLong newHCnt = new AtomicLong(0);
//                    MetricResult newResult = new MetricResult(newCounter, newSize, newHCnt);
//                    MetricResult existResult = indexMap.putIfAbsent(key, newResult);
//                    if (existResult == null) {
//                        result = newResult;
//                    } else {
//                        result = existResult;
//                    }
//                }
//                result.count.addAndGet(count);
//                result.size.addAndGet(size);
//                result.hCnt.addAndGet(hCnt);
//            }
//        } catch (Exception e) {
//            LOG.error(e.getMessage());
//        } finally {
//            readLock.unlock();
//        }
//    }
//
//    public static String timeFormat(String dateTime) {
//        long res = 0l;
//        String[] dateRegex = new String[]{
//                "yyyyMMdd HH",
//                "yyyyMMdd HH:mm",
//                "yyyyMMdd HH:mm:ss",
//                "yyyyMMdd HH:mm:ss.SSS",
//                "yyyy-MM-dd",
//                "yyyy-MM-dd HH",
//                "yyyy-MM-dd HH:mm",
//                "yyyy-MM-dd HH:mm:ss",
//                "yyyy-MM-dd HH:mm:ss.SSS",
//                "yyyy/MM/dd",
//                "yyyy/MM/dd HH",
//                "yyyy/MM/dd HH:mm",
//                "yyyy/MM/dd HH:mm:ss",
//                "yyyy/MM/dd HH:mm:ss.SSS",
//                "yyyyMMdd",
//                "yyyyMMddHH",
//                "yyyyMMddHHmm",
//                "yyyyMMddHHmmss"
//        };
//        try {
//            if (isNumeric(dateTime) && dateTime.length() == 13) {
//                res = Long.parseLong(dateTime);
//                return format.print(res);
//            }
//            String pattern = null;
//            if (dateTime.contains("-")) {
//                for (int i = 4; i < 9; i++) {
//                    if (dateRegex[i].length() == dateTime.length()) {
//                        pattern = dateRegex[i];
//                        break;
//                    }
//                }
//            } else if (dateTime.contains("/")) {
//                for (int i = 9; i < 14; i++) {
//                    if (dateRegex[i].length() == dateTime.length()) {
//                        pattern = dateRegex[i];
//                        break;
//                    }
//                }
//            } else if (dateTime.contains(" ") || dateTime.contains(":")) {
//                for (int i = 0; i < 4; i++) {
//                    if (dateRegex[i].length() == dateTime.length()) {
//                        pattern = dateRegex[i];
//                        break;
//                    }
//                }
//            } else {
//                for (int i = 14; i < 18; i++) {
//                    if (dateRegex[i].length() == dateTime.length()) {
//                        pattern = dateRegex[i];
//                        break;
//                    }
//                }
//            }
//            if (pattern == null) {
//                return format.print(0);
//            }
//            SimpleDateFormat formater = new SimpleDateFormat(pattern);
//            res = formater.parse(dateTime).getTime();
//            return format.print(res);
//        } catch (Exception e) {
//            return format.print(0);
//        }
//    }
//
//    private static String getKey(int type, String group, String topic, int partitionIndex, String ip,
//                                 String timeStamp) {
//        return type + METRICE_SEPARATOR + topic + METRICE_SEPARATOR + partitionIndex + METRICE_SEPARATOR + group +
//                METRICE_SEPARATOR + ip + METRICE_SEPARATOR + timeStamp;
//    }
//
//    private static final class IndexShutDownHook extends Thread {
//        @Override
//        public void run() {
//            flushIndex2Log();
//        }
//    }
//
//    public static boolean isNumeric(String str) {
//        Pattern pattern = Pattern.compile("-?[0-9]+(\\.([0-9]+))?");
//        return pattern.matcher(str).matches();
//    }
//
//    private static boolean isRecored(String key, long entryId) {
//        try {
//            BitSet bitSet = lruCache.get(key, new Callable<BitSet>() {
//                @Override
//                public BitSet call() throws Exception {
//                    return new BitSet(MAX_ENTRY_PER_LEDGER);
//                }
//            });
//
//
//            if (entryId > bitSet.size()) {
//                //TODO 收敛日志
//                LOG.warn("entryId : {} > bitset size: {} exception.", entryId, bitSet.size());
//                return false;
//            }
//
//            if (bitSet.get((int) entryId)) {
//                return true;
//            } else {
//                bitSet.set((int) entryId);
//                return false;
//            }
//        } catch (Exception e) {
//            LOG.error("isRecored method exception:", e);
//            return false;
//        }
//    }
//
//    public static void main(String[] args) {
//        try {
//            System.out.println(format.print(new DateTime(Long.valueOf(System.currentTimeMillis()))));
//            System.out.println(timeFormat(String.valueOf(System.currentTimeMillis())));
//        } catch (Exception e) {
//            LOG.error(e.getMessage());
//        }
//    }
//}
//
//class MetricResult {
//    final AtomicLong size;
//    final AtomicLong count;
//    final AtomicLong hCnt;
//
//    public MetricResult(AtomicLong size, AtomicLong count, AtomicLong hCnt) {
//        this.size = size;
//        this.count = count;
//        this.hCnt = hCnt;
//    }
//
//    public AtomicLong getSize() {
//        return size;
//    }
//
//    public AtomicLong getCount() {
//        return count;
//    }
//
//    public AtomicLong gethCnt() {
//        return hCnt;
//    }
//}
//
