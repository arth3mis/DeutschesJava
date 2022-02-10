package run;

import filesystem.Commander;
import main.Logger;
import main.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

class RunnerWindows extends Runner {

    public RunnerWindows(File mainClassFile, String[] args, String customRunner) {
        super(mainClassFile, args, customRunner);
    }

    @Override
    protected void buildCommand() {
        commands = new ArrayList<>();

        // special flag set?
        if (Main.Flag.SPECIAL_RUN.set) {
            // build command that executes java in a standalone (/k) cmd window
            // list is joined later (without spaces), it is not passed to a process builder
            commands.add("start cmd.exe /c ");  // original: "start cmd.exe @cmd /k " (the /k is better for debugging, but no idea about @cmd)
            commands.add("\"");
            // do escape of basic run command now (tries to replace env vars everywhere, it's easier this way though)
            commands.add(String.join(" ", basicRunCommand().stream().map(Commander::formatCommand).toList()));
            commands.add("&&echo.".repeat(programEndNewLines));
            commands.add("&&echo " + Main.OUTPUT_SEP);
            commands.add("&&pause&&exit");
            commands.add("\"");
        } else
            super.buildCommand();
    }

    @Override
    public boolean start() {
        return Main.Flag.SPECIAL_RUN.set ? startSpecial() : super.start();
    }

    private boolean startSpecial() {
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
            w.write(String.join("", commands));
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
                    .exec(Commander.escape(batchFile.getPath()))
                    .waitFor();
            Logger.log("Ausführung der Stapel-Datei beendet.");
            return true;
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
            return false;
        }
    }
}
