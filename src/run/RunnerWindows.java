package run;

import main.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class RunnerWindows extends Runner {

    public RunnerWindows(String customRunner) {
        this.customRunner = customRunner;
    }

    public boolean start(File file, String fileParentDir) {
        try {
            // custom runner set?
            String runnerPath = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
            // build command
            String s = String.format(
                    "start cmd.exe @cmd /k " +
                    "\"" +
                            "\"%s\" " +
                            "-cp \"%s\" " +  // parentDir
                            "\"%s\"" +       // file
                            "&echo.&echo.&pause&exit" +
                    "\"",
                    runnerPath,
                    fileParentDir,
                    file.getPath());

            // make batch file to execute command and delete itself afterwards todo direct execution possible?
            File x = new File(batchName);
            x.delete();
            if (!x.createNewFile()) {
                Logger.error("Fehler beim Erstellen der Stapel-Datei");
                return false;
            }
            FileWriter w = new FileWriter(x);
            w.write(s);
            w.write("\ndel \"%~f0\"");
            w.close();

            // execute batch file and wait for result
            Process p = Runtime.getRuntime().exec("\"" + x + "\"");
            try {
                while (p.isAlive())
                    Thread.sleep(10);
            } catch (InterruptedException ignored) {}

            // evaluate process return
            Logger.log("Ausführung erledigt (Endwert: " + p.exitValue() + ")");
            if (p.exitValue() == 0)
                return true;
        } catch (IOException e) {
            Logger.error("Prozess-Fehler beim Ausführen: " + e.getMessage());
        }
        return false;
    }
}
