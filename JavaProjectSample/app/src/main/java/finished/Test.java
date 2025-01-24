package finished;

public class Test {
    public void test(int i, int i2) {
        if (i == 1) {
            boolean b = false;
            if (i2 > 5) {
                i++;
                b = true;
            }
            test2(b);
        } else {
            boolean b = i < 3;
            i ++;
            if (b) {
                test3(i);
            } else {
                test3(i2);
            }
        }
    }
    public void test2(boolean b) {

    }

    public void test3 (int i) {

    }
}
