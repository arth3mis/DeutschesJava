package compile;

import filesystem.Filer;
import filesystem.JCmd;
import main.Logger;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public record Compiler(String customCompiler) {

    public boolean start(File[] javaFiles) {
        // file does not exist? (mostly happens with -K option)
        // only check 1st file to give clean error in the most common scenario,
        // later files are checked by the compiler
        if (!javaFiles[0].isFile()) {
            Logger.error("Kompilieren abgebrochen, Java-Datei existiert nicht");
            return false;
        }

        // custom compiler set?
        if (customCompiler != null && !customCompiler.isEmpty()) {
            return compileWithCommand(customCompiler, javaFiles);
        }

        // try system compiler if no custom compiler set
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            Logger.log("Kompilierung mit System-Kompilierer starten...");
            try {
                // use UTF-8 charset to correctly process [ÄÖÜß]
                StandardJavaFileManager fileManager =
                        compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
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
        Logger.log("Kompilierung mit %s starten...", compiler);

        // possible encoding problems? -> fixed by passing encoding arg to javac
//        if (!Filer.canJvmEncodeGerman()) {
//            // try to rewrite all 'äöüß' characters
//            if (Filer.rewriteFiles(javaFiles))
//                Logger.warning("Java-Dateien wurden ASCII-konform überschrieben.");
//            else
//                Logger.error("Java-Dateien konnten nicht alle ASCII-konform überschrieben werden.");
//        }

        try {
            ProcessBuilder pb = JCmd.get().createProcessBuilder(
                    compiler, Stream.concat(
                            // for systems like windows cmd where UTF-8 is not standard
                            Stream.of("-encoding", "UTF-8"),
                            Arrays.stream(javaFiles).map(File::getPath)).toList());
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
        } catch (NullPointerException e) {
            Logger.error("Kompilierung mit '%s' fehlgeschlagen.", compiler);
        }
        return false;
    }
}