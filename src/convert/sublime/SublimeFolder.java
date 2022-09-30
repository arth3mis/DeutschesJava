package convert.sublime;

import convert.Converter;
import convert.Translation;
import filesystem.Filer;
import main.Logger;
import main.Main;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SublimeFolder {

    private static final Pattern STANDARD_PATTERN = Pattern.compile("\\[\\[(\\w+)]]");

    private static final String SYNTAX_FILE = "DJava.sublime-syntax";
    private static final Pattern SYNTAX_PATTERN = STANDARD_PATTERN;

    // https://www.sublimetext.com/docs/completions.html
    private static final String COMPLETIONS_FILE = "DJava.sublime-completions";
    private static final Pattern COMPLETIONS_PATTERN = STANDARD_PATTERN;
    // todo make extra completions for basic & frequent words (see main translation txt)
    /*  something like this: (put before java.lang Interfaces in .sublime-completions file)
            DISCUSS: EN->DE or DE->EN?
            atm i prefer EN->DE for easier switching from java
    {
            "trigger": "sqrt",
            "contents": "qwrz",
            "annotation": "qwrz",
            "kind": "ambiguous"
        },
     */

    private static final String SNIPPET_FILE_EXTENSION = ".sublime-snippet";
    private static final String SNIPPET_TEMPLATE_FILE = "template" + SNIPPET_FILE_EXTENSION;  // template contains "djava" as literal
    private static final Pattern SNIPPET_PATTERN = Pattern.compile("#(input|output|desc)#");
    private static final Pattern SNIPPET_OUTPUT_PATTERN = Pattern.compile("\\[\\[([\\w.:^]+)]]");
    // file, input, output, description
    //  output fields: ($0: selection when done (inserted at end if missing);
    //                  $1-$n: selection tab order (can be marked words with ${1:word})
    //                  more: https://www.sublimetext.com/docs/completions.html#snippets
    private static final Map<String, Integer> MS = Map.of("input", 1, "output", 2, "desc", 3);
    private static final String[][] SNIPPET_FILES = {
            {"print", "[[^print]]", "[[System.out.print]]($1);", "[[print]]"},
            {"println", "dz", "[[System.out.println]]($1);", "[[println]]"},
            {"println-ref", "dzr", "[[System.out::println]]", "[[println]] Referenz"},
            {"main-class", "hk", """
[[public]] [[class]] $1 {
\t[[public]] [[static]] [[void]] [[main]]([[String]][] argumente) {
\t\t$0
\t}
}""", "Klasse + Haupt-Methode"},
            {"main-method", "hm", """
[[public]] [[static]] [[void]] [[main]]([[String]][] argumente) {
\t$0
}""", "Haupt-Methode"},
            {"method", "m", """
${3:[[void]]} ${4:methode}($5) ${1:[[throws]] $2 }{
\t$0
}""", "Methode"},
            {"public-static-final", "[[^public]][[^static]][[^final]]",
                    "[[public]] [[static]] [[final]] $0", "[[public]] [[static]] [[final]]"},
            {"public-static-final-string", "[[^public]][[^static]][[^final]][[^String]]",
                    "[[public]] [[static]] [[final]] [[String]] $0", "[[public]] [[static]] [[final]] [[String]]"},
    };

    private static final int SUBLIME_HIGHEST_NUM_VERSION = 3;
    private static final int SUBLIME_LOWEST_NUM_VERSION = 2;
    private static final String[] SUBLIME_PATH = {
            "Sublime Text",
            "Packages",
            "User"
    };

    public static void generateSublime() {
        // get reversed map (EN->DE)
        Translation.Reverse r = new Converter(null).loadAndGetReversedTranslation();

        boolean warnIfSuccess = false;
        File targetFolder = getSublimeUserFolder();
        if (targetFolder == null) {
            warnIfSuccess = true;
            targetFolder = Filer.getCurrentDir();
        }

        // make sub-folder "DJava"
        File outFolder = new File(targetFolder, Main.LANGUAGE_NAME);
        if (!outFolder.isDirectory() && !outFolder.mkdir()) {
            Logger.error("Fehler: Ausgabe-Ordner konnte nicht erstellt werden.");
            return;
        }

        boolean b;
        b = generateSublimeSyntaxFile(outFolder, r);
        b &= generateSublimeCompletions(outFolder, r);
        b &= generateSublimeSnippets(outFolder, r);

        // output operation result
        if (b) {
            Logger.info("\nErfolgreich erstellt.");
            if (warnIfSuccess) {
                Logger.warning("\nWarnung: Der Sublime Nutzer-Pakete Ordner konnte nicht gefunden werden.");
                Logger.warning("Bitte suche, wo sich der Ordner '.../Sublime Text[ 3]/Packages/User' befindet, und verschiebe den Ordner '%s' dort hinein.", Main.LANGUAGE_NAME);
            }
        } else
            Logger.error("Fehler: Dateien für Sublime Text konnten nicht alle generiert werden.");
    }

    private static boolean generateSublimeSyntaxFile(File outFolder, Translation.Reverse r) {
        // load syntax file template
        String content = loadFile(SYNTAX_FILE);
        if (content == null)
            return false;

        String result = replaceAllTranslations(content, SYNTAX_PATTERN, "(%s)", "|", r);

        return writeFile(new File(outFolder, SYNTAX_FILE), result);
    }

    private static boolean generateSublimeCompletions(File outFolder, Translation.Reverse r) {
        String content = loadFile(COMPLETIONS_FILE);
        if (content == null)
            return false;

        String result = replaceAllTranslations(content, COMPLETIONS_PATTERN, "%s", "|", r);

        // append "contents" line to triggers with separator "|"
        Pattern p = Pattern.compile("\"([\\wÄäÖöÜüẞß]+)(\\|[\\wÄäÖöÜüẞß|]+\",)");
        Matcher m = p.matcher(result);
        result = m.replaceAll(mr -> "\"" + mr.group(1) + mr.group(2) + "\n" +
                "\t\t\t\"contents\": \"" + mr.group(1) + "\",");

        return writeFile(new File(outFolder, COMPLETIONS_FILE), result);
    }

    private static boolean generateSublimeSnippets(File outFolder, Translation.Reverse r) {
        // files go in subfolder
        outFolder = new File(outFolder, "snippets");
        // delete so files from old versions are gone, (re)create folder
        if (outFolder.isDirectory()) {
            if (!Filer.deleteFiles(outFolder.listFiles()))
                return false;
            if (!outFolder.delete())
                return false;
        }
        if (!outFolder.mkdir())
            return false;

        String template = loadFile(SNIPPET_TEMPLATE_FILE);
        if (template == null)
            return false;
        Matcher m = SNIPPET_PATTERN.matcher(template);

        // create all snippet files
        boolean b = true;
        for (String[] contents : SNIPPET_FILES) {
            // replace marked words in output
            Map<String, String> cache = new HashMap<>();
            for (int i = 0; i < contents.length; i++) {
                Matcher m1 = SNIPPET_OUTPUT_PATTERN.matcher(contents[i]);

                contents[i] = m1.replaceAll(matchResult -> {
                    int firstLetters = matchResult.group(1).lastIndexOf("^") + 1;
                    String result = matchResult.group(1).substring(firstLetters);
                    if (cache.containsKey(result))
                        return firstLetters > 0 ? cache.get(result).substring(0, firstLetters).toLowerCase()
                                : cache.get(result);

                    String[] words = result.split("\\.|::");
                    Translation.Reverse lastR = r;
                    // replace with static/package or root translations
                    for (String s : words) {
                        if (lastR.translations.containsKey(s)) {
                            lastR = lastR.translations.get(s);
                            result = result.replaceFirst(s, lastR.getText());
                        } else if (r.translations.containsKey(s)) {
                            lastR = r.translations.get(s);
                            result = result.replaceFirst(s, lastR.getText());
                        } else {
                            lastR = r;
                        }
                    }

                    cache.put(matchResult.group(1).substring(firstLetters), result);
                    return firstLetters > 0 ? result.substring(0, firstLetters).toLowerCase()
                            : result;
                });
            }

            // precede all "$" signs with a "\" (otherwise the matcher replaces them)
            contents[MS.get("output")] = contents[MS.get("output")]
                    .replaceAll("\\$", "\\\\\\$");  // well, that escapelated quickly

            m.reset();
            String result = m.replaceAll(matchResult -> contents[MS.get(matchResult.group(1))]);
            b &= writeFile(new File(outFolder, contents[0] + SNIPPET_FILE_EXTENSION), result);
        }
        return b;
    }

    private static String replaceAllTranslations(String input, Pattern p, String multiFormat, String sep, Translation.Reverse r) {
        Matcher m = p.matcher(input);
        return m.replaceAll(matchResult -> {
            String k = matchResult.group(1);
            var l = r.translations.get(k);
            if (l == null) {
                return switch (k) {
                    case "LANGUAGE_NAME" -> Main.LANGUAGE_NAME;
                    case "EXTENSION_NAME" -> Main.EXTENSION_NAME;
                    default -> "!!!";
                };
            } else if (l.count() == 1) {
                return l.getText();
            } else {
                return multiFormat.formatted(String.join(sep, Stream.concat(Arrays.stream(
                        new String[]{ l.getText() }), l.extraTexts.stream()).toList()));
            }
        });
    }

    private static String loadFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                SublimeFolder.class.getResourceAsStream(fileName))))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException | NullPointerException e) {
            return null;
        }
    }

    private static boolean writeFile(File file, String output) {
        try {
            if (file.isFile() && !file.delete() || !file.createNewFile())
                return false;
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(output);
            bw.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static File getSublimeUserFolder() {
        File dataFolder = Filer.getAppDataFolder();
        for (int i = 0; i < SUBLIME_PATH.length; i++) {
            dataFolder = new File(dataFolder, SUBLIME_PATH[i]);
            if (!dataFolder.isDirectory()) {
                // check for "Sublime Text [n]"
                if (i == 0) {
                    for (int v = SUBLIME_HIGHEST_NUM_VERSION; v >= SUBLIME_LOWEST_NUM_VERSION; v--) {
                        dataFolder = new File(dataFolder.getParentFile(), SUBLIME_PATH[0] + " " + v);
                        if (dataFolder.isDirectory()) break;
                    }
                    if (dataFolder.isDirectory()) continue;
                }
                return null;
            }
        }
        return dataFolder;
    }
}
