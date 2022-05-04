package filesystem;

import main.Logger;
import main.OS;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
     * @return process builder ready to start
     */
    public ProcessBuilder createProcessBuilder(String mainCommand, List<String> appendItems) {
        ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true);

        // todo break up in parts that are moved to subclasses
        // todo check if * as space works in linux/mac? -> no, but keep testing
        // todo mechanic (linux) to try to eliminate spaces: mainCommand by pb.dir -> args by pb.dir (or maybe both, relativize?)

        // pre-format main command
        mainCommand = replaceEnvVars(mainCommand);
        // escape strings
        appendItems = escape(appendItems);

        // handle main command
        // Linux/Mac: change working directory of process builder (because of spaces)?
        if ((OS.isLinux() || OS.isMac())) {
            if (mainCommand.contains(" ")) {
                File f = new File(mainCommand);
                if (f.getParentFile() != null) {
                    Logger.log("Prozess relativ zur Datei des Rennen-Befehls verschoben.");
                    pb.directory(f.getParentFile());
                    // main Command is set relative to working dir
                    mainCommand = f.getName();
                }
                // spaces in name itself? basically no hope
                if (f.getParentFile() == null || mainCommand.contains(" ")) {
                    Logger.warning("Leerzeichen im Dateinamen des Rennen-Befehls!");
                    mainCommand = escape(mainCommand);
                }
            } else {
                // check if an argument contains
            }
        } else if (mainCommand.contains(" "))
            mainCommand = escape(mainCommand);

        // build command list
        List<String> commands = basicCommand();
        commands.add(mainCommand);
        commands.addAll(appendItems);

        // Windows: move strings with spaces to environment variables?
        if (OS.isWindows()) {
            int n = 0;
            final String envVarPrefix = "TEMP_DJAVA_VAR_";
            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i).contains(" ")) {
                    // save command and change to variable name
                    pb.environment().put(envVarPrefix + i, commands.get(i));
                    commands.set(i, formatEnvVar(envVarPrefix + i));
                    n++;
                }
            }
            if (n > 0)
                Logger.log("%d Befehls-Elemente mit Leerzeichen in Umgebungsvariablen verschoben", n);
        }

        Logger.log("Erstellter Befehl: %s", commands.toString());

        /*if (Main.Flag.TEST.set) {
            String in = "";
            java.util.Scanner sc = new java.util.Scanner(System.in);
            while (!in.equals("xx")) {
                System.out.print("index: ");
                int i = Integer.parseInt(sc.nextLine());
                System.out.print("was tun: ");
                in = sc.nextLine();
                if (in.equals("a")) {
                    System.out.print("add value: ");
                    String s = sc.nextLine();
                    commands.add(i, s);
                }else if (in.equals("rm")) {
                    commands.remove(commands.get(i));
                }else if (in.equals("env")) {
                    System.out.print("add env: ");
                    String s = sc.nextLine();
                    pb.environment().put(s.split(";")[0], s.split(";")[1]);
                }else if (in.equals("getenv")) {
                    System.out.println(pb.environment().toString().replace(", ", "\n"));
                }else if (in.equals("v")) {
                    System.out.print("new value: ");
                    String s = sc.nextLine();
                    commands.set(i, s.equals("--") ? commands.get(i) : s);
                }else if (in.equals("e")) {
                    commands.set(i, Commander.escape(commands.get(i)));
                }else if (in.equals("ue")) {
                    commands.set(i, Commander.unescape(commands.get(i)));
                }
                Logger.log("%s", commands.toString());
            }
        }*/

        return pb.command(commands);
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
     * @return s with escaped spaces, based on OS
     */
    public String escape(String s) {
        return s.contains(" ") ? String.format("\"%s\"", s) : s;
    }

    public List<String> escape(List<String> list) {
        return new ArrayList<>(list.stream().map(this::escape).toList());
    }

    public String unescape(String s) {
        switch (OS.getOS()) {
            case WINDOWS -> s = s.contains(" ") && s.startsWith("\"") ? s.substring(1, s.length()-1) : s;
            case LINUX, MAC -> s = s.replace("\\ ", " ");
        }
        return s;
    }

    public boolean isEscaped(String s) {
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

    public String formatEnvVar(String s) {
        switch (OS.getOS()) {
            case WINDOWS -> s = "%" + s + "%";
            case LINUX, MAC -> s = "${" + s + "}";
        }
        return s;
    }
}
