package finished;


import android.app.Activity;
import android.content.Intent;


/* loaded from: /home/cloud/codes/jadx反编译/exagear windows 3.0.1/反编译错误/4 代码块位置错误/0 原类.dex */
public class Test3 extends Activity {
    ViewOfXServer viewOfXServer;
    private static final Object ENABLE_TRACING_METHODS = false;

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:10:0x0065  */
    /* JADX WARN: Removed duplicated region for block: B:8:0x0064 A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected void onCreate() {
        ViewFacade viewFacade;
        Object applicationState = new Object();
        Object component = new Object();
        String s = "";
        Class cls = (Class) new Intent().getSerializableExtra("facadeclass");
        if (cls != null) {
            try {
                viewFacade = (ViewFacade) cls.getDeclaredConstructor(Object.class, Object.class).newInstance(component, applicationState);
            } catch (Exception unused) {
                System.err.println(ENABLE_TRACING_METHODS);
                viewFacade = null;
            }
        } else {
            viewFacade = null;
        }
        getWindow().addFlags(128);
        getWindow().addFlags(4194304);
//        setContentView(R.layout.activity_main);
        if (checkForSuddenDeath()) {
            this.viewOfXServer = new ViewOfXServer(this, component, viewFacade, applicationState);
        }
    }

    private boolean checkForSuddenDeath() {
        return true;
    }


    public static class ViewFacade {

    }


    public static class ViewOfXServer {

        public ViewOfXServer(Test3 xServerDisplayActivity, Object component, ViewFacade viewFacade, Object applicationState) {
        }
    }
}