package concurrentcube;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

//...

public class Cube {

    private static class Dimension {
        public boolean[] blocked;
        public AtomicInteger howManyUsed = new AtomicInteger(0);

        public Dimension(int size) {
            blocked = new boolean[size];
            Arrays.fill(blocked, false);
        }
    }

    private static final int numberOfSides = 6;

    private final int size;
    private Side[] cubeSides;
    private final BiConsumer<Integer, Integer> beforeRotation;
    private final BiConsumer<Integer, Integer> afterRotation;
    private final Runnable beforeShowing;
    private final Runnable afterShowing;

    private final AtomicInteger readersCounter = new AtomicInteger(0);
    private final AtomicInteger writersCounter = new AtomicInteger(0);
    private final AtomicInteger waitingReadersCounter = new AtomicInteger(0);
    private final AtomicInteger waitingWritersCounter = new AtomicInteger(0);

    private final Semaphore readersQueue = new Semaphore(0, true);
    private final Semaphore writersQueue = new Semaphore(0, true);
    private final Semaphore mutex = new Semaphore(1, true);

    private final Semaphore gettingReady = new Semaphore(0, true);
    private Field waitingSide = null;
    private int waitingLayer = -1;

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

    // ok?
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

    // ok
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

    // ok
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

    // ok
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

    // ok
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

    // ok?
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

    private boolean canWrite(Field side, int layer) {
        if (readersCounter.get() > 0) {
            return false;
        }

        boolean canStartWriting = true;
        for (Field sideType : dimensions.keySet()) {
            if (!sideType.equals(side) && dimensions.get(sideType).howManyUsed.get() > 0) {
                canStartWriting = false;
                break;
            }
        }

        return canStartWriting && (!dimensions.get(side).blocked[layer]);
    }

    private void markWriting(Field side, int layer) {
        dimensions.get(side).blocked[layer] = true;
        dimensions.get(side).howManyUsed.incrementAndGet();
    }

    private void unmarkWriting(Field side, int layer) {
        dimensions.get(side).blocked[layer] = false;
        dimensions.get(side).howManyUsed.decrementAndGet();
    }

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

    // pisarz
    public void rotate(int side, int layer) throws InterruptedException {

        RightParameteres parameteres = new RightParameteres(side, layer, size);

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        mutex.acquireUninterruptibly();

        if (readersCounter.get() > 0 || waitingWritersCounter.get() > 0 || waitingSide != null) {
            waitingWritersCounter.incrementAndGet();
            mutex.release();
            writersQueue.acquireUninterruptibly();
        }

        if (!canWrite(parameteres.side, parameteres.layer)) {
            waitingSide = parameteres.side;
            waitingLayer = parameteres.layer;
            mutex.release();
            gettingReady.acquireUninterruptibly();
        }

        markWriting(parameteres.side, parameteres.layer);
        mutex.release();
        if (waitingSide != null && waitingReadersCounter.get() == 0 && waitingWritersCounter.get() > 0) {
            waitingSide = null;
            writersQueue.release();
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        // pisanie
        beforeRotation.accept(side, layer);
        rotateAux(Field.getFieldType(side), layer);
        afterRotation.accept(side, layer);

        mutex.acquireUninterruptibly();

        unmarkWriting(parameteres.side, parameteres.layer);
        if (waitingSide != null && canWrite(waitingSide, waitingLayer)) {
            gettingReady.release();
        }
        else if (waitingReadersCounter.get() > 0) {
            readersQueue.release();
        }
        else {
            mutex.release();
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private String readCube() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfSides; ++i) {
            result.append(cubeSides[i].toString());
        }
        return result.toString();
    }

    // czytelnik
    public String show() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        // pisarze i czytelnicy
        mutex.acquireUninterruptibly();
        if (waitingWritersCounter.get() > 0) {
            waitingReadersCounter.incrementAndGet();
            if (Thread.interrupted()) {
                waitingReadersCounter.decrementAndGet();
                mutex.release();
                throw new InterruptedException();
            }
            mutex.release();

            readersQueue.acquireUninterruptibly();
            waitingReadersCounter.decrementAndGet();
            readersCounter.incrementAndGet();
            if (waitingReadersCounter.get() > 0) {
                readersQueue.release();
            }
            else {
                mutex.release();
            }
        }
        else {
            readersCounter.incrementAndGet();
            mutex.release();
        }

        String result = "";
        if (!Thread.interrupted()) {
            // reading
            beforeShowing.run();
            result = readCube();
            afterShowing.run();
        }

        mutex.acquireUninterruptibly();
        readersCounter.decrementAndGet();
        if (readersCounter.get() == 0) {
            if (writersCounter.get() > 0) {
                writersQueue.release();
            }
            else {
                mutex.release();
            }
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return result;
    }

}