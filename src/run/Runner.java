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
            return new RunnerGeneral(customRunner);// debug only
        else if (OS.isMac())
            return new RunnerGeneral(customRunner);
        else if (OS.isLinux())
            return new RunnerGeneral(customRunner);
        else
            return null;
    }

    public abstract void start(File mainClassFile, String[] args);

    protected String formatArgs(String[] args) {
        StringBuilder sbArgs = new StringBuilder();
        if (args != null && args.length > 0) {
            for (String arg : args)
                sbArgs.append(" \"").append(arg).append("\"");
        }
        return sbArgs.toString();
    }

}
