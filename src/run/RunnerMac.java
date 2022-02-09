package run;

import java.io.File;

class RunnerMac extends Runner {

    public RunnerMac(File mainClassFile, String[] args, String customRunner) {
        this.mainClassFile = mainClassFile;
        this.args = args;
        this.customRunner = customRunner;
        buildCommand();
    }
}
