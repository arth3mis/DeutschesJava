package run;

import filesystem.Commander;

import java.io.File;

class RunnerLinux extends Runner {

    public RunnerLinux(File mainClassFile, String[] args, String customRunner) {
        super(mainClassFile, args, customRunner);
    }

    @Override
    protected void buildCommand() {
        //commands = Commander.basicCommand();
        commands.add("\"" + String.join(" ", basicRunCommand()) + "\"");
    }
}
