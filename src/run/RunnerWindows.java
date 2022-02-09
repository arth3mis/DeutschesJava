package run;

import filesystem.Commander;
import main.Logger;
import main.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class RunnerWindows extends Runner {

    public RunnerWindows(File mainClassFile, String[] args, String customRunner) {
        this.mainClassFile = mainClassFile;
        this.args = args;
        this.customRunner = customRunner;
        buildCommand();
    }

    @Override
    protected void buildCommand() {
        // standard command build
        super.buildCommand();

        // special flag set?
        if (Main.Flag.SPECIAL_RUN.set) {
            // build command that executes java in a standalone (/k) cmd window
            command = String.format(
                    "start cmd.exe @cmd /k \"%s\"",
                    Commander.build(runnerCommand, args,
                            "%s " +
                            "-classpath %s " +           // parent dir (absolute path; see Main.evaluateArgs())
                            "%s" +                       // file name
                            "&&echo.".repeat(programEndNewLines) +
                            "&&echo "+programBorder +    // '&' also works
                            "&&pause&&exit" +
                            "\"",
                            mainClassFile.getParent() == null ? "." : mainClassFile.getParent(),
                            mainClassFile.getName())
            );
        }
    }

    @Override
    public boolean start() {
        // special run?
        if (Main.Flag.SPECIAL_RUN.set)
            return startSpecial();
        else
            return super.start();
    }

    private boolean startSpecial() {
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
            return false;
        }

        // execute batch file and wait for result
        try {
            Logger.log("Ausführung starten...");
            Process p = Runtime.getRuntime().exec("\"" + batchFile + "\"");
            // evaluate process return
            int exitValue = p.waitFor();
            Logger.log("Ausführung der Stapel-Datei beendet.");
            return true;
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
            return false;
        }
    }
}
