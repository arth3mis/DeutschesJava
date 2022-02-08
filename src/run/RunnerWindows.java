package run;

import main.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class RunnerWindows extends Runner {

    public RunnerWindows(String customRunner) {
        this.customRunner = customRunner;
    }

    @Override
    public void start(File mainClassFile, String[] args) {
        // custom runner set?
        String runnerCommand = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
        // build command that executes java in a standalone (/k) cmd window
        final String ARG_FLAG = "[ARGS]";
        String command = String.format(
                "start cmd.exe @cmd /k " +
                        "\"" +
                        "\"%s\" " +
                        "-classpath \"%s\" " +           // parent dir (absolute path; see Main.evaluateArgs())
                        "\"%s\"" +                       // file name
                        ARG_FLAG +                       // args (added later)
                        "&&echo.&&echo.&&pause&&exit" +  // '&' also works
                        "\"",
                runnerCommand,
                mainClassFile.getParent() == null ? "." : mainClassFile.getParent(),
                mainClassFile.getName());

        // add arguments
        command = command.replace(ARG_FLAG, formatArgs(args));

        // make batch file in user.dir to execute command and delete itself afterwards
        // executing the command directly does not find "start" or "cmd.exe"
        File batchFile;
        try {
            final String BATCH_FILE_NAME = "Pausen-Akro.bat";
            batchFile = new File(BATCH_FILE_NAME);
            if (batchFile.delete())
                Logger.log("Stapel-Datei '%s' wird überschrieben.", BATCH_FILE_NAME);
            if (!batchFile.createNewFile()) {
                throw new IOException("Datei existiert bereits");
            }
            FileWriter w = new FileWriter(batchFile);
            w.write(command);
            w.write("\ndel \"%~f0\"");
            w.close();
            Logger.log("Stapel-Datei fürs Rennen erstellt.");
        } catch (IOException e) {
            Logger.error("Fehler beim Erstellen der Stapel-Datei fürs Rennen: " + e.getMessage());
            return;
        }

        // execute batch file and wait for result
        try {
            Logger.log("Ausführung starten...");
            Process p = Runtime.getRuntime().exec("\"" + batchFile + "\"");
            // evaluate process return
            Logger.log("Ausführung mit '%s' beendet (Endwert: %d)", runnerCommand, p.waitFor());  // returns 1 (normal?)
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
        }
    }
}
