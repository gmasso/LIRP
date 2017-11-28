package userInterfaceLIRP;

import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class VehiclesPanel extends JPanel implements ItemListener {
	/*
	 * ATTRIBUTES
	 */
	private 	JComboBox<Integer> nbVBox = new JComboBox<Integer>();
	private 	JComboBox<Integer> capaBox = new JComboBox<Integer>();
	private JCheckBox nbVCheck, capaCheck;
	private JLabel capaExplain = new JLabel("(average number of demand periods covered by the fleet)");

	/*
	 * STATIC ATTRIBUTES
	 */
	private static final long serialVersionUID = 1L;	

	/**
	 * Create a new VehiclesPanel object
	 * @throws IOException
	 */
	public VehiclesPanel() throws IOException {
		super(new GridLayout(4,3));
        
		// Frame and title the panel
		this.setBorder(BorderFactory.createTitledBorder("Vehicles fleet"));
		
		// Fill the nbVehicles combo box with possible fleet sizes
		this.nbVBox.addItem(1);
		this.nbVBox.addItem(2);
		this.nbVBox.addItem(5);
		this.nbVBox.addItem(10);
		this.nbVBox.addItem(30);
		
		// Add the check box that chooses between a limited or infinite number of vehicles
		this.add(new JLabel("Number of vehicles"));
		this.nbVCheck = new JCheckBox("Unlimited", true);
		this.nbVCheck.addItemListener(this);
		this.add(this.nbVCheck);
		this.nbVCheck.setMnemonic(KeyEvent.VK_C);
		this.add(new JPanel());
		this.add(new JPanel());
		this.add(this.nbVBox);
		this.nbVBox.setVisible(false);
		this.add(new JPanel());
		
		// Fill the capacities combo box with possible capacities
		this.capaBox.addItem(1);
		this.capaBox.addItem(3);
		this.capaBox.addItem(10);
		this.capaBox.addItem(30);
		// Add the check box that chooses between a finite or infinite capacity for each vehicle
		this.add(new JLabel("Capacity of vehicles"));
		this.capaCheck = new JCheckBox("Unlimited", true);
		this.capaCheck.addItemListener(this);
		this.add(this.capaCheck);
		this.capaCheck.setMnemonic(KeyEvent.VK_G);
		this.add(new JPanel());
		this.add(new JPanel());
		this.add(this.capaBox);
		this.capaBox.setVisible(false);
		this.add(this.capaExplain);
		this.capaExplain.setVisible(false);
	}
	
	/**
	 * 
	 * @return	the number of vehicles in the fleet if it is defined
	 */
	public int getNbVehicles() {
		if(this.nbVCheck.isSelected()) {
			return -1;
		}
		else {
			return (int) nbVBox.getSelectedItem();
		}
	}
	
	/**
	 * 
	 * @return	the capacity of the vehicles if it is defined
	 */
	public double getCapa() {
		if(this.capaCheck.isSelected()) {
			return -1;
		}
		else {
			return (int) capaBox.getSelectedItem();
		}
	}

	/**
	 * Set the vehicles object visible or not depending on the choice of the user
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
 
        //Now that we know which button was pushed, find out
        //whether it was selected or deselected.
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (source == this.nbVCheck) {
				this.nbVBox.setVisible(false);
            } else if (source == this.capaCheck) {
				this.capaBox.setVisible(false);
				this.capaExplain.setVisible(false);
            } 
        }
        else if (e.getStateChange() == ItemEvent.DESELECTED) {
            if (source == this.nbVCheck) {
				this.nbVBox.setVisible(true);
            } else if (source == this.capaCheck) {
				this.capaBox.setVisible(true);
				this.capaExplain.setVisible(true);
            } 
        }
	}
}
