package filesystem;

import java.util.ArrayList;
import java.util.List;

class JCmdWindows extends JCmd {

    @Override
    public List<String> basicCommand() {
        return new ArrayList<>(List.of(new String[]{"cmd.exe", "/c"}));
    }

    @Override
    public ProcessBuilder createProcessBuilder(String mainCommand, List<String> appendItems) {
        return super.createProcessBuilder(mainCommand, appendItems);
    }
}
