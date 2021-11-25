public class Test {
    Integer a = 3;

    public void cos() {
        Integer b = a;
        b = 1;
    }

    public void show() {
        System.out.println(a);
    }

    public static void main(String[] args) {
        Test test = new Test();
        test.cos();
        test.show();
    }
}
