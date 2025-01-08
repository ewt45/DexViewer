package finished;

import java.io.File;
import java.io.IOException;

public class TestIfInTry {
    public File dir;

    public int test() {
        try {
            int a = f();
            if (a != 0) {
                return a;
            }
        } catch (Exception e) {
            // skip
        }
        try {
            f();
            return 1;
        } catch (IOException e) {
            return -1;
        }
    }

    private int f() throws IOException {
        return 0;
    }
}