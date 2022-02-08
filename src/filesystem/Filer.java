package filesystem;

import main.Main;
import main.OS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public static boolean deleteFiles(File[] files) {
        boolean allSuccess = true;
        for (File f : files) {
            if (!f.delete())
                allSuccess = false;
        }
        return allSuccess;
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
    public static File getAppConfigFolder() {
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
        return new File(path, Main.LANGUAGE_NAME);
    }
}
