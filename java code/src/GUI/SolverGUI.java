package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import instanceManager.Instance;
import solverLIRP.Solution;
import solverLIRP.Solver;

public class SolverGUI extends JFrame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JButton instButton, logButton, solveButton, cancelButton;
	private JPanel instPanel, solPanel;
	JTextArea instLog;
	private JScrollPane instScrollPane;
	private JFileChooser fc;

	public SolverGUI() {
		// TODO Auto-generated method stub
		super("Solver");
		this.setBounds(32, 32, 400, 200);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLayout(new FlowLayout());

		// Instance panel
		this.instPanel = new JPanel();
		this.instPanel.setBackground(Color.white);
		this.instPanel.setLayout(new FlowLayout());
		this.instPanel.setBorder(BorderFactory.createTitledBorder("Instance selection"));
		this.instLog = new JTextArea(5,20);
		this.instLog.setMargin(new Insets(5,5,5,5));
		this.instLog.setEditable(false);
		this.instScrollPane = new JScrollPane(instLog);

		this.instButton = new JButton("Select instances");

		this.instPanel.add(this.instLog);
		this.instPanel.add(this.instButton);

		this.instButton.addActionListener(this);
		
		// Solver panel
		JPanel solPanel = new JPanel();
		solPanel.setBackground(Color.white);
		solPanel.setLayout(new FlowLayout());
		solPanel.setBorder(BorderFactory.createTitledBorder("Solver parameters"));
		instPanel.setLayout(new FlowLayout());
		JButton solButton = new JButton("Log files directory");
		JLabel solDirSelect = new JLabel("Log files directory"); // REPLACE WITH A PROPOSED NAME
		instPanel.add(instButton, BorderLayout.WEST);
		instPanel.add(instSelect, BorderLayout.EAST);


		// Method selection
		JPanel 
		JCheckBox routeSamplingBox = new JCheckBox("Use the route sampling procedure");
		routeSamplingBox.setSelected(false);
		JCheckBox areaSamplingBox = new JCheckBox("Use the area sampling procedure");
		areaSamplingBox.setSelected(false);

		// Launch the resolution
		JButton okBouton = new JButton("OK");
		JButton cancelBouton = new JButton("Cancel");

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new GridLayout(4, 1));
		// Add the control buttons into a separate panel
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout());
		controlPanel.add(okBouton, BorderLayout.EAST);
		controlPanel.add(cancelBouton, BorderLayout.WEST);
		// Add the radio buttons and the control panel one after another
		choicePanel.add(instPanel);
		choicePanel.add(routeSamplingBox);
		choicePanel.add(areaSamplingBox);
		choicePanel.add(controlPanel);

		// Implement the actions to take when the buttons are selected
		okBouton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {        
				String options = "";
				if(routeSamplingBox.isSelected())
					options += "r";
				if(areaSamplingBox.isSelected())
					options += "a";

				String instFileName = instSelect.getText();
				if(instFileName == "None") {
					JOptionPane.showMessageDialog(solverFrame,
							"Please select an instance file to solve.",
							"Error",
							JOptionPane.ERROR_MESSAGE);
				}
				else if(!(instFileName.contains("Instances/Complete/") && instFileName.contains(".json"))) {
					JOptionPane.showMessageDialog(solverFrame,
							"The file selected is not an instance of LIRP.",
							"Error",
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					String output = "";
					Instance LIRPInst = new Instance(instFileName);
					Solution sol = Solver.solve(LIRPInst, LIRPInst.getRoutes(), options, output);
				}
			}

		});

		cancelBouton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				choicePanel.setVisible(false);
			}      
		});

		solverFrame.getContentPane().add(instPanel);
		solverFrame.getContentPane().add(choicePanel);
		solverFrame.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {

		//Handle open button action.
		if (e.getSource() == this.instButton) {
			String[] instFiles = InstanceGUI.createAndShowInstanceGUI();

		}
		else if (e.getSource() == this.cancelButton) {
			
		}
	}
}
