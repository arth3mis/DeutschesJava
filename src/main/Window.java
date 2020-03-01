package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Window extends JFrame implements KeyListener {

    static final Dimension WD = Toolkit.getDefaultToolkit().getScreenSize();

    static JFrame f = new Window();

    Window() {
        initFrame();
    }

    private void initFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WD.width, WD.height-40);
        //setMinimumSize(WD);
        getContentPane().setBackground(new Color(48, 0, 75));
    }

    public static void main(String[] args) {
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
