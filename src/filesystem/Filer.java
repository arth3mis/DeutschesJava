package filesystem;

import main.Logger;
import main.Main;
import main.OS;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Filer {

    public static boolean checkExtension(File file, String extension) {
        int extDot = file.getName().lastIndexOf('.');
        return extension.isEmpty() && extDot < 1 || file.getName().substring(extDot + 1).equals(extension);
    }

    public static File[] refactorExtension(File[] files, String newExtension) {
        List<File> newFiles = new ArrayList<>();
        for (File f : files)
            newFiles.add(refactorExtension(f, newExtension));
        return newFiles.toArray(new File[0]);
    }

    public static File refactorExtension(File file, String newExtension) {
        // make new file name
        String name = file.getName();
        int extDot = name.lastIndexOf('.');
        String newName = name.substring(0,
                extDot > 0 ? extDot : name.length()) + (newExtension.isEmpty() ? "" : ".") + newExtension;
        // create file from path and name
        return new File(file.getParentFile(), newName);  // parent == null is handled by constructor
    }

    public static boolean deleteFiles(File @Nullable [] files) {
        if (files == null) return true;
        boolean allSuccess = true;
        for (File f : files) {
            allSuccess &= f.delete();
        }
        return allSuccess;
    }

    public static boolean deleteInnerClassFiles(File[] files) {
        boolean allSuccess = true;
        for (File f : files) {
            String s = f.getName().substring(0, f.getName().lastIndexOf('.'));
            File[] subs = f.getParentFile().listFiles((dir, name) ->
                    name.startsWith(s) && name.endsWith(".class"));
            if (subs != null)
                allSuccess &= deleteFiles(subs);
        }
        return allSuccess;
    }

    private static final Map<Character, String> UMLAUTS = Map.of(
            'ä', "_ae_",
            'Ä', "_AE_",
            'ö', "_oe_",
            'Ö', "_OE_",
            'ü', "_ue_",
            'Ü', "_UE_",
            'ß', "_ss_",
            'ẞ', "_SS_"
    );

    public static boolean canJvmEncodeGerman() {
        CharsetEncoder encoder = Charset.defaultCharset().newEncoder();
        return UMLAUTS.keySet().stream().allMatch(encoder::canEncode);
    }

    public static boolean rewriteFiles(File[] files) {
        boolean b = true;
        for (File f : files)
            b &= rewriteFile(f);
        return b;
    }

    /**
     * rewrites file to contain only ASCII characters
     * @return success
     */
    public static boolean rewriteFile(File file) {
        // read file
        String content;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            content = br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return false;
        }

        CharsetEncoder encoder = Charset.defaultCharset().newEncoder();

        // no rewrite needed?
        if (encoder.canEncode(content))
            return true;

        // replace all found umlaut elements
        String result = content.chars()
                .mapToObj(i -> UMLAUTS.containsKey((char)i) ? UMLAUTS.get((char)i) : String.valueOf((char)i))
                .collect(Collectors.joining());

        // encoding still fails?
        if (!encoder.canEncode(result))
            return false;

        // write result to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(result);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * @return user.dir
     */
    public static File getCurrentDir() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Windows: %APPDATA%\LANGUAGE_NAME;
     * Mac: [user.home]/Library/Application Support/LANGUAGE_NAME;
     * Linux: [user.home]/.config/LANGUAGE_NAME;
     */
    public static File getDJavaConfigFolder() {
        return new File(getAppDataFolder(), Main.LANGUAGE_NAME);
    }

    /**
     * Windows: %APPDATA%;
     * Mac: [user.home]/Library/Application Support;
     * Linux: [user.home]/.config;
     */
    public static File getAppDataFolder() {
        String path = System.getProperty("user.home");
        // empty path? link to user.dir
        if (path == null)
            return new File("");
        // adjust based on OS
        switch (OS.getOS()) {
            case WINDOWS -> {
                if (System.getenv("APPDATA") != null)
                    path = System.getenv("APPDATA");
            }
            case MAC -> path += File.separator + "Library" + File.separator + "Application Support";
            case LINUX -> path += File.separator + ".config";
        }
        return new File(path);
    }
}
