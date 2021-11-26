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
        if (fields.length > 100) {
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
        else {
            Arrays.stream(fields).forEach(
                    column -> Arrays
                            .stream(column)
                            .forEach(field -> field = fieldsColor)
            );
        }
    }

    private void transpose() {
        Field tmpField;
        final int length = fields.length;

        for (int i = 0; i < length; ++i) {
            for (int j = i + 1; j < length; ++j) {
                tmpField = fields[j][i];
                fields[j][i] = fields[i][j];
                fields[i][j] = tmpField;
            }
        }
    }

    private void reverseRow(int rowIndex) {
        Field tmpField;
        final int length = fields.length;
        for (int i = 0; i < length / 2; ++i) {
            tmpField = fields[rowIndex][length - 1 - i];
            fields[rowIndex][length - 1 - i] = fields[rowIndex][i];
            fields[rowIndex][i] = tmpField;
        }
    }

    private void reverseColumn(int colIndex) {
        Field tmpField;
        final int length = fields.length;
        for (int i = 0; i < length / 2; ++i) {
            tmpField = fields[length - 1 - i][colIndex];
            fields[length - 1 - i][colIndex] = fields[i][colIndex];
            fields[i][colIndex] = tmpField;
        }
    }

    public void rotateRight() {
        transpose();
        final int length = fields.length;
        for (int i = 0; i < length; ++i) {
            reverseRow(i);
        }
    }

    public void rotateLeft() {
        transpose();
        final int length = fields.length;
        for (int i = 0; i < length; ++i) {
            reverseColumn(i);
        }
    }

    public Field[] getRow(int rowIndex) {
        final int length = fields.length;
        Field[] result = new Field[length];

        System.arraycopy(fields[rowIndex], 0, result, 0, length);
        // a może po prostu zwracać referencję?

        return result;
    }

    public Field[] getColumn(int columnIndex) {
        final int length = fields.length;
        Field[] result = new Field[length];

        for (int i = 0; i < length; ++i) {
            result[i] = fields[i][columnIndex];
        }

        return result;
    }

    public void replaceRow(Field[] newRow, int rowIndex) {
        fields[rowIndex] = newRow;
    }

    public void replaceColumn(Field[] newColumn, int columnIndex) {
        final int length = fields.length;
        for (int i = 0; i < length; ++i) {
            fields[i][columnIndex] = newColumn[i];
        }
    }
}
