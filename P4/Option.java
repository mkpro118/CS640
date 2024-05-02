import java.util.HashSet;
import java.util.Set;

/**
 * Represents an option with a description, type, and value
 *
 * @param <T> the type of the option's value
 *
 * @author Mrigank Kumar
 */
public class Option<T> {
    // Description of the option
    private String description;

    // Type of the option's value
    private Class<T> type;

    // Value of the option
    private T value;

    // Indicates whether the option has been found
    private boolean found;

    // Aliases for this option
    private Set<String> aliases;

    /**
     * Constructs an Option object with the specified description and type
     * The value is set to the default value of the specified type,
     * and found is initialized to falses
     *
     * @param description the description of the option
     * @param type the type of the option's value
     */
    public Option(String description, Class<T> type) {
        this.description = description;
        this.type = type;
        this.value = Option.getDefaultValue(type);
        this.found = false;
        this.aliases = new HashSet<String>();
    }

    /**
     * Accessor for the description of the option
     *
     * @return the description of the option
     */
    public String getDescription() { return description; }

    /**
     * Accessor for the type of the option's value
     *
     * @return the type of the option's value
     */
    public Class<T> getType() { return type; }

    /**
     * Accessor for the value of the option
     *
     * @return the value of the option
     */
    public T getValue() { return value; }

    /**
     * Sets the value of the option and marks the option as found
     *
     * @param value the value to be set
     */
    public void setValue(T value) {
        this.value = value;
        this.found = true;
    }

    public String[] getAliases() {
        return this.aliases.stream().toArray(String[]::new);
    }

    public void addAlias(String name) {
        this.aliases.add(name);
    }

    /**
     * Checks whether the option has been found
     *
     * @return true if the option has been found, otherwise false
     */
    public boolean found() { return found; }

    /**
     * Retrieves the default value for the specified type
     *
     * @param type the type for which the default value is to be retrieved
     * @param <T> the type parameter
     *
     * @return the default value for the specified type, or null if
     *         the type is not supported
     */
    @SuppressWarnings("unchecked")
    private static <T> T getDefaultValue(Class<T> type) {
        if (type.equals(Boolean.class))
            return (T) Boolean.FALSE;

        if (type.equals(Character.class))
            return (T) Character.valueOf((char) 0);

        if (type.equals(Byte.class))
            return (T) Byte.valueOf((byte) 0);

        if (type.equals(Short.class))
            return (T) Short.valueOf((short) 0);

        if (type.equals(Integer.class))
            return (T) Integer.valueOf(0);

        if (type.equals(Long.class))
            return (T) Long.valueOf((long) 0);

        if (type.equals(Float.class))
            return (T) Float.valueOf((float) 0.0);

        if (type.equals(Double.class))
            return (T) Double.valueOf(0.0);

        if (type.equals(String.class))
            return (T) "";

        return null;
    }
}
