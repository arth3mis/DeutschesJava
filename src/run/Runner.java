package run;

import main.OS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.*;

public abstract class Runner {

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Runner newInstance() {
        Scanner sc = new Scanner(System.in); sc.next();
        if (OS.isWindows())
            return new RunnerWindows();
        else
            return null;
    }

    public String start(String[] args) {
        return null;
    }

}
