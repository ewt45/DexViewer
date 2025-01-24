package finished;

public class Test2 {
    //???为什么写成StateClass就无法解析，写成T就可以解析？？？
    //分开到单独java文件，也不行。
    //和字母个数有关系吗？其他单字母也不行，应该就是只有T可以
    //不继承父类的时候不会触发
    //终于知道了，原来是因为父类的泛型是T。必须要父类和子类的泛型名字一样，才能识别出泛型继承的基类。
    //要求子类有自己的泛型，且将泛型填充进父类泛型，且子类泛型是父类泛型的类型子集（相等也行），此时父类函数返回的父类泛型应该可以转化为子类泛型但转化失败

    public abstract static class Class2<S extends I1 & I2> extends Parent2<S> {
        public void test() {
            S s = get();
            s.i1();
            s.i2();
        }
    }

    static class Parent2<T extends I1> {
        T t;
        protected T get() {
            return t;
        }
    }

    interface I1 {
        void i1();
    }
    interface I2 {
        void i2();
    }
}

