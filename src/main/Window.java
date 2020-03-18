package main;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Window extends JFrame implements KeyListener {

    static final Dimension WD = Toolkit.getDefaultToolkit().getScreenSize();

    static JFrame f = new Window();

    JMenuBar mbTop;
        JMenu mFile;
            JMenuItem miOpenProject;
    JLabel lProjName, lFileName;
    JButton btnTransl, btnCompile, btnRun, btnAll;

    Window() {
        initFrame();
    }

    private void initFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("DJAVA by Arthur");
        setSize(WD.width, WD.height-40);
        setSize(400, 300);
        setLocation(600, 400);
        //setMinimumSize(WD);
        getContentPane().setBackground(new Color(48, 0, 75));

        getContentPane().setLayout(new BorderLayout());

        JPanel pCenter = new JPanel();

        miOpenProject = new JMenuItem("Projekt öffnen");
        miOpenProject.addActionListener(e -> {
            String s = JOptionPane.showInputDialog(this, "Projektname eingeben:", "Projekt öffnen", JOptionPane.PLAIN_MESSAGE);
            Interpreter.projectDir = s;
        });
        mFile = new JMenu("Datei");
        mFile.add(miOpenProject);
        mbTop = new JMenuBar();
        mbTop.add(mFile);
        setJMenuBar(mbTop);

        BoxLayout lpm = new BoxLayout(pCenter, BoxLayout.PAGE_AXIS);
        pCenter.setLayout(lpm);
        pCenter.setBackground(null);
        getContentPane().add(pCenter, BorderLayout.CENTER);

        pCenter.add(Box.createRigidArea(new Dimension(0, 10)));

        lProjName = new JLabel(Interpreter.projectDir);
        lProjName.setForeground(Color.WHITE);
        pCenter.add(lProjName);

        pCenter.add(Box.createRigidArea(new Dimension(0, 10)));

        btnAll = new JButton("Translate, Compile & Run");
        btnAll.addActionListener(e -> {
            Interpreter.read();
            Interpreter.replace();
            Interpreter.makeJavaFile();
            Interpreter.compile();
            Interpreter.run();
        });
        pCenter.add(btnAll);
    }

    public static void main(String[] args) {
        Interpreter.loadTranslation();
        f.setVisible(true);
    }


    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
