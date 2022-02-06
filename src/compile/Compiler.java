package compile;

import main.OS;

import java.io.File;

public abstract class Compiler {

    protected String customCompiler;

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Compiler newInstance(String customCompiler) {
        if (OS.isWindows())
            return new CompilerWindows(customCompiler);
        else
            return null;
    }

    public abstract boolean start(File[] javaFiles);
}