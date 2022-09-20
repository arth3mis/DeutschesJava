package convert.translation;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Math.*;
import static java.util.stream.Stream.Builder;

/**
 * path reference class to detect txt files when inside a jar archive.
 * Must be located in the same folder as the translation txt files
 */
public class TranslationFolder {

    public static void main(String[] args) {
        // insert search term here
        String searchTerm = "strippe";
        boolean ignoreCase = true;
        String replaceTerm = null;  // todo rewrite txt
        boolean confirmEachReplace = true;

        // confirm replace option
        if (replaceTerm != null) {
            if (JOptionPane.showConfirmDialog(null, "replace is active, is this really wanted?") != JOptionPane.YES_OPTION)
                System.exit(0);
        }

        Pattern rgx = Pattern.compile("("+searchTerm+")", Pattern.CASE_INSENSITIVE);

        // find all occurences in txt files
        File fo = new File("src/convert/translation");
        File[] txts = fo.listFiles((dir, name) -> name.endsWith(".txt"));
        if (txts == null) return;
        for (File f : txts) {
            boolean found = false;
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                int ln = 0;
                String s;
                while ((s = br.readLine()) != null) {
                    lines.add(s);
                    ln++;
                    if (rgx.matcher(s).find()) {
                        if (!found) {
                            System.out.printf("\n\n%s %s\n\n", f.getName(), "-".repeat(70 - f.getName().length()));
                            found = true;
                        }
                        System.out.printf("Z.%3d: '%s'\n", ln, rgx.matcher(s).replaceAll("[$1]"));
                        if (replaceTerm != null) {
                            int c = confirmEachReplace ? JOptionPane.showConfirmDialog(null, "replace in this line?") : JOptionPane.YES_OPTION;
                            if (c == JOptionPane.CANCEL_OPTION)
                                replaceTerm = null;
                            else if (c == JOptionPane.YES_OPTION) {
                                lines.remove(lines.size() - 1);
                                lines.add(rgx.matcher(s).replaceAll(replaceTerm));
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
