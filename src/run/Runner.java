package run;

import main.Main;
import main.OS;

import java.io.File;

public abstract class Runner {

    protected String customRunner;

    protected String programBorder = "_".repeat(60);
    protected int programEndNewLines = 2;

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Runner newInstance(String customRunner) {
        if (Main.Flag.SPECIAL_RUN.set) {
            if (OS.isWindows())
                return new RunnerWindows(customRunner);
        }
        return new RunnerGeneral(customRunner);
    }

    /**
     * @param mainClassFile program entry point
     * @param args launch arguments for program
     * @return true if process launched correctly
     */
    public abstract boolean start(File mainClassFile, String[] args);

    protected String formatArgs(String[] args) {
        StringBuilder sbArgs = new StringBuilder();
        if (args != null && args.length > 0) {
            for (String arg : args)
                sbArgs.append(" \"").append(arg).append("\"");
        }
        return sbArgs.toString();
    }

}
