package filesystem;

import main.Main;
import main.OS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commander {

    public static List<String> basicCommand() {
        List<String> c = new ArrayList<>();
        switch (OS.getOS()) {
            case WINDOWS -> c.addAll(List.of(new String[]{"cmd.exe", "/c"}));
            case LINUX, MAC -> c.addAll(List.of(new String[]{"bash", "-c"}));
        }
        return c;
    }

    /**
     * @param command first part of command; referenced environment variables will be replaced, spaces escaped
     * @return formatted mainCommand
     */
    public static String formatCommand(String command) {
        return escape(replaceEnvVars(command));
    }

    /**
     * Sets up new process builder, escapes strings if necessary. Error stream is redirected to stdout.
     * @param commands list of commands and arguments, split in single items that can be escaped together
     * @return process builder ready to start
     */
    public static ProcessBuilder createProcessBuilder(List<String> commands) {
        ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true);
        // escape strings
        commands = new ArrayList<>(commands.stream()
                .map(Commander::escape)
                .toList());
        // move strings with spaces to environment variables?
        if (OS.isWindows() || Main.Flag.TEST.set) {
            final String envVarPrefix = "TEMP_DJAVA_VAR_";

            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i).contains(" ")) {
                    // save command and change to variable name
                    pb.environment().put(envVarPrefix + i, commands.get(i));
                    commands.set(i, formatEnvVar(envVarPrefix + i));
                }
            }
        }
        return pb.command(commands);
    }

    /**
     * @param s input
     * @return s with escaped spaces, based on OS
     */
    public static String escape(String s) {
        switch (OS.getOS()) {
            case WINDOWS -> s = s.contains(" ") ? String.format("\"%s\"", s) : s;
            case LINUX, MAC -> s = s.replace(" ", "\\ ");
        }
        return s;
    }

    public static boolean isEscaped(String s) {
        boolean b = false;
        switch (OS.getOS()) {
            case WINDOWS -> b = s.contains(" ") && s.startsWith("\"") && s.endsWith("\"");
            case LINUX, MAC -> b = s.contains("\\ ");
        }
        return b;
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
                Pattern subExpr = Pattern.compile(Pattern.quote(matcher.group(0)));
                s = subExpr.matcher(s).replaceAll(envValue);
            }
        }
        return s;
    }

    public static String formatEnvVar(String s) {
        switch (OS.getOS()) {
            case WINDOWS -> s = "%" + s + "%";
            case LINUX, MAC -> s = "${" + s + "}";
        }
        return s;
    }
}
