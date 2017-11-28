package GUI;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class DepotsFrame extends JFrame {
	public DepotsFrame() {
		this.setTitle("Depots");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(new FlowLayout());;
		
		this.add(new JLabel(""));
		this.setVisible(true);
	}
}
