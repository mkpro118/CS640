/**
 * @author Mrigank Kumar
 *
 * Enumeration of constants used in network testing
 * This enum provides predefined constant values for various parameters used
 * in network testing, such as chunk size, time conversion factors, and byte
 * and bit conversion factors
 */
public enum Constants {
    /** The size of data chunks used in network communication (bytes) */
    CHUNK_SIZE(1000),

    /** The conversion factor from milliseconds to seconds */
    MILLISECONDS_IN_SECONDS(1000),

    /** The conversion factor from bytes to kilobytes */
    BYTES_IN_KB(1000),

    /** The conversion factor from bits to bytes */
    BITS_IN_BYTE(8);

    // The integer value associated with each constant
    private final int value;

    /**
     * Constructs a Constants enum with the specified integer value
     *
     * @param value the integer value associated with the constant
     */
    private Constants(int value) { this.value = value; }

    /**
     * Accessor for the integer value associated with the constant
     *
     * @return the integer value associated with the constant
     */
    public int getValue() { return value; }
}
