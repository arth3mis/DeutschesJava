package run;

import main.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class RunnerWindows extends Runner {

    private final String BATCH_FILE_NAME = "Pausen-Akro.bat";

    public RunnerWindows(String customRunner) {
        this.customRunner = customRunner;
    }

    @Override
    public boolean start(File mainClassFile, String[] args) {
        // custom runner set?
        String runnerPath = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
        // build command that executes java in a standalone (/k) cmd window
        final String ARG_FLAG = "[ARGS]";
        String command = String.format(
                "start cmd.exe @cmd /k " +
                        "\"" +
                        "\"%s\" " +
                        "-classpath \"%s\" " +           // parent dir (relative to user.dir)
                        "\"%s\"" +                       // file name
                        ARG_FLAG +                       // args (added later)
                        "&&echo.&&echo.&&pause&&exit" +  // '&' also works
                        "\"",
                runnerPath,
                mainClassFile.getParent() == null ? "." : mainClassFile.getParent(),
                mainClassFile.getName());

        // add arguments
        StringBuilder sbArgs = new StringBuilder();
        if (args != null && args.length > 0) {
            for (String arg : args)
                sbArgs.append(" \"").append(arg).append("\"");
        }
        command = command.replace(ARG_FLAG, sbArgs.toString());

        // make batch file in user.dir to execute command and delete itself afterwards
        // executing the command directly does not find "start" or "cmd.exe"
        File batchFile;
        try {
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
            return false;
        }

        // execute batch file and wait for result
        try {
            Logger.log("Ausführung starten...");
            Process p = Runtime.getRuntime().exec("\"" + batchFile + "\"");
            try {
                while (p.isAlive())
                    Thread.sleep(10);
            } catch (InterruptedException ignored) {}

            // evaluate process return
            Logger.log("Ausführung beendet (Endwert: " + p.exitValue() + ")");  // returns 1 (normal?)
            return true;
        } catch (IOException | SecurityException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
            return false;
        }
    }
}
