package run;

import filesystem.Filer;
import filesystem.JCmd;
import main.Logger;
import main.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class RunnerWindows extends Runner {

    public RunnerWindows(File mainClassFile, String[] args, String customRunner) {
        super(mainClassFile, args, customRunner);
    }

    @Override
    public boolean start() {
        // file does not exist? (mostly happens with -R option)
        if (!mainClassFile.isFile() && !Filer.refactorExtension(mainClassFile, "class").isFile()) {
            Logger.error("Rennen abgebrochen, Klassen-Datei existiert nicht");
            return false;
        }

        return Main.Flag.SPECIAL_RUN.set ? startSpecial() : super.start();
    }

    private boolean startSpecial() {
        // build command that executes java in a standalone (/k) cmd window
        String command =
                "start cmd.exe /c " // original: "start cmd.exe @cmd /k " (the /k is better for debugging, but no idea about @cmd)
                        + "\""
                        + JCmd.get().formatCommand(runnerCommand) + " "
                        + String.join(" ", javaCommandArguments().stream().map(JCmd.get()::escape).toList())
                        + "&&echo.".repeat(programEndNewLines)
                        + "&&echo " + Main.OUTPUT_SEP
                        + "&&pause&&exit"
                        + "\"";

        // make batch file in user.dir to execute command and delete itself afterwards
        // executing the command directly does not find "start" or "cmd.exe"
        File batchFile = new File("Pausen-Akro.bat");
        try {
            if (batchFile.delete())
                Logger.log("Stapel-Datei '%s' wird überschrieben.", batchFile.getName());
            if (!batchFile.createNewFile()) {
                throw new IOException("Datei konnte nicht überschrieben werden.");
            }
            // write commands to file (join list without spaces)
            FileWriter w = new FileWriter(batchFile);
            w.write(command);
            w.write("\ndel \"%~f0\"");
            w.close();
            Logger.log("Stapel-Datei fürs Rennen erstellt.");
        } catch (IOException e) {
            Logger.error("Fehler beim Erstellen der Stapel-Datei fürs Rennen: " + e.getMessage());
            return false;
        }

        // execute batch file and wait for return (not return of java program)
        try {
            Logger.log("Ausführung starten...");
            Runtime.getRuntime()
                    .exec(JCmd.get().escape(batchFile.getPath()))
                    .waitFor();
            Logger.log("Ausführung der Stapel-Datei beendet.");
            return true;
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
            return false;
        }
    }
}
