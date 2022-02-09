package filesystem;

import main.OS;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commander {

    /**
     * builds a safe command based on OS.
     * @param mainCommand first part of command; referenced environment variables will be replaced, spaces escaped
     * @param endArguments arguments that are appended to command end
     * @param appendFormat string that follows mainCommand, to be formatted with args
     * @param args appended arguments, spaces in strings will be escaped IF they are part of
     * @return command string
     */
    public static String build(String mainCommand, String[] endArguments, String appendFormat, String... args) {
        // escape main command after replacing variables
        String s1 = buildMain(mainCommand);

        // escape all strings that are part of the format string
        for (int i = 0; i < args.length; i++) {
                args[i] = escape(args[i], true);
        }
        String s2 = "";
        if (appendFormat != null)
            s2 = String.format(appendFormat, (Object[]) args);

        // append additional args as run arguments
        String s3 = formatArgs(endArguments);

        return s1 + (s2.isEmpty() ? "" : " " + s2) + (s3.isEmpty() ? "" : s3);
    }

    /**
     * @param mainCommand first part of command; referenced environment variables will be replaced, spaces escaped
     * @return formatted mainCommand
     */
    public static String buildMain(String mainCommand) {
        return escape(replaceEnvVars(mainCommand), false);
    }

    /**
     * @param s input
     * @return s with escaped spaces, based on OS
     */
    public static String escape(String s, boolean force) {
        switch (OS.getOS()) {
            case WINDOWS -> {
                if (s.contains(" ") || force)
                    return String.format("\"%s\"", s);
                else
                    return s;
            }
            case LINUX, MAC -> {
                return s.replace(" ", "\\ ");
            }
        }
        return s;
    }

    /**
     * @param s input
     * @return s with replaced environment variable, based on OS
     */
    public static String replaceEnvVars(String s) {
        // https://stackoverflow.com/questions/4752817/expand-environment-variables-in-text
        Map<String, String> envMap = System.getenv();
        String pattern = null;
        switch (OS.getOS()) {
            case WINDOWS -> pattern = "%([A-Za-z0-9_]+)%";
            case LINUX, MAC -> pattern = "\\$([A-Za-z0-9_]+)|\\$\\{([A-Za-z0-9_]+)}";
        }
        if (pattern != null) {
            Pattern expr = Pattern.compile(pattern);
            Matcher matcher = expr.matcher(s);
            while (matcher.find()) {
                String envValue = envMap.get(matcher.group(1).toUpperCase());
                if (envValue == null) {
                    envValue = "";
                } else {
                    envValue = envValue.replace("\\", "\\\\");
                }
                Pattern subexpr = Pattern.compile(Pattern.quote(matcher.group(0)));
                s = subexpr.matcher(s).replaceAll(envValue);
            }
        }
        return s;
    }

    private static String formatArgs(String[] args) {
        StringBuilder sbArgs = new StringBuilder();
        if (args != null && args.length > 0) {
            for (String arg : args)
                sbArgs.append(" \"").append(arg).append("\"");
        }
        return sbArgs.toString();
    }

    private static int getFormatSpecifierCount(String format) {
        // https://stackoverflow.com/questions/37413816/get-number-of-placeholders-in-formatter-format-string
        String formatSpecifier = "%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
        Pattern pattern = Pattern.compile(formatSpecifier);
        // Build the matcher for a given String
        Matcher matcher = pattern.matcher(format);
        // Count the total amount of matches in the String
        int counter = 0;
        while (matcher.find())
            counter++;
        return counter;
    }
}
