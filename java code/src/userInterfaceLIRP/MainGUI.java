package userInterfaceLIRP;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class MainGUI {

	public JMenuBar createMenuBar() {
		/* Create the menu bar */
		JMenuBar menuBar = new JMenuBar();

		/* Add the LIRP menu */
		JMenu menuLIRP = new JMenu("LIRP");


		/* Add the instances menu */
		JMenu menuInst = new JMenu("Instances");
	    menuInst.setMnemonic(KeyEvent.VK_A);
        menuInst.getAccessibleContext().setAccessibleDescription(
                "Access the instance manager");

        //a group of JMenuItems
        JMenuItem menuItem = new JMenuItem("Select an existing LIRP instance",
                                 KeyEvent.VK_T);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Select one or several LIRP instances");
        menuInst.add(menuItem);
        
		/* Add the LIRP menu */
		JMenu menuSolver = new JMenu("Solver");

		menuBar.add(menuLIRP);
		menuBar.add(menuInst);
		menuBar.add(menuSolver);
		
		return menuBar;
	}
	
	public JPanel createContentPane() {
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setOpaque(true);
		
		return contentPane;
	}
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("LIRP");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the content pane.
        MainGUI lirpGUI = new MainGUI();
        frame.setJMenuBar(lirpGUI.createMenuBar());
        frame.setContentPane(lirpGUI.createContentPane());
 
        //Display the window.
        frame.setSize(450, 600);
        frame.setVisible(true);
    }
 
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
