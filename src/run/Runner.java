package run;

import main.OS;

import java.io.File;

public abstract class Runner {

    protected String customRunner;
    protected String batchName = "Pausen-Akro.bat";

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Runner newInstance(String customRunner) {
        if (OS.isWindows())
            return new RunnerWindows(customRunner);
        else
            return null;
    }

    public String start(File file) {  // todo change signature
        return null;
    }

}
