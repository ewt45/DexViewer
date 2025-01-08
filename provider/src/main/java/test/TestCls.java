package test;

public class TestCls {
    public boolean f;

    @SuppressWarnings("ConstantConditions")
    private boolean test(Object obj) {
        this.f = false;
        try {
            exc(obj);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.f = true;
        }
        return this.f;
    }

    private static boolean exc(Object obj) throws Exception {
        if (obj == null) {
            throw new Exception("test");
        }
        return (obj instanceof String);
    }

}
