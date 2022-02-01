package compile;

import main.OS;

public abstract class Compiler {

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Compiler newInstance() {
        if (OS.isWindows())
            return new CompilerWindows();
        else
            return null;
    }
}