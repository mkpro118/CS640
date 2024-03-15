package edu.wisc.cs.sdn.vnet.utils;

import edu.wisc.cs.sdn.vnet.utils.PeriodicTask;
import edu.wisc.cs.sdn.vnet.utils.TimedValue;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A map that associates keys with timed values and automatically
 * removes entries that exceed a timeout duration
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@SuppressWarnings("serial")
public class TimeoutMap<K, V> extends ConcurrentHashMap<K, TimedValue<V>> {
    private long timeoutDuration;
    private long granularity;
    private PeriodicTask cleaner;

    /**
     * Constructs a new TimeoutMap with the specified timeout duration and
     * cleanup granularity
     *
     * @param timeoutDuration the timeout duration in milliseconds
     * @param granularity     the cleanup granularity in milliseconds
     */
    public TimeoutMap(long timeoutDuration, long granularity) {
        super();

        this.timeoutDuration = timeoutDuration;
        this.granularity = granularity;

        cleaner = new PeriodicTask(() -> timeout(), granularity);
        cleaner.start();
    }

    /**
     * Associates the specified value with the specified key in this map,
     * with an expiration time
     *
     * This method behaves almost exactly as {@code Map.put(...)}, but wraps the
     * value in a TimedValue to maintain update times
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     *
     * @return the previous value associated with the key,
     *         or {@code null} if there was no mapping for the key
     */
    public V putTimed(K key, V value) {
        TimedValue<V> old = get(key);

        if (old == null) {
            put(key, new TimedValue<>(value));
            return null;
        }

        if (old.getValue().equals(value))
            old.update();
        else
            put(key, new TimedValue<>(value));

        return old.getValue();
    }

    public V getTimed(K key) {
        TimedValue<V> value = get(key);

        if (value == null) { return null; }

        return value.getValue();
    }

    /**
     * Removes entries from the map that have exceeded the timeout duration
     */
    public void timeout() {
        long currTime = System.currentTimeMillis();

        entrySet()
        .parallelStream()
        .filter(e -> currTime - e.getValue().getLastUpdate() > timeoutDuration)
        .forEach(e -> this.remove(e.getKey()));
    }

    /**
     * Stops the periodic cleanup task associated with this map
     */
    public void stopCleaner() { cleaner.stop(); }
}
