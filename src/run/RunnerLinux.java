package run;

import java.io.File;

class RunnerLinux extends Runner {

    public RunnerLinux(File mainClassFile, String[] args, String customRunner) {
        this.mainClassFile = mainClassFile;
        this.args = args;
        this.customRunner = customRunner;
        buildCommand();
    }
}
