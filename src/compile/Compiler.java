package compile;

import main.Logger;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public record Compiler(String customCompiler) {

    public boolean start(File[] javaFiles) {
        // custom compiler set?
        if (customCompiler != null && !customCompiler.isEmpty()) {
            if (compileWithCommand(customCompiler, javaFiles))
                return true;
        }
        // try system compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            Logger.log("Kompilierung mit System-Kompilierer starten...");
            try {
                // stuff...
                StandardJavaFileManager fileManager =
                        compiler.getStandardFileManager(null, null, null);
                File pathRef = javaFiles[0].getAbsoluteFile().getParentFile();
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(pathRef));
                Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjects(javaFiles);

                // execute task
                if (compiler
                        .getTask(null, fileManager, null, null, null, compilationUnits1)
                        .call()) {
                    Logger.log("Kompilieren erfolgreich.");
                    return true;
                } else
                    Logger.error("Kompilieren mit System-Kompilierer fehlgeschlagen.");
            } catch (IOException | NullPointerException e) {
                Logger.error("Kompilierung mit System-Kompilierer fehlgeschlagen: %s", e.getMessage());
            }
        }
        // try global "javac" command
        return compileWithCommand("javac", javaFiles);
    }

    private boolean compileWithCommand(String compiler, File @NotNull [] javaFiles) {
        try {
            StringBuilder sb = new StringBuilder();
            for (File f : javaFiles)
                sb.append(" \"").append(f.toString()).append("\"");
            Logger.log("Kompilierung mit '%s' starten...", compiler);
            String command = "\"" + compiler + "\"" + sb;
            Process p = Runtime.getRuntime().exec(command);
            try {
                while (p.isAlive())
                    Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            Logger.log("Kompilierung durch Befehl beendet mit Rückgabewert: %d", p.exitValue());
            return true;
        } catch (IOException e) {
            Logger.error("Kompilierung durch Befehl fehlgeschlagen: %s", e.getMessage());
            return false;
        }
    }
}