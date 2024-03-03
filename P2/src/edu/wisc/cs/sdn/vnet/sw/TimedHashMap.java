import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class TimedHashMap<K, V> extends ConcurrentHashMap<K, Value<V>> {
    private long duration;
    private long granularity;

    public TimedHashMap(long duration, long granularity) {
        this.duration = duration;
        this.granularity = granularity;
        PeriodicTask task = new PeriodicTask(() -> timeout(), granularity);
        task.start();
    }

    public void putTimed(K key, V value) {
        Value<V> wrapperValue = new Value<V>(value);

        put(key, wrapperValue);
    }

    public V getTimed(K key) {
        Value<V> wrapperValue = get(key);

        return (wrapperValue != null) ? wrapperValue.getValue() : null;
    }

    public void timeout() {
        long currTime = System.currentTimeMillis();
        for(Map.Entry<K, Value<V>> entry : entrySet()) {
            if((currTime - entry.getValue().getLastUpdate()) > duration) {
                remove(entry);
            }
        }
    }

    public static void main(String[] args) {

    }
}

class Value<X> {
    private long lastUpdated;
    private X value;

    public Value(X value) {
        lastUpdated = System.currentTimeMillis();
        this.value = value;
    }

    public X getValue() {
        return this.value;
    }

    public long getLastUpdate() {
        return this.lastUpdated;
    }
}

class PeriodicTask implements Runnable {
    Runnable task;
    long period;
    Thread thread;

    public PeriodicTask(Runnable task, long period) {
        this.task = task;
        this.period = period;
    }

    public void start() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while(true) {
            task.run();
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }
}
