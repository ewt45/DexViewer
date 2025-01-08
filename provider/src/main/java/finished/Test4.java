package finished;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Test4 {
    private static final String PROPERTIES_FILE = "";
    private static final String TAG = "";
    private final CountDownLatch mInitializedLatch = new CountDownLatch(1);
    public int mC2KServerPort = 0;
    private String mSuplServerHost = "";
    public int mSuplServerPort = 0;
    private String mC2KServerHost = "";

    public Test4() {
        Properties mProperties = new Properties();
        try {
            File file = new File(PROPERTIES_FILE);
            FileInputStream stream = new FileInputStream(file);
            mProperties.load(stream);
            stream.close();

            mSuplServerHost = mProperties.getProperty("SUPL_HOST");
            String portString = mProperties.getProperty("SUPL_PORT");
            if (mSuplServerHost != null && portString != null) {
                try {
                    mSuplServerPort = Integer.parseInt(portString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "unable to parse SUPL_PORT: " + portString);
                }
            }

            mC2KServerHost = mProperties.getProperty("C2K_HOST");
            portString = mProperties.getProperty("C2K_PORT");
            if (mC2KServerHost != null && portString != null) {
                try {
                    mC2KServerPort = Integer.parseInt(portString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "unable to parse C2K_PORT: " + portString);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not open GPS configuration file " + PROPERTIES_FILE);
        }

        Thread mThread = new Thread();
        mThread.start();
        while (true) {
            try {
                mInitializedLatch.await();
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class Log {
        public static void e(String tag, String s) {
        }
    }
}