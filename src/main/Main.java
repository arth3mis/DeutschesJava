package main;

import java.io.File;
import java.util.Arrays;

public class Main {

    static boolean log = false;

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0)
                System.exit(1);
            if (args[0].equalsIgnoreCase("-v")) {
                if (args.length == 1)
                    System.exit(1);
                log = true;
                String[] a = new String[args.length-1];
                System.arraycopy(args, 1, a, 0, a.length);
                args = a;
            }
            if (args.length == 1 && args[0].startsWith("-"))
                System.exit(1);
            log("\n");
            log(Interpreter.loadTranslation());
            if (args[0].startsWith("-")) {
                String[] args2 = new String[args.length - 1];
                System.arraycopy(args, 1, args2, 0, args2.length);
                args2 = makeAbsolutePaths(args2);
                switch (args[0]) {
                    case "-convert":
                        log(Interpreter.makeJavaFile(args2));
                        break;
                    case "-compile":
                        log(Interpreter.makeJavaFile(args2));
                        log(Interpreter.compile(args2));
                        log(Interpreter.deleteJavaFile(args2));
                        break;
                    case "-run":
                        log(Interpreter.makeJavaFile(args2));
                        log(Interpreter.compile(args2));
                        log(Interpreter.deleteJavaFile(args2));
                        log(Interpreter.run(args2[0]));
                        break;
                    case "-complete":
                        log(Interpreter.makeJavaFile(args2));
                        log(Interpreter.compile(args2));
                        log(Interpreter.run(args2[0]));
                        break;
                    default:
                        System.exit(1);
                }
            } else {
                args = makeAbsolutePaths(args);
                Interpreter.makeJavaFile(args);
                Interpreter.compile(args);
                Interpreter.deleteJavaFile(args);
                Interpreter.run(args[0]);
            }
        } catch (Exception e) {
            System.exit(1);
        }
    }

    static String[] makeAbsolutePaths(String[] paths) {
        String[] ap = new String[paths.length];
        String currentLocation = System.getProperty("user.dir");
        for (int i = 0; i < ap.length; i++) {
            File f = new File(paths[i]);
            if (!f.exists() || !f.isAbsolute()) {
                ap[i] = new File(currentLocation, paths[i]).toString();
                log("created abs path: " + ap[i]);
            } else {
                ap[i] = paths[i];
            }
        }
        return ap;
    }

    static void log(String s) {
        if (log) {
            System.out.println(s);
        }
    }

}
