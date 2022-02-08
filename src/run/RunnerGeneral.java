package run;

import main.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

class RunnerGeneral extends Runner {

    public RunnerGeneral(String customRunner) {
        this.customRunner = customRunner;
    }

    @Override
    public void start(File mainClassFile, String[] args) {
        // custom runner set?
        String runnerPath = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
        // build command that executes java
        final String ARG_FLAG = "[ARGS]";
        String command = String.format(
                        "\"%s\" " +
                        "-classpath \"%s\" " +           // parent dir (absolute path; see Main.evaluateArgs())
                        "\"%s\"" +                       // file name
                        ARG_FLAG,                        // args (added later)
                runnerPath,
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
            Process p = pb.start();

            System.out.println("\n--------");
            p.getInputStream().transferTo(System.out);
            System.out.println("\n--------");

            // evaluate process return
            int exitValue = p.waitFor();
            Logger.log("Ausführung beendet (Endwert: " + exitValue + ")");  // returns 1 (normal?)
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
        }
    }
}
