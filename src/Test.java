import concurrentcube.Cube;

import java.util.function.BiConsumer;

public class Test {

    public static void main(String[] args) {
        Cube cube = new Cube(3, new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer integer, Integer integer2) {
                return;
            }
        }, new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer integer, Integer integer2) {
                return;
            }
        }, new Runnable() {
            @Override
            public void run() {
                return;
            }
        }, new Runnable() {
            @Override
            public void run() {
                return;
            }
        });

        try {
            cube.rotate(0, 0);
            cube.rotate(1, 1);
            cube.rotate(2, 2);

            System.out.println(cube.show());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
