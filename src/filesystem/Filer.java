package filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Filer {

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
}
