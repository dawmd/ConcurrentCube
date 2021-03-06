package concurrentcube;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;


public class Cube {

    private static class RightParameteres {
        public final Field side;
        public final int layer;

        private RightParameteres() {
            side = null;
            layer = 0;
        }

        public RightParameteres(int side, int layer, int size) {
            switch (Field.getFieldType(side)) {
                case TOP:
                    this.side = Field.TOP;
                    this.layer = layer;
                    break;
                case LEFT:
                    this.side = Field.LEFT;
                    this.layer = layer;
                    break;
                case FRONT:
                    this.side = Field.FRONT;
                    this.layer = layer;
                    break;
                case RIGHT:
                    this.side = Field.LEFT;
                    this.layer = size - layer - 1;
                    break;
                case BACK:
                    this.side = Field.FRONT;
                    this.layer = size - layer - 1;
                    break;
                case DOWN:
                    this.side = Field.TOP;
                    this.layer = size - layer - 1;
                    break;
                default:
                    assert (false);
                    this.side = null;
                    this.layer = 0;
                    break;
            }
        }
    }

    private static class Dimension {
        public Semaphore[] blocked;
        public int waitingCount = 0;
        public Semaphore waitingQueue = new Semaphore(0, true);

        public Dimension(int size) {
            blocked = new Semaphore[size];
            for (int i = 0; i < size; ++i) {
                blocked[i] = new Semaphore(1, true);
            }
        }
    }

    private static final int numberOfSides = 6;

    private final int size;
    private Side[] cubeSides;
    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;

    private int readersCounter = 0;
    private int writersCounter = 0;
    private int waitingReadersCounter = 0;
    private int waitingWritersCounter = 0;

    private final Semaphore readersQueue = new Semaphore(0, true);
    private final Semaphore mainWritersQueue = new Semaphore(0, true);
    private final Semaphore mutex = new Semaphore(1, true);

    private Field currentDimension = null;

    private final HashMap<Field, Cube.Dimension> dimensions = new HashMap<>();

    private Cube() {
        this.size = 0;
        this.beforeRotation = null;
        this.afterRotation = null;
        this.beforeShowing = null;
        this.afterShowing = null;
    }

    public Cube(int size,
                 BiConsumer<Integer, Integer> beforeRotation,
                 BiConsumer<Integer, Integer> afterRotation,
                 Runnable beforeShowing,
                 Runnable afterShowing) {

        assert (numberOfSides == Field.values().length);

        this.size = size;
        this.beforeRotation = beforeRotation;
        this.afterRotation = afterRotation;
        this.beforeShowing = beforeShowing;
        this.afterShowing = afterShowing;

        cubeSides = new Side[numberOfSides];

        for (int i = 0; i < numberOfSides; ++i) {
            cubeSides[i] = new Side(size, Field.values()[i]);
        }

        dimensions.put(Field.TOP, new Cube.Dimension(size));
        dimensions.put(Field.LEFT, new Cube.Dimension(size));
        dimensions.put(Field.FRONT, new Cube.Dimension(size));

    }

    private void rotateSide(Field side, boolean rotateRight) {
        Side sideToRotate = cubeSides[side.getVal()];
        if (rotateRight)
            sideToRotate.rotateRight();
        else
            sideToRotate.rotateLeft();
    }

    private void rotateAuxTop(int layer) {
        Field[] tmp = cubeSides[Field.FRONT.getVal()].getRow(layer);
        cubeSides[Field.FRONT.getVal()].replaceRow(
                cubeSides[Field.RIGHT.getVal()].getRow(layer),
                layer
        );
        cubeSides[Field.RIGHT.getVal()].replaceRow(
                cubeSides[Field.BACK.getVal()].getRow(layer),
                layer
        );
        cubeSides[Field.BACK.getVal()].replaceRow(
                cubeSides[Field.LEFT.getVal()].getRow(layer),
                layer
        );
        cubeSides[Field.LEFT.getVal()].replaceRow(
                tmp,
                layer
        );

        if (layer == 0) {
            rotateSide(Field.TOP, true);
        }
        else if (layer == size - 1) {
            rotateSide(Field.DOWN, false);
        }
    }

    private void rotateAuxLeft(int layer) {
        Field[] tmp = cubeSides[Field.TOP.getVal()].getColumn(layer);
        cubeSides[Field.TOP.getVal()].replaceColumn(
                cubeSides[Field.BACK.getVal()].getReversedColumn(size - layer - 1),
                layer
        );
        cubeSides[Field.BACK.getVal()].replaceColumn(
                cubeSides[Field.DOWN.getVal()].getReversedColumn(layer),
                size - layer - 1
        );
        cubeSides[Field.DOWN.getVal()].replaceColumn(
                cubeSides[Field.FRONT.getVal()].getColumn(layer),
                layer
        );
        cubeSides[Field.FRONT.getVal()].replaceColumn(
                tmp,
                layer
        );

        if (layer == 0) {
            rotateSide(Field.LEFT, true);
        }
        else if (layer == size - 1) {
            rotateSide(Field.RIGHT, false);
        }
    }

    private void rotateAuxFront(int layer) {
        Field[] tmp = cubeSides[Field.TOP.getVal()].getRow(size - layer - 1);
        cubeSides[Field.TOP.getVal()].replaceRow(
                cubeSides[Field.LEFT.getVal()].getReversedColumn(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.LEFT.getVal()].replaceColumn(
                cubeSides[Field.DOWN.getVal()].getRow(layer),
                size - layer - 1
        );
        cubeSides[Field.DOWN.getVal()].replaceRow(
                cubeSides[Field.RIGHT.getVal()].getReversedColumn(layer),
                layer
        );
        cubeSides[Field.RIGHT.getVal()].replaceColumn(
                tmp,
                layer
        );

        if (layer == 0) {
            rotateSide(Field.FRONT, true);
        }
        else if (layer == size - 1) {
            rotateSide(Field.BACK, false);
        }
    }

    private void rotateAuxRight(int layer) {
        Field[] tmp = cubeSides[Field.TOP.getVal()].getReversedColumn(size - layer - 1);
        cubeSides[Field.TOP.getVal()].replaceColumn(
                cubeSides[Field.FRONT.getVal()].getColumn(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.FRONT.getVal()].replaceColumn(
                cubeSides[Field.DOWN.getVal()].getColumn(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.DOWN.getVal()].replaceColumn(
                cubeSides[Field.BACK.getVal()].getReversedColumn(layer),
                size - layer - 1
        );
        cubeSides[Field.BACK.getVal()].replaceColumn(
                tmp,
                layer
        );

        if (layer == 0) {
            rotateSide(Field.RIGHT, true);
        }
        else if (layer == size - 1) {
            rotateSide(Field.LEFT, false);
        }
    }

    private void rotateAuxBack(int layer) {
        Field[] tmp = cubeSides[Field.TOP.getVal()].getReversedRow(layer);
        cubeSides[Field.TOP.getVal()].replaceRow(
                cubeSides[Field.RIGHT.getVal()].getColumn(size - layer - 1),
                layer
        );
        cubeSides[Field.RIGHT.getVal()].replaceColumn(
                cubeSides[Field.DOWN.getVal()].getReversedRow(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.DOWN.getVal()].replaceRow(
                cubeSides[Field.LEFT.getVal()].getColumn(layer),
                size - layer - 1
        );
        cubeSides[Field.LEFT.getVal()].replaceColumn(
                tmp,
                layer
        );

        if (layer == 0) {
            rotateSide(Field.BACK, true);
        }
        else if (layer == size - 1) {
            rotateSide(Field.FRONT, false);
        }
    }

    private void rotateAuxDown(int layer) {
        Field[] tmp = cubeSides[Field.FRONT.getVal()].getRow(size - layer - 1);
        cubeSides[Field.FRONT.getVal()].replaceRow(
                cubeSides[Field.LEFT.getVal()].getRow(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.LEFT.getVal()].replaceRow(
                cubeSides[Field.BACK.getVal()].getRow(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.BACK.getVal()].replaceRow(
                cubeSides[Field.RIGHT.getVal()].getRow(size - layer - 1),
                size - layer - 1
        );
        cubeSides[Field.RIGHT.getVal()].replaceRow(
                tmp,
                size - layer - 1
        );

        if (layer == 0) {
            rotateSide(Field.DOWN, true);
        }
        else if (layer == size - 1) {
            rotateSide(Field.TOP, false);
        }
    }

    private void rotateAux(Field side, int layer) {
        switch (side) {
            case TOP:
                rotateAuxTop(layer);
                break;
            case LEFT:
                rotateAuxLeft(layer);
                break;
            case FRONT:
                rotateAuxFront(layer);
                break;
            case RIGHT:
                rotateAuxRight(layer);
                break;
            case BACK:
                rotateAuxBack(layer);
                break;
            case DOWN:
                rotateAuxDown(layer);
                break;
            default:
                assert (false);
                break;
        }
    }

    private boolean shouldWait(Field side) {
        return currentDimension != null && !side.equals(currentDimension);
    }

    // Writer
    public void rotate(int side, int layer) throws InterruptedException {

        RightParameteres parameteres = new RightParameteres(side, layer, size);
        Dimension myDimension = dimensions.get(parameteres.side);

        mutex.acquireUninterruptibly();
        ++waitingWritersCounter;
        if (readersCounter > 0 || waitingReadersCounter > 0 || shouldWait(parameteres.side)) {
            ++myDimension.waitingCount;
            if (myDimension.waitingCount == 1) {
                mutex.release();
                mainWritersQueue.acquire();
            }
            else {
                mutex.release();
                myDimension.waitingQueue.acquire();
            }
            --myDimension.waitingCount;
        }

        currentDimension = parameteres.side;
        --waitingWritersCounter;
        ++writersCounter;
        if (myDimension.waitingCount > 0) {
            myDimension.waitingQueue.release();
        }
        else {
            mutex.release();
        }

        myDimension.blocked[parameteres.layer].acquire();

        // Critical section
        beforeRotation.accept(side, layer);
        rotateAux(Field.getFieldType(side), layer);
        afterRotation.accept(side, layer);

        myDimension.blocked[parameteres.layer].release();
        mutex.acquireUninterruptibly();

        --writersCounter;
        if (writersCounter == 0) {
            currentDimension = null;
            if (waitingReadersCounter > 0) {
                readersQueue.release();
            }
            else if (waitingWritersCounter > 0) {
                mainWritersQueue.release();
            }
            else {
                mutex.release();
            }
        }
        else {
            mutex.release();
        }
    }

    private String readCube() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfSides; ++i) {
            result.append(cubeSides[i].toString());
        }
        return result.toString();
    }

    // Reader
    public String show() throws InterruptedException {

        mutex.acquireUninterruptibly();
        ++waitingReadersCounter;
        if (writersCounter > 0 || waitingWritersCounter > 0) {
            mutex.release();
            readersQueue.acquire();
        }

        --waitingReadersCounter;
        ++readersCounter;
        if (waitingReadersCounter > 0) {
            readersQueue.release();
        }
        else {
            mutex.release();
        }

        // Critical section
        beforeShowing.run();
        String result = readCube();
        afterShowing.run();

        mutex.acquireUninterruptibly();
        --readersCounter;
        if (readersCounter == 0) {
            if (waitingWritersCounter > 0) {
                mainWritersQueue.release();
            }
            else if (waitingReadersCounter > 0) {
                readersQueue.release();
            }
            else {
                mutex.release();
            }
        }
        else {
            mutex.release();
        }

        return result;
    }

}