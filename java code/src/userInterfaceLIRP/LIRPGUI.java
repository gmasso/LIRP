package userInterfaceLIRP;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

public class LIRPGUI {

	public JMenuBar createMenuBar() {
		/* Create the menu bar */
		JMenuBar menuBar = new JMenuBar();

		/* Add the LIRP menu */
		JMenu menuLIRP = new JMenu("LIRP");


		/* Add the instances menu */
		JMenu menuInst = new JMenu("Instances");
	    menuInst.setMnemonic(KeyEvent.VK_I);
        menuInst.getAccessibleContext().setAccessibleDescription(
                "Access the instance manager");

        //a group of JMenuItems
        JMenuItem menuItem = new JMenuItem("Select an existing LIRP instance",
                                 KeyEvent.VK_E);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_E, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "Select one or several LIRP instances");
        menuInst.add(menuItem);
        
		/* Add the Solver menu */
		JMenu menuSolver = new JMenu("Solver");
	    menuSolver.setMnemonic(KeyEvent.VK_S);
        menuSolver.getAccessibleContext().setAccessibleDescription(
                "Choose solver options");
		ButtonGroup modelGroup = new ButtonGroup();
		
		/* Choose between model 1 and model 2 */
		JRadioButtonMenuItem mod1MenuItem = new JRadioButtonMenuItem("Model 1 (direct-loop)");
        mod1MenuItem.setSelected(true);
		mod1MenuItem.setMnemonic(KeyEvent.VK_1);
        modelGroup.add(mod1MenuItem);
        menuSolver.add(mod1MenuItem);
        
		JRadioButtonMenuItem mod2MenuItem = new JRadioButtonMenuItem("Model 2 (loop-direct)");
		mod2MenuItem.setMnemonic(KeyEvent.VK_2);
        modelGroup.add(mod2MenuItem);
        menuSolver.add(mod2MenuItem);

        /* Decide which ampling methods to use to use */
        menuSolver.addSeparator();
        JCheckBoxMenuItem cbRouteSampling = new JCheckBoxMenuItem("Use the route sampling method");
        cbRouteSampling.setMnemonic(KeyEvent.VK_R);
        menuSolver.add(cbRouteSampling);

        JCheckBoxMenuItem cbZoneSampling = new JCheckBoxMenuItem("Use the zone sampling method");
        cbZoneSampling.setMnemonic(KeyEvent.VK_Z);
        menuSolver.add(cbZoneSampling);

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
        JFrame frame = new JFrame("LIRP Project");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Create and set up the content pane.
        LIRPGUI lirpGUI = new LIRPGUI();
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
