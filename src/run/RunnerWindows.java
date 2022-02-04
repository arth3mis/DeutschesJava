package run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class RunnerWindows extends Runner {

    public String start(String[] args) {
        if (args == null || args.length == 0)
            return "Keine Argumente gegeben (interner Fehler)";
        File f = new File(args[0]);
        if (!f.exists() || !f.isFile())
            return "Argument ist keine Datei";

        try {
            // TODO: make option to set path which is then saved in appdata; could also search PATH for java.../bin
            String s = "start cmd.exe @cmd /k \"\"C:\\Program Files\\Java\\jdk-17\\bin\\java.exe\" -cp \""+f.getParent()+"\" \""+f.getName().substring(0, f.getName().lastIndexOf('.'))+"\"&echo.&echo.&pause&exit\"";
            File x = new File(f.getParent(), "cfr_bat_tmp.bat");
            x.delete();
            if (!x.createNewFile())
                return "Fehler beim Erstellen der Stapel-Datei";
            FileWriter w = new FileWriter(x);
            w.write(s);
            w.write("\ndel \"%~f0\"");
            w.close();
            Process p = Runtime.getRuntime().exec("\""+x.toString()+"\"");
            while (p.isAlive());
            return "Erledigt (Endwert: " + p.exitValue() + ")";
        } catch (IOException e) {
            return "Prozess-Fehler: " + e.getMessage();
        }
    }
}
