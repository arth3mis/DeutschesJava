package cfr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Runner {
    static File f;
    public static String start(String[] args) {
        if (args == null || args.length == 0)
            return "no args";
        f = new File(args[0]);
        if (!f.exists() || !f.isFile())
            return "args are not files";
        try {
            String s = "start cmd.exe @cmd /k \"\"C:\\Program Files\\Java\\jre1.8.0_241\\bin\\java.exe\" -cp \""+f.getParent()+"\" \""+f.getName().substring(0, f.getName().indexOf("."))+"\"&echo.&echo.&pause&exit\"";
            File x = new File(f.getParent(), "crf_bat_tmp.bat");
            x.delete();
            if (!x.createNewFile())
                return "error creating batch file";
            FileWriter w = new FileWriter(x);
            w.write(s);
            w.write("\ndel \"%~f0\"");
            w.close();
            Process p = Runtime.getRuntime().exec("\""+x.toString()+"\"");
            while (p.isAlive());
            return "done (exit value: " + p.exitValue() + ")";
        } catch (IOException e) {
            return "process error: " + e.getMessage();
        }
    }
}
