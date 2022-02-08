package run;

import main.Logger;

import java.io.*;
import java.util.Scanner;

class RunnerGeneral extends Runner {

    public RunnerGeneral(String customRunner) {
        this.customRunner = customRunner;
    }

    @Override
    public void start(File mainClassFile, String[] args) {
        // custom runner set?
        String runnerCommand = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
        // build command that executes java
        final String ARG_FLAG = "[ARGS]";
        String command = String.format(
                        "\"%s\" " +
                        "-classpath \"%s\" " +           // parent dir (absolute path; see Main.evaluateArgs())
                        "\"%s\"" +                       // file name
                        ARG_FLAG,                        // args (added later)
                runnerCommand,
                mainClassFile.getParent() == null ? "." : mainClassFile.getParent(),
                mainClassFile.getName());

        // add arguments
        command = command.replace(ARG_FLAG, formatArgs(args));

        // execute command via process builder (
        // route streams to let user interact with the program
        try {
            Logger.log("Ausführung starten...");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            String programBorders = "_".repeat(40);

            final Process p = pb.start();

            System.out.println(programBorders);
            /////////////////////////////////////////////////////////////////
            final Scanner scanner = new Scanner(System.in);
            Thread th = new Thread(() -> {
                while (p.isAlive()) {
                    try {
                        if (scanner.hasNext()) {
                            String input = scanner.nextLine() + "\n";
                            p.outputWriter().write(input);
                            p.outputWriter().flush();
                        }
                    } catch (IOException | NullPointerException ignored) {
                        //Logger.error("DEBUG FEHLER: %s", ignored.getMessage());
                    }
                }
            });
            th.start();

            p.getInputStream().transferTo(System.out);
            /////////////////////////////////////////////////////////////////
            System.out.println("\n" + programBorders);

            // user input necessary to kill thread
            System.out.print("[ENTER] Beenden ");
            scanner.close();
            try {
                scanner.nextLine();
            } catch (IllegalStateException ignored) {
            }

            // evaluate process return
            int exitValue = p.waitFor();
            Logger.log("Ausführung mit '%s' beendet (Endwert: %d)", runnerCommand, exitValue);
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen mit '%s': %s", runnerCommand, e.getMessage());
        }
    }
}
