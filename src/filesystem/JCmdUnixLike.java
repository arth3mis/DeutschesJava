package filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class JCmdUnixLike extends JCmd {

    @Override
    public List<String> basicCommand() {
        return new ArrayList<>(/*List.of(new String[]{"bash", "-c"})*/);
    }

    @Override
    public String escape(String s) {
        return s.replace(" ", "\\ ");
    }

    @Override
    public boolean isEscaped(String s) {
        return s.contains("\\ ");
    }

    @Override
    public String unescape(String s) {
        return s.replace("\\ ", " ");
    }
}
