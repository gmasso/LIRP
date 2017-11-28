package GUI;

import java.io.*;
import java.text.NumberFormat;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import instanceManager.DepotsMap;
import instanceManager.Location;
import instanceManager.Parameters;

public class DepotsPanel extends JPanel implements ActionListener {

	/*
	 * STATIC ATTRIBUTES
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * ATTRIBUTES
	 */
	private JRadioButton createButton, selectButton;

	private 	JComboBox<Integer> nbDepotsBox = new JComboBox<Integer>();
	private 	JComboBox<Integer> fcBox = new JComboBox<Integer>();
	private JFormattedTextField[] ocFields;
	private NumberFormat doubleFormat = NumberFormat.getNumberInstance();
	private JRadioButton distButton, customButton;
	private JPanel ocPane = new JPanel();
	private JPanel customOCPane = new JPanel(new BorderLayout());

	/**
	 * Creates a new DepotsPanel object
	 * @throws IOException
	 */
	public DepotsPanel() throws IOException {
		super(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Depots"));
		this.doubleFormat.setMaximumFractionDigits(2);

		/* Panel for the number of client */
		JPanel nbDepotsPane = new JPanel();
		nbDepotsPane.add(new JLabel("Number of possible locations"));
		for(int i = 1; i < 11; i++) 
			this.nbDepotsBox.addItem(i);
		nbDepotsPane.add(this.nbDepotsBox);

		/* Panel for the fixed cost for opening a depot */
		JPanel fcPane = new JPanel();
		fcPane.add(new JLabel("Fixed opening cost"));
		this.fcBox.addItem(1000);
		this.fcBox.addItem(5000);
		this.fcBox.addItem(10000);
		fcPane.add(this.fcBox);

		/* Choice panel to decide between selecting an existing depots map or creating a custom new one */
		this.createButton = new JRadioButton("Create a new custom depots map");
		this.selectButton = new JRadioButton("Select at random among existing depots maps", true);
		ButtonGroup mapBG = new ButtonGroup();
		mapBG.add(this.createButton);
		mapBG.add(this.selectButton);

		/* Panel dealing with the map features */ 
		JPanel mapsPane = new JPanel();
		mapsPane.setLayout(new GridLayout(2,2));
		mapsPane.add(nbDepotsPane);
		mapsPane.add(fcPane);
		mapsPane.add(this.createButton);
		mapsPane.add(this.selectButton);

		/* ButtonGroup to choose the way to compute ordering costs */
		ButtonGroup ocBG = new ButtonGroup();
		this.distButton = new JRadioButton("Ordering costs computed using the distance with the supplier", true);
		this.customButton = new JRadioButton("Custom ordering costs", false);
		ocBG.add(this.distButton);
		ocBG.add(this.customButton);

		/* Panel for the ordering cost of the depots */
		this.ocPane.setLayout(new GridLayout(1,2));
		JPanel distPane = new JPanel(new BorderLayout());
		distPane.add(this.distButton, BorderLayout.NORTH);
		this.ocPane.add(distPane);
		this.customOCPane.add(this.customButton, BorderLayout.NORTH);
		this.ocPane.add(this.customOCPane);
		this.ocPane.setVisible(false);

		/* Add all the panels to the DepotsPanel object */
		this.add(mapsPane, BorderLayout.NORTH);
		this.add(ocPane);

		/* add all the ActionListener to the button on the panel */
		this.createButton.addActionListener(this);
		this.selectButton.addActionListener(this);
		this.customButton.addActionListener(this);
		this.distButton.addActionListener(this);
		this.nbDepotsBox.addActionListener(this);
	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the values of the ordering costs for each depot
	 */
	private double[] getOCs() {
		double[] ocValues = new double[this.getNbSites()];
		if(this.distButton.isSelected()) {
			for(int dIter=0; dIter < ocValues.length; dIter++) {
				ocValues[dIter] = -1;
			}
		}
		else {
			for(int dIter=0; dIter < ocValues.length; dIter++) {
				ocValues[dIter] = (double) this.ocFields[dIter].getValue();
			}
		}
		return ocValues;
	}

	/**
	 *
	 * @return	the number of sites selected for this map
	 */
	private int getNbSites() {
		return (int) this.nbDepotsBox.getSelectedItem();
	}
	
	/**
	 * 
	 * @return	the value of the fixed cost selected for the depots
	 */
	private double getFC() {
		return (double) this.fcBox.getSelectedItem();
	}

	/*
	 * METHODS
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource() == this.customButton) {
			activateOCFields();
		}
		if(e.getSource() == this.distButton) {
			desactivateOCFields();
		}
		if(e.getSource() == this.nbDepotsBox && this.customButton.isSelected()) {
			activateOCFields();
		}
		if(e.getSource() == this.selectButton) {
			this.ocPane.setVisible(false);
		}
		if(e.getSource() == this.createButton) {
			this.ocPane.setVisible(true);
		}
	}

	/**
	 * Change the panel by showing the ordering cost choices
	 */
	private void activateOCFields() {
		this.customOCPane.removeAll();
		this.customOCPane.add(this.customButton, BorderLayout.NORTH);
		this.ocFields = new JFormattedTextField[this.getNbSites()];
		JPanel ocFieldsPane = new JPanel(new GridLayout(Parameters.max_nb_depots, 2));
		for(int dIndex = 0; dIndex < this.ocFields.length; dIndex++) {
			ocFieldsPane.add(new JLabel("Depot " + (dIndex+1) + ":"));
			this.ocFields[dIndex] = new JFormattedTextField(doubleFormat);
			ocFieldsPane.add(this.ocFields[dIndex]);
		}
		this.customOCPane.add(ocFieldsPane);
		this.customOCPane.revalidate();
		this.customOCPane.repaint();
		if(this.createButton.isSelected()) {
			this.customOCPane.setVisible(true);
		}
	}

	/**
	 * Change the panel by hiding the ordering cost choices
	 */
	private void desactivateOCFields() {
		this.customOCPane.removeAll();
		this.customOCPane.add(this.customButton, BorderLayout.NORTH);
		this.customOCPane.revalidate();
		this.customOCPane.repaint();
		this.customOCPane.setVisible(true);
	}
	
	/**
	 * 
	 * @return	a DepotsMap object generated according to the characteristics decided by the user
	 */
	public DepotsMap generateDepotsMap(){
		try {
			return new DepotsMap(Parameters.grid_size, this.getFC(), this.getOCs(), 0, -1, new Location(new Point2D.Double(Parameters.grid_size/2, Parameters.grid_size/2)));
		}
		catch (IOException ioe) {
			System.out.println("ERROR :" + ioe.getMessage());
			System.exit(0);
		}
		
		return null;
	}
}
