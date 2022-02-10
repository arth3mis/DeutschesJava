package compile;

import filesystem.Commander;
import main.Logger;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public record Compiler(String customCompiler) {

    public boolean start(File[] javaFiles) {
        // custom compiler set?
        if (customCompiler != null && !customCompiler.isEmpty()) {
            return compileWithCommand(customCompiler, javaFiles);
        }

        // try system compiler if no custom compiler set
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
                } else {
                    Logger.error("Kompilieren mit System-Kompilierer fehlgeschlagen.");
                    return false;
                }
            } catch (IOException | NullPointerException e) {
                Logger.error("Kompilierung mit System-Kompilierer fehlgeschlagen: %s", e.getMessage());
            }
        }

        // try global "javac" command if system compiler not found/had error (other than compilation failed)
        return compileWithCommand("javac", javaFiles);
    }

    private boolean compileWithCommand(String compiler, File @NotNull [] javaFiles) {
        try {
            Logger.log("Kompilierung mit %s starten...", Commander.formatCommand(compiler));
            List<String> commands = Commander.basicCommand();
            commands.add(Commander.replaceEnvVars(compiler));
            commands.addAll(Arrays.stream(javaFiles).map(File::getPath).toList());

            ProcessBuilder pb = Commander.createProcessBuilder(commands);
            Process p = pb.start();

            // redirect output from process
            p.getInputStream().transferTo(System.err);

            // wait for process to finish
            int exitValue = -1;
            try {
                exitValue = p.waitFor();
            } catch (InterruptedException ignored) {
            }

            // must return 0 for success (1 indicates errors during compilation)
            if (exitValue == 0)
                Logger.log("Kompilierung erfolgreich.");
            else
                Logger.error("Kompilierung mit '%s' fehlgeschlagen.", compiler);
            return exitValue == 0;
        } catch (IOException e) {
            Logger.error("Kompilierung mit '%s' fehlgeschlagen: %s", compiler, e.getMessage());
            return false;
        }
    }
}