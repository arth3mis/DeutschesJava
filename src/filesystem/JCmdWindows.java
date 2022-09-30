package filesystem;

import main.Logger;

import java.util.ArrayList;
import java.util.List;

class JCmdWindows extends JCmd {

    @Override
    public List<String> basicCommand() {
        return new ArrayList<>(List.of(new String[]{"cmd.exe", "/c"}));
    }

    @Override
    protected boolean acceptSpaces(String mainCommand, List<String> appendItems) {
        return true;
    }

    /**
     * Move strings with spaces to environment variables
     * @param commands command list, is altered by this method
     * @param pb process builder, is altered by this method
     */
    @Override
    protected void moveToEnvVars(List<String> commands, ProcessBuilder pb) {
        int n = 0;
        final String envVarPrefix = "TEMP_DJAVA_VAR_";
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i).contains(" ")) {
                // save command and change to variable name
                pb.environment().put(envVarPrefix + i, commands.get(i));
//                Logger.debug("'%s' -> '%s'", commands.get(i), formatEnvVar(envVarPrefix + i));
                commands.set(i, formatEnvVar(envVarPrefix + i));
                n++;
            }
        }
        if (n > 0)
            Logger.debug("%d Befehls-Elemente mit Leerzeichen in Umgebungsvariablen verschoben", n);
    }

    private String formatEnvVar(String s) {
        return "%" + s + "%";
    }
}
