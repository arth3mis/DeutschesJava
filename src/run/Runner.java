package run;

import main.OS;

import java.io.File;

public abstract class Runner {

    protected String customRunner;

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Runner newInstance(String customRunner) {
        if (OS.isWindows())
            return new RunnerWindows(customRunner);
        else
            return null;
    }

    public abstract boolean start(File mainClassFile);

}
