package concurrentcube;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTests {

    private static void aux(Cube cube, int size, int numberOfRotations) throws Exception {
        ArrayList<Field[]> pairs = new ArrayList<>(3);
        pairs.add(0, new Field[2]);
        pairs.add(1, new Field[2]);
        pairs.add(2, new Field[2]);

        pairs.get(0)[0] = Field.TOP;
        pairs.get(0)[1] = Field.DOWN;
        pairs.get(1)[0] = Field.LEFT;
        pairs.get(1)[1] = Field.RIGHT;
        pairs.get(2)[0] = Field.FRONT;
        pairs.get(2)[1] = Field.BACK;

        ArrayList<SimpleEntry<Integer, Integer>> order = new ArrayList<>(numberOfRotations);
        Random rand = new Random();
        int[] choice = new int[numberOfRotations];

        for (int i = 0; i < numberOfRotations; ++i) {
            order.add(i, new SimpleEntry<Integer, Integer>(rand.nextInt(3), rand.nextInt(size)));
            choice[i] = rand.nextInt(2);
        }

        for (int i = 0; i < numberOfRotations; ++i) {
            cube.rotate(pairs.get(order.get(i).getKey())[choice[i]].getVal(), order.get(i).getValue());
        }
        for (int i = numberOfRotations - 1; i >= 0; --i) {
            cube.rotate(pairs.get(order.get(i).getKey())[1 - choice[i]].getVal(), size - order.get(i).getValue() - 1);
        }
    }

    // one-thread-based tests

    @Test
    public void test1() throws Exception {
        final int cubeSize = 3;
        Cube cube = new Cube(cubeSize, (o, o2) -> {}, (o, o2) -> {}, () -> {}, () -> {});
        cube.rotate(0, 0);
        cube.rotate(1, 1);
        cube.rotate(2, 2);
        cube.rotate(1, 2);
        cube.rotate(2, 0);
        cube.rotate(5, 2);
        assertEquals("152141101444213513525000002223133133040555544440522332", cube.show());
    }



    @Test
    public void test2() throws Exception {
        final int size = 5;
        final int numberOfRotations = 10;
        Cube cube = new Cube(size, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {});
        aux(cube, size, numberOfRotations);

        StringBuilder correctResult = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            correctResult.append(String.valueOf(i).repeat(size * size));
        }
        assertEquals(correctResult.toString(), cube.show());
    }

    @Test
    public void test3() throws Exception {
        final int size = 50;
        final int numberOfRotations = 1000;
        Cube cube = new Cube(size, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {});
        aux(cube, size, numberOfRotations);

        StringBuilder correctResult = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            correctResult.append(String.valueOf(i).repeat(size * size));
        }
        assertEquals(correctResult.toString(), cube.show());
    }

    @Test
    public void test4() throws Exception {
        final int size = 500;
        final int numberOfRotations = 20000;
        Cube cube = new Cube(size, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {});
        aux(cube, size, numberOfRotations);

        StringBuilder correctResult = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            correctResult.append(String.valueOf(i).repeat(size * size));
        }
        assertEquals(correctResult.toString(), cube.show());
    }

    @Test
    public void test5() throws Exception {
        final int size = 1000;
        final int numberOfRotations = 10000;
        Cube cube = new Cube(size, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {});
        aux(cube, size, numberOfRotations);

        StringBuilder correctResult = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            correctResult.append(String.valueOf(i).repeat(size * size));
        }
        assertEquals(correctResult.toString(), cube.show());
    }

    // concurrent tests

    private void concurrentAux(Cube cube, int size, int numberOfTests, int numberOfThreads) throws Exception {
        Thread[] threads = new Thread[numberOfThreads];
        Random rand = new Random();

        int[] colorCounter = new int[6];
        Arrays.fill(colorCounter, 0);

        for (int t = 0; t < numberOfTests; ++t) {
//            System.out.println("Test number " + (t + 1) + "\n");
            for (int i = 0; i < numberOfThreads; ++i) {
                threads[i] = new Thread(() -> {
                    try {
                        cube.rotate(rand.nextInt(6), rand.nextInt(size));
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                });
                threads[i].start();
            }

            for (int i = 0; i < numberOfThreads; ++i) {
                threads[i].join();
            }

            String state = cube.show();
            for (int i = 0; i < state.length(); ++i) {
                ++colorCounter[(int) state.charAt(i) - '0'];
            }

            for (int i = 0; i < 6; ++i) {
                System.out.println("Color " + i + " count: " + colorCounter[i]);
            }

            for (int i = 0; i < 6; ++i) {
                assertEquals(size * size, colorCounter[i]);
            }

            Arrays.fill(colorCounter, 0);
        }
    }

    @Test
    public void test6() throws Exception {
        BiConsumer<Integer, Integer> cons = (integer, integer2) -> {};

        Runnable run = () -> {};

        final int cubeSize = 3;

        Cube cube = new Cube(cubeSize, cons, cons, run, run);

        concurrentAux(cube, cubeSize, 1000, 11);
    }

    @Test
    public void test7() throws Exception {
        System.err.println("This test will take a while...");

        BiConsumer<Integer, Integer> cons = (integer, integer2) -> {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };

        Runnable run = () -> {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };

        final int cubeSize = 10;

        Cube cube = new Cube(cubeSize, cons, cons, run, run);

        concurrentAux(cube, cubeSize, 5, 11);
    }

    @Test
    public void test8() throws Exception {
        System.err.println("This test will take a while...");
        BiConsumer<Integer, Integer> cons = (integer, integer2) -> {};

        Runnable run = () -> {};

        final int cubeSize = 500;

        Cube cube = new Cube(cubeSize, cons, cons, run, run);

        concurrentAux(cube, cubeSize, 1000, 1000);
    }

    @Test
    public void test9() throws Exception {
        System.err.println("This test will take a while...");
        BiConsumer<Integer, Integer> cons = (integer, integer2) -> {};

        Runnable run = () -> {};

        final int cubeSize = 1000;

        Cube cube = new Cube(cubeSize, cons, cons, run, run);

        concurrentAux(cube, cubeSize, 1000, 250);
    }

    private void addPermutation(Cube cube, Set<String> set, int[] order, int[] side, int[] layer) {
        try {
            for (int i = 0; i < order.length; ++i) {
                cube.rotate(side[order[i]], layer[order[i]]);
            }
            set.add(cube.show());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    private void generatePermsAux(int cubeSize, Set<String> possibleResults, int[] sideOrder, int[] layerOrder,
                                  boolean[] available, int[] order, int currentIndex) {

        for (int i = 0; i < available.length; ++i) {
            if (available[i]) {
                available[i] = false;
                order[currentIndex] = i;
                if (currentIndex == available.length - 1) {
                    addPermutation(
                            new Cube(cubeSize, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {}),
                            possibleResults, order, sideOrder, layerOrder
                    );
                }
                else {
                    generatePermsAux(cubeSize, possibleResults, sideOrder, layerOrder, available, order, currentIndex + 1);
                }
                available[i] = true;
            }
        }

    }

    private void generatePermutations(int cubeSize, Set<String> possibleResults, int[] sideOrder, int[] layerOrder) {
        boolean[] available = new boolean[sideOrder.length];
        int[] order = new int[sideOrder.length];
        for (int i = 0; i < sideOrder.length; ++i) {
            available[i] = true;
        }
        generatePermsAux(cubeSize, possibleResults, sideOrder, layerOrder, available, order, 0);
    }

    private void permutationTest(int cubeSize, int permutationLength, int testCount) throws Exception {
        Random rand = new Random();
        int[] sideOrder = new int[permutationLength];
        int[] layerOrder = new int[permutationLength];

        for (int i = 0; i < permutationLength; ++i) {
            sideOrder[i] = rand.nextInt(6);
        }
        for (int i = 0; i < permutationLength; ++i) {
            layerOrder[i] = rand.nextInt(cubeSize);
        }

        Set<String> possibleResults = new HashSet<>();
        generatePermutations(cubeSize, possibleResults, sideOrder, layerOrder);

        for (int i = 0; i < testCount; ++i) {
            Cube cube = new Cube(cubeSize, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {});

            Thread[] threads = new Thread[permutationLength];
            for (int j = 0; j < permutationLength; ++j) {
                int side = sideOrder[j];
                int layer = layerOrder[j];
                threads[j] = new Thread(() -> {
                    try {
                        cube.rotate(side, layer);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                threads[j].start();
            }

            for (int j = 0; j < permutationLength; ++j) {
                threads[j].join();
            }

            String cubeResult = cube.show();
            assertTrue(possibleResults.contains(cubeResult));
        }
    }

    // permutacje, sprawdzamy tylko dla 10 obrotow, bo 10! jest juz wystarczajaco duze
    @Test
    public void test10() throws Exception {
        final int cubeSize = 400;
        // larger numbers would result in a veeeery long test
        // because there are permutationLenght! permutations of the given length
        final int permutationLength = 7;
        permutationTest(cubeSize, permutationLength, 400);
    }

    @Test
    public void test11() throws Exception {
        permutationTest(3, 10, 20);
    }

    @Test
    public void test12() throws Exception {
        permutationTest(3, 7, 2000);
    }

    @Test
    public void test13() throws Exception {
        permutationTest(1000, 5, 2000);
    }

    // random order of operations on a cube

    private void checkIfCorrect(int cubeSize, String result) throws Exception {
        int[] colorCount = new int[6];
        for (int i = 0; i < 6; ++i) {
            colorCount[i] = 0;
        }

        for (int i = 0; i < result.length(); ++i) {
            ++colorCount[(int) result.charAt(i) - '0'];
        }
        for (int i = 0; i < 6; ++i) {
            if (colorCount[i] != cubeSize * cubeSize) {
                fail();
            }
        }
    }

    private void randomOrder(int cubeSize, BiConsumer<Integer, Integer> beforeRot, BiConsumer<Integer, Integer> afterRot,
                             Runnable beforeShow, Runnable afterShow, int testCount, int checkCount) throws Exception {

        Cube cube = new Cube(cubeSize, beforeRot, afterRot, beforeShow, afterShow);
        Random rand = new Random();
        Thread[] threads = new Thread[testCount];

        for (int i = 0; i < testCount; ++i) {
            if (rand.nextBoolean()) {
                threads[i] = new Thread(() -> {
                    try {
                        cube.rotate(rand.nextInt(6), rand.nextInt(cubeSize));
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                });
            }
            else {
                threads[i] = new Thread(() -> {
                    try {
                        cube.show();
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                });
            }
            threads[i].start();
        }

        for (int i = 0; i < checkCount; ++i) {
            checkIfCorrect(cubeSize, cube.show());
        }

        for (int i = 0; i < testCount; ++i) {
            threads[i].join();
        }
    }

    @Test
    public void test14() throws Exception {
        randomOrder(3, (p, q) -> {}, (p, q) -> {}, () -> {}, () -> {}, 500, 500);
    }

    @Test
    public void test15() throws Exception {
        randomOrder(
                150,
                (p, q) -> {
                    int sum = 0;
                    for (int i = 0; i < q; ++i) {
                        for (int j = 0; j < p; ++j) {
                            sum += j;
                        }
                    }
                },
                (p, q) -> {},
                () -> { System.out.println("Something"); },
                () -> {},
                10000,
                100
        );
    }

    @Test
    public void test16() throws Exception {

    }

    @Test
    public void test17() throws Exception {

    }

    @Test
    public void test18() throws Exception {

    }

    @Test
    public void test19() throws Exception {

    }

    @Test
    public void test20() throws Exception {

    }

}