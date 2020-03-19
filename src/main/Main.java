package main;

public class Main {

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0 || (args.length == 1 && args[0].startsWith("-")))
                System.exit(1);
            Interpreter.loadTranslation();
            if (args[0].startsWith("-")) {
                String[] args2 = new String[args.length - 1];
                System.arraycopy(args, 1, args2, 0, args2.length);
                switch (args[0]) {
                    case "-convert":
                        Interpreter.makeJavaFile(args2);
                        break;
                    case "-compile":
                        Interpreter.makeJavaFile(args2);
                        Interpreter.compile(args2);
                        Interpreter.deleteJavaFile(args2);
                        break;
                    case "-run":
                        Interpreter.makeJavaFile(args2);
                        Interpreter.compile(args2);
                        Interpreter.deleteJavaFile(args2);
                        Interpreter.run(args2[0]);
                        break;
                    case "-complete":
                        Interpreter.makeJavaFile(args2);
                        Interpreter.compile(args2);
                        Interpreter.run(args2[0]);
                        break;
                    default:
                        System.exit(1);
                }
            } else {
                Interpreter.makeJavaFile(args);
                Interpreter.compile(args);
                Interpreter.deleteJavaFile(args);
                Interpreter.run(args[0]);
            }
        } catch (Exception e) {
            System.exit(1);
        }
    }

}
