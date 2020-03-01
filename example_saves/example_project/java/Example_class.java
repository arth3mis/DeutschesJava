import javax.swing.*;

public class Example_class extends JFrame {
	public static void main(String[] argumente) {
		JFrame r = new Example_class();
		r.setDefaultCloseOperation(EXIT_ON_CLOSE);
		r.setSize(300, 300);
		r.setVisible(true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException a) {
			System.out.print("Fehler\n");
		}
		System.out.println("out");
		System.exit(0);
	}
}
