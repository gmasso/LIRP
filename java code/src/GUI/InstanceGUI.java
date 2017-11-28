package GUI;

import java.io.File;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import instanceManager.InstanceFilter;

public class InstanceGUI extends JFrame implements ActionListener {

	/*
	 * STATIC ATTRIBUTES
	 */
	private static final long serialVersionUID = 1L;

	static private final String newline = "\n";

	/*
	 * ATTRIBUTES
	 */
	private boolean calledFromSolver = false;
	private JButton selectButton;
	private JButton createButton;
	private JButton cancelButton;
	private JTextArea log;
	private JFileChooser fc;

	private File[] instFiles;

	public InstanceGUI(boolean solverCall) {
		// TODO Auto-generated method stub
		super("Instance manager");
		this.setBounds(100, 100, 500, 300);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(new FlowLayout());
		this.calledFromSolver = solverCall;

		log = new JTextArea(20,20);
		log.setMargin(new Insets(5,5,5,5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		this.selectButton = new JButton("Select an existing instance");
		this.createButton = new JButton("Create a new instance");
		this.cancelButton = new JButton("Cancel");

		this.fc = new JFileChooser();

		// Add the control buttons into a separate panel
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(3,1));
		controlPanel.add(this.selectButton);
		controlPanel.add(this.createButton);
		controlPanel.add(this.cancelButton);

		JPanel instPane = new JPanel();
		instPane.add(logScrollPane);
		instPane.add(controlPanel);

		this.selectButton.addActionListener(this);
		this.createButton.addActionListener(this);
		this.cancelButton.addActionListener(this);

		this.getContentPane().add(instPane);
	}

	private void selectInstanceFiles() {
		fc.setMultiSelectionEnabled(true);
		fc.addChoosableFileFilter(new InstanceFilter());
		fc.setCurrentDirectory(new File("./Instances/Complete"));
		fc.setAcceptAllFileFilterUsed(false);

		int returnVal = fc.showOpenDialog(InstanceGUI.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			this.instFiles = fc.getSelectedFiles();
			log.setText("");
			//This is where a real application would open the file.
			log.append("Files selected: " + newline);
			for(int findex = 0; findex < this.instFiles.length; findex++) {
				log.append(this.instFiles[findex].getName() + newline);
			}
		} else {
			log.append("Open command cancelled by user." + newline);
		}
		log.setCaretPosition(log.getDocument().getLength());

		//Reset the file chooser for the next time it's shown.
		fc.setSelectedFile(null);
	}

	private void createInstanceFiles() {
		// TODO Auto-generated method stub
		InstanceCreator instCreaDialog = new InstanceCreator(this);
		instCreaDialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {

		//Handle open button action.
		if (e.getSource() == this.selectButton)
			selectInstanceFiles();
		else if(e.getSource() == this.createButton) 
			createInstanceFiles();
		else {
			if(this.calledFromSolver)
				this.dispose();
			else
				System.exit(0);
		}
	}

	public File[] createAndShowInstanceGUI() {
		this.pack();
		this.setVisible(true);

		return this.instFiles;
	}

	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				InstanceGUI instGUI = new InstanceGUI(false);
				instGUI.createAndShowInstanceGUI();
			}
		});
	}
}
