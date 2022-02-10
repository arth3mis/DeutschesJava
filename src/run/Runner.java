package run;

import filesystem.Commander;
import main.Logger;
import main.Main;
import main.OS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public abstract class Runner {

    protected File mainClassFile;
    protected String[] args;

    protected String runnerCommand;

    protected final int programEndNewLines = 2;

    /**
     * @return new instance of subclass based on OS, null if OS is not supported
     */
    public static Runner newInstance(File mainClassFile, String[] runArgs, String customRunner) {
        if (OS.isWindows())
            return new RunnerWindows(mainClassFile, runArgs, customRunner);
        else if (OS.isLinux())
            return new RunnerLinux(mainClassFile, runArgs, customRunner);
        else if (OS.isMac())
            return new RunnerMac(mainClassFile, runArgs, customRunner);
        return null;
    }

    public Runner(File mainClassFile, String[] args, String customRunner) {
        this.mainClassFile = mainClassFile;
        this.args = args;
        // custom runner set?
        runnerCommand = customRunner == null || customRunner.isEmpty() ? "java" : customRunner;
    }

    /**
     * @return -classpath (path) (main_class) [args]
     */
    protected List<String> javaCommandArguments() {
        List<String> c = new ArrayList<>();
        c.add("-classpath");
        c.add(mainClassFile.getParent() == null ? "." : mainClassFile.getParent());
        c.add(mainClassFile.getName());
        if (args != null)
            c.addAll(Arrays.asList(args));
        return c;
    }

    /**
     * @return true if process launched correctly
     */
    public boolean start() {
        // execute command via process builder
        // route streams to let user interact with the program
        // (https://stackoverflow.com/questions/5711084/java-runtime-getruntime-getting-output-from-executing-a-command-line-program)
        try {
            Logger.log("Ausführung mit %s starten...", runnerCommand);

            ProcessBuilder pb = Commander.createProcessBuilder(runnerCommand, javaCommandArguments());
            // debug for linux
            //ProcessBuilder pb = new ProcessBuilder().redirectErrorStream(true).command(commands);
            //pb.directory(new File(Commander.replaceEnvVars(runnerCommand)).getParentFile());
            /*commands = commands.stream().map(Commander::escape).toList();
            Logger.log("%s; %s", commands.toString(), pb.directory());
            if (Main.Flag.TEST.set) {
                String in = "";
                Scanner sc = new Scanner(System.in);
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
            //pb.command(commands.subList(0,commands.size()-1));
                    //Commander.createProcessBuilder(commands.subList(0,commands.size()-1), doEnvs);
            //Logger.log("%s", commands.toString());

            final Process p = pb.start();

            System.out.println(Main.OUTPUT_SEP);
            /////////////////////////////////////////////////////////////////

            // user input logic
            final Scanner scanner = new Scanner(System.in);

            new Thread(() -> {
                while (p.isAlive()) {
                    try {
                        if (scanner.hasNext()) {
                            String input = scanner.nextLine() + "\n";
                            p.outputWriter().write(input);
                            p.outputWriter().flush();
                        }
                    } catch (IOException | NullPointerException | IllegalStateException ignored) {
                        //Logger.error("DEBUG FEHLER: %s", ignored.getMessage());
                    }
                }
            }).start();

            // program output logic
            p.getInputStream().transferTo(System.out);

            /////////////////////////////////////////////////////////////////
            System.out.println("\n".repeat(programEndNewLines) + Main.OUTPUT_SEP);

            // evaluate process return (probably only 0 or 1, but useful to signal successful termination)
            int exitValue = p.waitFor();
            System.out.printf("Programm beendet mit Endwert %d\n\n", exitValue);

            // user input necessary to kill scanner thread
            System.out.println("[ENTER] Fertig");
            scanner.close();
            try {
                scanner.nextLine();
                // this ^^^^^^^^^^ will trigger an exception because scanner is closed
                // causes thread to break out of scanner.hasNext()
            } catch (IllegalStateException ignored) {
            }
            return true;
        } catch (IOException | SecurityException | InterruptedException e) {
            Logger.error("Prozess-Fehler beim Ausführen mit %s: %s", Commander.formatCommand(runnerCommand), e.getMessage());
            return false;
        }
    }
}
