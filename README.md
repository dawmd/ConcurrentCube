# Concurrent Cube
The program implements the two basic operations on the Rubik's cube:
* `rotate(int side, int layer)` -- rotating (clockwise) the `layer`-th layer having the `side`-th side at the front,
* `show()` -- returning a string representing the current state of the cube.

Indexing the sides of the cube:
* 0 -- TOP,
* 1 -- LEFT,
* 2 -- FRONT,
* 3 -- RIGHT,
* 4 -- BACK,
* 5 -- DOWN.

The `i`-th layer looking from side `X`'s perspective is the fields in the `i`-th row from the side `X`. Layers are indexed from `0` to `n - 1`, where `n` stands for the size of the cube.

![Representation of a layer](https://github.com/dawmd/ConcurrentCube/blob/master/CUBE.png?raw=true)

### TODO
* Take care of `InterruptedException`s.
