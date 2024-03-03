package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
    private TimeoutMap<MACAddress, Iface> cache;
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
        cache = new TimeoutMap<MACAddress, Iface>(15000L, 1000L);
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */

        MACAddress srcMAC = etherPacket.getSourceMAC();
        MACAddress destMAC = etherPacket.getSourceMAC();

        cache.putTimed(srcMAC, inIface);

        Iface outIface = cache.getTimed(destMAC);

        // If dest Iface exists in cache, send it there
        if (outIface != null) {
            sendPacket(etherPacket, outIface);
            return;
        }

        // Flood the packet otherwise
        interfaces
        .values()
        .parallelStream()
        .filter(e -> e != null)
        .filter(e -> !e.getName().equals(inIface.getName()))
        .forEach(iface -> sendPacket(etherPacket, iface));
		
		/********************************************************************/
	}
}

/**
 * A generic class representing a value associated with a
 * timestamp of its last update.
 *
 * @param <T> the type of the value
 */
class TimedValue<T> {
    private long lastUpdate;
    private T value;

    /**
     * Constructs a new {@code TimedValue} with the given initial value.
     *
     * @param value the initial value
     */
    public TimedValue(T value) {
        this.value = value;
        update();
    }

    /**
     * Updates the timestamp to the current system time
     */
    public void update() { lastUpdate = System.currentTimeMillis(); }

    /**
     * Gets the timestamp of the last update
     *
     * @return the timestamp of the last update
     */
    public long getLastUpdate() { return lastUpdate; }

    /**
     * Gets the value
     *
     * @return the value
     */
    public T getValue() { return value; }

    /**
     * Returns a string representation of the value and it's last update time
     *
     * @return a string representation of the value and it's last update time
     */
    @Override
    public String toString() {
        String repr = "{value=" + this.value;
        repr += ", lastUpdated=" + lastUpdate + "}";
        return repr;
    }
}

/**
 * A class representing a task that runs periodically at a specified interval
 */
class PeriodicTask implements Runnable {
    private Thread thread;
    private Runnable task;
    private long duration;
    private boolean isDaemon;
    private volatile boolean active;

    /**
     * Constructs a new periodic task with the given task and duration,
     * and sets it as a daemon task
     *
     * @param task     the task to run periodically
     * @param duration the duration between each run of the task in milliseconds
     */
    public PeriodicTask(Runnable task, long duration) {
        this(task, duration, true);
    }

    /**
     * Constructs a new periodic task with the given task, duration,
     * and daemon status
     *
     * @param task     the task to run periodically
     * @param duration the duration between each run of the task in milliseconds
     * @param isDaemon whether the task should be a daemon task or not
     */
    public PeriodicTask(Runnable task, long duration, boolean isDaemon) {
        this.task = task;
        this.active = true;
        this.isDaemon = isDaemon;
        setDuration(duration);
    }

    /**
     * Get the periodically run task
     *
     * @return the periodically run task
     */
    public Runnable getTask() { return task; }

    /**
     * Sets the task to run periodically
     * Typical usage should not need to use this method, but it exists to
     * allow flexibility
     *
     * @param task the task to run periodically
     */
    public void setTask(Runnable task) { this.task = task; }

    /**
     * Get the interval duration of the periodic task
     *
     * @return the interval duration of the periodic task
     */
    public long getDuration() { return duration; }

    /**
     * Sets the duration between each run of the task
     * Typical usage should not need to use this method, but it exists to
     * allow flexibility
     *
     * @param duration the duration between each run of the task in milliseconds
     */
    public void setDuration(long duration) { this.duration = duration; }

    /**
     * Returns the daemon status of the period task thread
     *
     * @return true if the task is running on a daemon thread, false otherwise
     */
    public boolean isDaemon() { return isDaemon; }

    /**
     * Sets whether the task should be a daemon task or not
     *
     * @param isDaemon true if the task should be a daemon task, false otherwise
     */
    public void setDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
        this.thread.setDaemon(isDaemon);
    }

    /**
     * Runs the task periodically at the specified interval until the
     * task is stopped
     */
    @Override
    public void run() {
        while (active) {
            task.run();

            try { Thread.sleep(duration); }
            catch (InterruptedException e) { e.printStackTrace(System.err); }
        }
    }

    /**
     * Starts the periodic task
     */
    public void start() {
        thread = new Thread(this);
        thread.setDaemon(isDaemon);
        thread.start();
    }

    /**
     * Stops the periodic task
     */
    public void stop() {
        active = false;

        if (thread == null) return;

        try { thread.join(); }
        catch (InterruptedException e) { e.printStackTrace(System.err); }
    }
}

/**
 * A map that associates keys with timed values and automatically
 * removes entries that exceed a timeout duration
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@SuppressWarnings("serial")
class TimeoutMap<K, V> extends ConcurrentHashMap<K, TimedValue<V>> {
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
