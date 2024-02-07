import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Arrays;

/**
 * @author Mrigank Kumar
 *
 * Represents an option with a description, type, and value
 *
 * @param <T> the type of the option's value
 */
class Option<T> {
    // Description of the option
    private String description;

    // Type of the option's value
    private Class<T> type;

    // Value of the option
    private T value;

    // Indicates whether the option has been found
    private boolean found;

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

/**
 * @author Mrigank Kumar
 *
 * This class provides functionality for parsing command-line arguments
 * and managing options
 */
public class ArgParser {
    /**
     * Custom exception class for indicating unsupported types
     * during argument parsing
     */
    @SuppressWarnings("serial")
    private static class TypeNotSupportedException extends RuntimeException {
        public TypeNotSupportedException() {}

        public TypeNotSupportedException(String msg) {
            super(msg);
        }
    }


    // Map to store mappings between string representation of types
    // and their corresponding Class objects
    private static final Map<String, Class<?>> typeMap;

    // Initialize typeMap
    static {
        typeMap = new HashMap<>();
        // Boolean type
        typeMap.put("boolean", Boolean.class);

        // Character type
        typeMap.put("char", Character.class);

        // Integer types
        typeMap.put("byte", Byte.class);
        typeMap.put("short", Short.class);
        typeMap.put("int", Integer.class);
        typeMap.put("long", Long.class);

        // Floating point types
        typeMap.put("float", Float.class);
        typeMap.put("double", Double.class);

        // String type
        typeMap.put("string", String.class);
    }

    // Map to store defined options
    private Map<String, Option<?>> options;

    /**
     * Constructs an ArgParser object with no options defined initially
     */
    public ArgParser() {
        options = new HashMap<>();
    }

    /**
     * Adds an option with the specified key and type to the parser
     *
     * @param key the key of the option
     * @param type the string representation of the type of the option's value
     *
     * @return the ArgParser object for method chaining
     */
    public ArgParser addOption(String key, String type) {
        addOption(key, getClass(type), null);
        return this;
    }

    /**
     * Adds an option with the specified key and type to the parser
     *
     * @param key the key of the option
     * @param type the Class object representing the type of the option's value
     * @param <T> the type parameter of the option's value
     *
     * @return the ArgParser object for method chaining
     */
    public <T> ArgParser addOption(String key, Class<T> type) {
        addOption(key, type, null);
        return this;
    }

    /**
     * Adds an option with the specified key, type and description
     * to the parser
     *
     * @param key the key of the option
     * @param type the string representation of the type of the option's value
     * @param description the description of the option
     *
     * @return the ArgParser object for method chaining
     */
    public ArgParser addOption(String key, String type, String description) {
        addOption(key, getClass(type), description);
        return this;
    }

    /**
     * Adds an option with the specified key, type, and description
     * to the parser
     *
     * @param key the key of the option
     * @param type the Class object representing the type of the option's value
     * @param description the description of the option
     * @param <T> the type parameter of the option's value
     *
     * @return the ArgParser object for method chaining
     */
    public <T> ArgParser addOption(String key, Class<T> type, String description) {
        options.put(key, new Option<T>(description, type));
        return this;
    }

    /**
     * Retrieves the value of the option associated with the specified key
     *
     * @param key the key of the option
     * @param <T> the type parameter of the option's value
     *
     * @return the value of the option, or null if the option does not exist
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Option<T> option = (Option<T>) options.get(key);
        return option != null ? option.getValue() : null;
    }

    /**
     * Retrieves the option associated with the specified key
     *
     * @param key the key of the option
     *
     * @return the option object, or null if the option does not exist
     */
    public Option<?> getOption(String key) { return options.get(key); }

    /**
     * Retrieves the set of keys for all defined options
     *
     * @return the set of keys for all defined options
     */
    public Set<String> getOptionsSet() { return options.keySet(); }

     /**
     * Parses the provided command-line arguments
     *
     * @param args the command-line arguments as a single string
     */
    public void parse(String args) {
        parse(Arrays.stream(args.split(" "))
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new));
    }

    /**
     * Parses the provided command-line arguments
     *
     * @param args the array of command-line arguments
     */
    @SuppressWarnings("unchecked")
    public void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            Option<?> option = options.get(args[i]);

            // If no associated option is found, ignore
            if (option == null)
                continue;

            // Boolean args are specified by existence
            if (option.getType().equals(Boolean.class))
                ((Option<Boolean>) option).setValue(true);
            else if (i + 1 < args.length)  // Ensure we have a value to read
                setOptionValue(option, args[++i]);
        }
    }

    /**
     * Removes the option associated with the specified key from the parser
     *
     * @param name the key of the option to be removed
     *
     * @return the removed option object, or null if the option does not exist
     */
    public Option<?> removeOption(String name) {
        return options.remove(name);
    }

    /**
     * Sets the value of the specified option based on the provided string value
     *
     * @param option the option whose value is to be set
     * @param value the string representation of the value
     * @param <T> the type parameter of the option's value
     */
    @SuppressWarnings("unchecked")
    private <T> void setOptionValue(Option<T> option, String value) {
        if (option.getType().equals(Character.class))
            ((Option<Character>) option).setValue(value.charAt(0));

        else if (option.getType().equals(Byte.class))
            ((Option<Byte>) option).setValue(Byte.valueOf(value));

        else if (option.getType().equals(Short.class))
            ((Option<Short>) option).setValue(Short.valueOf(value));

        else if (option.getType().equals(Integer.class))
            ((Option<Integer>) option).setValue(Integer.valueOf(value));

        else if (option.getType().equals(Long.class))
            ((Option<Long>) option).setValue(Long.valueOf(value));

        else if (option.getType().equals(Float.class))
            ((Option<Float>) option).setValue(Float.valueOf(value));

        else if (option.getType().equals(Double.class))
            ((Option<Double>) option).setValue(Double.valueOf(value));

        else if (option.getType().equals(String.class))
            ((Option<String>) option).setValue(value);
    }

    /**
     * Retrieves the Class object corresponding to the specified type string
     *
     * @param type the string representation of the type
     * @param <T> the type parameter
     *
     * @return the Class object corresponding to the specified type
     *
     * @throws TypeNotSupportedException if the specified type is not supported
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> getClass(String type) {
        type = type.toLowerCase();

        Class<T> typeClass = (Class<T>) typeMap.get(type);

        if (typeClass == null) {
            throw new TypeNotSupportedException(
                "Cannot read arguments as type " + type);
        }

        return typeClass;
    }
}
