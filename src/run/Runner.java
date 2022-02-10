package run;

import filesystem.Commander;
import main.Logger;
import main.Main;
import main.OS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public abstract class Runner {

    protected File mainClassFile;
    protected String[] args;

    protected String runnerCommand;
    protected List<String> commands;

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

    public Runner(File mainClassFile, String[] args, String customRunner) {
        this.mainClassFile = mainClassFile;
        this.args = args;
        // custom runner set?
        runnerCommand = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
        // command setup
        buildCommand();
    }

    protected void buildCommand() {
        commands = Commander.basicCommand();
        commands.addAll(basicRunCommand());
    }

    /**
     * @return (java_executable) -classpath (path) (main_class) [args]
     */
    protected List<String> basicRunCommand() {
        List<String> c = new ArrayList<>();
        c.add(Commander.replaceEnvVars(runnerCommand));
        c.add("-classpath");
        c.add(mainClassFile.getParent() == null ? "." : mainClassFile.getParent());
        c.add(mainClassFile.getName());
        if (args != null)
            c.addAll(Arrays.asList(args));
        return c;
    }

    /**
     * @return true if process launched correctly
     */
    public boolean start() {
        // execute command via process builder
        // route streams to let user interact with the program
        // (https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program)
        try {
            Logger.log("Ausführung mit %s starten...", Commander.formatCommand(runnerCommand));

            ProcessBuilder pb = Commander.createProcessBuilder(commands);
            final Process p = pb.start();

            System.out.println(Main.OUTPUT_SEP);
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
            System.out.println("\n".repeat(programEndNewLines) + Main.OUTPUT_SEP);

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
            Logger.error("Prozess-Fehler beim Ausführen mit %s: %s", Commander.formatCommand(runnerCommand), e.getMessage());
            return false;
        }
    }
}
