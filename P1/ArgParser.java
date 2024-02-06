import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Arrays;

/**
 * @author Mrigank Kumar
 */
class Option<T> {
    private String description;
    private Class<T> type;
    private T value;
    private boolean found;

    public Option(String description, Class<T> type) {
        this.description = description;
        this.type = type;
        this.value = Option.getDefaultValue(type);
        this.found = false;
    }

    public String getDescription() { return description; }

    public Class<T> getType() { return type; }

    public T getValue() { return value; }

    public void setValue(T value) {
        this.value = value;
        this.found = true;
    }

    public boolean found() { return found; }

    @SuppressWarnings("unchecked")
    private static <T> T getDefaultValue(Class<T> type) {
        if (type.equals(Boolean.class))
            return (T) Boolean.FALSE;

        if (type.equals(Character.class))
            return (T) Character.valueOf('\0');

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
 */
public class ArgParser {
    @SuppressWarnings("serial")
    private static class TypeNotSupportedException extends RuntimeException {
        public TypeNotSupportedException() {}

        public TypeNotSupportedException(String msg) {
            super(msg);
        }
    }

    private static final Map<String, Class<?>> typeMap;

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

    private Map<String, Option<?>> options;

    public ArgParser() {
        options = new HashMap<>();
    }

    public ArgParser addOption(String key, String type) {
        addOption(key, getClass(type), null);
        return this;
    }

    public <T> ArgParser addOption(String key, Class<T> type) {
        addOption(key, type, null);
        return this;
    }

    public ArgParser addOption(String key, String type, String description) {
        addOption(key, getClass(type), description);
        return this;
    }

    public <T> ArgParser addOption(String key, Class<T> type, String description) {
        options.put(key, new Option<T>(description, type));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Option<T> option = (Option<T>) options.get(key);
        return option != null ? option.getValue() : null;
    }

    public Option<?> getOption(String key) { return options.get(key); }

    public Set<String> getOptionsSet() { return options.keySet(); }

    public <T> void parse(String args) {
        parse(Arrays.stream(args.split(" "))
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new));
    }

    @SuppressWarnings("unchecked")
    public <T> void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            Option<?> option = options.get(args[i]);

            if (option == null)
                continue;

            if (option.getType().equals(Boolean.class))
                ((Option<Boolean>) option).setValue(true);
            else if (i + 1 < args.length)
                setOptionValue(option, args[++i]);
        }
    }

    public Option<?> removeOption(String name) {
        return options.remove(name);
    }

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
