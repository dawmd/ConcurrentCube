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

    private void rotateAux(Field side, int layer) throws InterruptedException {

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

        mutex.acquire();
        // tutaj trzeba zrobić warunek w while'u i sprawdzać nie to, czy ktoś pisze,
        // tylko czy ktoś zajmuje to, co nam jest potrzebne
//        if (/* czy pisze || */ readersCounter.get() > 0) {
//            waitingWritersCounter.incrementAndGet();
//            mutex.release();
//            writersQueue.acquire();
//            waitingWritersCounter.decrementAndGet();
//        }

        // to trzeba poprawić, bo w tym momencie po wybudzeniu przejdzie na koniec kolejki, a nie powinien!
        // mozna drugi semaphor chyba zrobić, ale wydaje się to też problematyczne
        if (readersCounter.get() > 0 || waitingWritersCounter.get() > 0 || waitingSide != null) {
            waitingWritersCounter.incrementAndGet();
            mutex.release();
            writersQueue.acquire();
        }
//        else {
//            /* wieszamy się na semaforze z kostki i inkrementujemy liczbę trwających procesów na danym wymiarze kostki */
//            /* te semafory muszą być mutexami */
//        }

        if (!canWrite(parameteres.side, parameteres.layer)) {
            waitingSide = parameteres.side;
            waitingLayer = parameteres.layer;
            mutex.release();
            gettingReady.acquire();
        }

        markWriting(parameteres.side, parameteres.layer);
        /* czy pisze = true */
        mutex.release(); // ???? okej, to ma sens, bo chwile wczesniej jakis czytelnik przekazal nam mutex

        // pisanie
        beforeRotation.accept(side, layer);
        rotateAux(parameteres.side, parameteres.layer);
        afterRotation.accept(side, layer);

        mutex.acquire();

        unmarkWriting(parameteres.side, parameteres.layer);
        /* czy pisze = false */
        if (waitingSide != null && canWrite(waitingSide, waitingLayer)) {
            gettingReady.release();
        }
        else if (waitingReadersCounter.get() > 0) {
            readersQueue.release();
        }
        else {
            mutex.release();
        }
    }

    private String readCube() {
        return "";
    }

    // czytelnik
    public String show() throws InterruptedException {
        // pisarze i czytelnicy
        mutex.acquire();
        if (waitingWritersCounter.get() > 0) {
            waitingReadersCounter.incrementAndGet();
            mutex.release();
            readersQueue.acquire();
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

        // reading
        beforeShowing.run();
        String result = readCube();
        afterShowing.run();

        mutex.acquire();
        readersCounter.decrementAndGet();
        if (readersCounter.get() == 0) {
            if (writersCounter.get() > 0) {
                writersQueue.release();
            }
            else {
                mutex.release();
            }
        }
        mutex.release();

        return result;
    }

}