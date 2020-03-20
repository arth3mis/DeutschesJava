package cfr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class Runner {

    static File f;

    public static String start(String[] args) {
        if (args == null || args.length == 0)
            return "Keine Argumente gegeben (interner Fehler)";
        f = new File(args[0]);
        if (!f.exists() || !f.isFile())
            return "Argument ist keine Datei";

        String os = System.getProperty("os.name", "unspecified").toLowerCase(Locale.ENGLISH);
        // MAC
        if (os.contains("mac") || os.contains("darwin")) {
            return "'rennen' ist noch nicht möglich mit Betriebssystem: Mac";
        }
        // WINDOWS
        else if (os.contains("win")) {
            try {
                String s = "start cmd.exe @cmd /k \"\"C:\\Program Files\\Java\\jre1.8.0_241\\bin\\java.exe\" -cp \""+f.getParent()+"\" \""+f.getName().substring(0, f.getName().indexOf("."))+"\"&echo.&echo.&pause&exit\"";
                File x = new File(f.getParent(), "crf_bat_tmp.bat");
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
        // LINUX
        else if (os.contains("nux")) {
            return "'rennen' ist noch nicht möglich mit Betriebssystem: Linux";
        }
        // OTHER
        else {
            return "'rennen' ist noch nicht möglich mit Betriebssystem: WAS ZUR HÖLLE HAST DU FÜR EIN BETRIEBSSYSTEM???";
        }
    }
}
