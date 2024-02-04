public enum Constants {
    CHUNK_SIZE(1000),
    BYTES_IN_KB(1000),
    BITS_IN_BYTE(8);

    private final int value;

    private Constants(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
