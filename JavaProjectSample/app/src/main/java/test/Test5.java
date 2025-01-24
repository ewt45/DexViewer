package test;

import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;

public class Test5 {
    public void test() {
        int i = 1;
        Log.d("aaa", 3*i + "");
        
    }

    public void test1_普通trycatch() {
        try {
            System.out.println("try");
        } catch (RuntimeException e) {
            System.out.println("catch");
        }
    }

    public void test2_加finally() {
        try {
            System.out.println("try");
        } catch (RuntimeException e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
    }

    public void test2$1_加外部finally() {
        try {
            try {
                System.out.println("try");
            } catch (RuntimeException e) {
                System.out.println("catch");
            }
        } finally {
            System.out.println("finally");
        }
    }

    public void test2$2_加finally嵌套() {
        try {
            System.out.println("outer try");
            try {
                System.out.println("inner try");
            } catch (RuntimeException e) {
                System.out.println("inner catch");
            } finally {
                System.out.println("inner finally");
            }
        } catch (RuntimeException e) {
            System.out.println("outer catch");
        } finally {
            System.out.println("outer finally");
        }
    }

    public void test2$3_加finally_多catch() {
        try {
            FileInputStream fis = new FileInputStream("file");
            System.out.println("try");
        } catch (RuntimeException e) {
            System.out.println("catch");
        } catch (IOException e) {
            System.out.println("catch_io");
        } finally {
            System.out.println("finally");
        }
    }

    boolean flag;
    public void test3_try多一行() {
        try {
            System.out.println("try");
            flag = true;
        } catch (RuntimeException e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
    }


    public void test3$1_try多一行是throw() {
        try {
            System.out.println("try");
            throw new RuntimeException("throw");
        } catch (RuntimeException e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
    }

    public void test3$2_try多一行是throw放中间() {
        try {
            System.out.println("try");
            if (!flag) {
                throw new RuntimeException("throw");
            }
            flag = true;
        } catch (RuntimeException e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
    }


    public void test4_try外有一行() {
        try {
            System.out.println("try");
        } catch (RuntimeException e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
        System.out.println("Code after try-catch-finally");
    }

    public void test5_try多一行_try外有一行() {
        try {
            System.out.println("try");
            flag = true;
        } catch (RuntimeException e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
        System.out.println("Code after try-catch-finally");
    }

    public void test6_自己捕捉throwable() {
        try {
            System.out.println("try");
        } catch (Throwable e) {
            System.out.println("catch");
        }
    }

    public void test7_自己捕捉throwable和finally同时存在() {
        try {
            System.out.println("try");
        } catch (Throwable e) {
            System.out.println("catch");
        } finally {
            System.out.println("finally");
        }
    }

    //try with resources
    // try with resources finally
}
