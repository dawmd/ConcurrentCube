package concurrentcube;

import java.util.Arrays;

public class Side {
    private Field[][] fields;

    private Side() {}

    public Side(int size, Field fieldsColor) {
        fields = new Field[size][size];
        colorSide(fieldsColor);
    }

    private void colorSide(Field fieldsColor) {
        Thread[] threads = new Thread[fields.length];

        for (int i = 0; i < fields.length; ++i) {
            // this might be a bit risky, but afaik,
            // this creates a REFERENCE to the original array,
            // so the changes made on it should also affect the original one
            Field[] fieldsTmp = fields[i];
            threads[i] = new Thread(
                    () -> Arrays.stream(fieldsTmp)
                            .forEach(field -> field = fieldsColor)
            );
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
               thread.join();
            } catch (InterruptedException e) {
                System.err.println("error while coloring in the constructor");
            }
        }
    }
}
