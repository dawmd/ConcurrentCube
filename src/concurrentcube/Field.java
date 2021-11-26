package concurrentcube;

public enum Field {
    TOP (0),
    LEFT (1),
    FRONT (2),
    RIGHT (3),
    BACK (4),
    DOWN (5);

    private final int numVal;

    private Field(int numVal) {
        this.numVal = numVal;
    }

    public int getVal() {
        return numVal;
    }

    public static Field getFieldType(int val) {
        assert (val >= 0 && val < Field.values().length);

        return Field.values()[val];
    }

    public static String toString(Field field) {
        return Integer.toString(field.getVal());
    }
}
