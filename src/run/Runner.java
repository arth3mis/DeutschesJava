package run;

import filesystem.Commander;
import main.Logger;
import main.OS;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public abstract class Runner {

    protected String customRunner;
    protected String command, runnerCommand;
    protected File mainClassFile;
    protected String[] args;

    public static final String programBorder = "_".repeat(60);
    protected final int programEndNewLines = 2;

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Runner newInstance(File mainClassFile, String[] runArgs, String customRunner) {
        if (OS.isWindows())
            return new RunnerWindows(mainClassFile, runArgs, customRunner);
        else if (OS.isLinux())
            return new RunnerLinux(mainClassFile, runArgs, customRunner);
        else if (OS.isMac())
            return new RunnerMac(mainClassFile, runArgs, customRunner);
        return null;
    }

    protected void buildCommand() {
        // custom runner set?
        runnerCommand = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
        // build command that executes java
        command = Commander.build(runnerCommand, args,
                "-classpath %s %s",
                mainClassFile.getParent() == null ? "." : mainClassFile.getParent(),
                mainClassFile.getName());
    }

    protected String formatArgs(String[] args) {
        StringBuilder sbArgs = new StringBuilder();
        if (args != null && args.length > 0) {
            for (String arg : args)
                sbArgs.append(" \"").append(arg).append("\"");
        }
        return sbArgs.toString();
    }

    /**
     * @return true if process launched correctly
     */
    public boolean start() {
        // execute command via process builder
        // route streams to let user interact with the program
        // (https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program)
        try {
            Logger.log("Ausführung mit %s starten...", Commander.buildMain(runnerCommand));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            final Process p = pb.start();

            System.out.println(programBorder);
            /////////////////////////////////////////////////////////////////

            // user input logic
            final Scanner scanner = new Scanner(System.in);

            new Thread(() -> {
                while (p.isAlive()) {
                    try {
                        if (scanner.hasNext()) {
                            String input = scanner.nextLine() + "\n";
                            p.outputWriter().write(input);
                            p.outputWriter().flush();
                        }
                    } catch (IOException | NullPointerException | IllegalStateException ignored) {
                        //Logger.error("DEBUG FEHLER: %s", ignored.getMessage());
                    }
                }
            }).start();

            // program output logic
            p.getInputStream().transferTo(System.out);

            /////////////////////////////////////////////////////////////////
            System.out.println("\n".repeat(programEndNewLines) + programBorder);

            // evaluate process return (probably only 0 or 1, but useful to signal successful termination)
            int exitValue = p.waitFor();
            System.out.printf("Programm beendet mit Endwert %d\n\n", exitValue);

            // user input necessary to kill scanner thread
            System.out.println("[ENTER] Fertig");
            scanner.close();
            try {
                scanner.nextLine();
                // this ^^^^^^^^^^ will trigger an exception because scanner is closed
                // causes thread to break out of scanner.hasNext()
            } catch (IllegalStateException ignored) {
            }
            return true;
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen mit %s: %s", Commander.buildMain(runnerCommand), e.getMessage());
            return false;
        }
    }
}
