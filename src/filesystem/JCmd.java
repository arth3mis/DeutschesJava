package filesystem;

import main.Logger;
import main.OS;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JCmd {

    protected JCmd() { }

    @Contract(" -> new")
    public static @NotNull JCmd get() {
        if (OS.isWindows()) {
            return new JCmdWindows();
        } else if (OS.isLinux() || OS.isMac()) {
            return new JCmdUnixLike();
        }
        return new JCmd();
    }

    public List<String> basicCommand() {
        return new ArrayList<>();
    }

    /**
     * @return -classpath (path) (main_class)
     */
    protected List<String> runCommand(String runnerExecutable, File mainClassFile) {
        List<String> c = new ArrayList<>();
        c.add(runnerExecutable);
        c.add("-classpath");
        c.add(mainClassFile.getParent() == null ? "." : mainClassFile.getParent());
        c.add(mainClassFile.getName());
        return c;
    }

    /**
     * Sets up new process builder, formats commands. Error stream is redirected to stdout.
     * @param mainCommand javac/java
     * @param appendItems list of command arguments, split in single items that can be escaped together
     * @return process builder ready to start, null if problems occur
     */
    public ProcessBuilder createProcessBuilder(String mainCommand, List<String> appendItems) {
        // spaces not allowed?
        if (!acceptSpaces(mainCommand, appendItems))
            return null;
        // pre-format main command
        mainCommand = escape(replaceEnvVars(mainCommand));
        // escape strings
        appendItems = escape(appendItems);

        ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true);

        // build command list
        List<String> commands = basicCommand();
        commands.add(mainCommand);
        commands.addAll(appendItems);
        moveToEnvVars(commands, pb);
        Logger.log("Erstellter Befehl: %s", commands.toString());
        return pb.command(commands);
    }

    protected boolean acceptSpaces(String mainCommand, List<String> appendItems) {
        // do not accept spaces by default
        if (mainCommand.contains(" ") || appendItems.stream().anyMatch(s -> s.contains(" "))) {
            Logger.error("Leerzeichen in Befehl/Argumenten funktionieren auf diesem Betriebssystem nicht.");
            return false;
        }
        return true;
    }

    protected void moveToEnvVars(List<String> commands, ProcessBuilder pb) {
        // no standard implementation
    }

    /**
     * @param command input
     * @return referenced environment variables will be replaced, spaces escaped
     */
    public String formatCommand(String command) {
        return escape(replaceEnvVars(command));
    }

    /**
     * @param s input
     * @return s with escaped spaces
     */
    public String escape(String s) {
        return s.contains(" ") ? String.format("\"%s\"", s) : s;
    }

    public List<String> escape(List<String> list) {
        return new ArrayList<>(list.stream().map(this::escape).toList());
    }

    public String unescape(String s) {
        return s.contains(" ") && s.startsWith("\"") ? s.substring(1, s.length()-1) : s;
    }

    public boolean isEscaped(String s) {
        return s.contains(" ") && s.startsWith("\"") && s.endsWith("\"");
    }

    /**
     * @param s input
     * @return s with replaced environment variable, based on OS
     */
    public String replaceEnvVars(String s) {
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
}
