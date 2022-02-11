package filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class JCmdUnixLike extends JCmd {

    private File relativeDir;

    @Override
    public List<String> basicCommand() {
        return new ArrayList<>(/*List.of(new String[]{"bash", "-c"})*/);
    }

    /**
     * Checks if strings contain spaces, tries to relativize with user.dir
     * @return -classpath (path) (main_class)
     */
    @Override
    protected List<String> runCommand(String runnerExecutable, File mainClassFile) {
        List<String> c = new ArrayList<>();
        c.add(runnerExecutable);
        c.add("-classpath");
        c.add(mainClassFile.getParent() == null ? "." : mainClassFile.getParent());
        c.add(mainClassFile.getName());
        return c;
    }

    @Override
    public ProcessBuilder createProcessBuilder(String mainCommand, List<String> appendItems) {
        return super.createProcessBuilder(mainCommand, appendItems);
    }

    @Override
    public String escape(String s) {
        return s.replace(" ", "*");
    }
}
