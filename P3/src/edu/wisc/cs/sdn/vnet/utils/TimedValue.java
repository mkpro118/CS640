package edu.wisc.cs.sdn.vnet.utils;

/**
 * A generic class representing a value associated with a
 * timestamp of its last update.
 *
 * @param <T> the type of the value
 */
public class TimedValue<T> {
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
