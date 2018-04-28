package userInterfaceLIRP;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import instanceManager.ClientsMap;
import tools.Config;

public class ClientsPanel extends JPanel implements ActionListener{

	/*
	 * STATIC ATTRIBUTES
	 */
	private static final long serialVersionUID = 1L;	
	
	/*
	 * ATTRIBUTES
	 */
	private JFormattedTextField[] probaSizes;
	private double[] possibleSizes = {0.03, 0.06, 0.09, 0.12, 0.18} ;
	private double[] sizesCDF = {0.25, 0.60, 0.8, 0.92, 1}; 
	private double[] urbanRatios = {0.5, 0.75, 0.95};

	private JRadioButton createButton = new JRadioButton("Create a new custom clients map");
	private JRadioButton selectButton= new JRadioButton("Select at random among existing clients maps", true);
	private JRadioButton uniformButton = new JRadioButton("Uniform", true);
	private JRadioButton normalButton = new JRadioButton("Gaussian");
	private 	JComboBox<Integer> nbClientsBox = new JComboBox<Integer>();
	private 	JComboBox<Integer> citiesBox = new JComboBox<Integer>();
	private JComboBox<Integer> urCBox = new JComboBox<Integer>();
	private JComboBox<Integer> holdingCBox = new JComboBox<Integer>();
	private JComboBox<Integer> periodCBox = new JComboBox<Integer>();
	
	private JPanel probaPane= new JPanel();
	private JLabel periodLabel = new JLabel("Demand period:");
	private JLabel aperiodLabel = new JLabel(" (0 if aperiodic)");
 
	private NumberFormat doubleFormat = NumberFormat.getNumberInstance();
	private	NumberFormat percentFormat = NumberFormat.getIntegerInstance();
	private NumberFormat integerFormat = NumberFormat.getIntegerInstance();
	private JFormattedTextField minField, maxField, horizonField;


	/**
	 * Creates a new ClientsPanel object
	 * @throws IOException
	 */
	public ClientsPanel() throws IOException {
		super(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Clients"));
		this.doubleFormat.setMaximumFractionDigits(2);
		this.percentFormat.setMinimumIntegerDigits(1);
		this.percentFormat.setMaximumIntegerDigits(3);
		this.integerFormat.setMinimumIntegerDigits(1);
		this.integerFormat.setMaximumIntegerDigits(4);

		this.probaSizes = new JFormattedTextField[sizesCDF.length];
		this.probaSizes[0] = new JFormattedTextField(percentFormat);
		this.probaSizes[0].setValue((int) (this.sizesCDF[0]*100));
		for(int pIter = 1; pIter < sizesCDF.length; pIter++) {
			this.probaSizes[pIter] = new JFormattedTextField(percentFormat);
			this.probaSizes[pIter].setValue((int) ((this.sizesCDF[pIter]-this.sizesCDF[pIter-1])*100));
		}

		/* Set the combo box to select the number of client */
		this.nbClientsBox.addItem(10);
		for(int i = 25; i < 301; i += 25) 
			this.nbClientsBox.addItem(i);
		this.nbClientsBox.setSelectedIndex(this.nbClientsBox.getItemCount()-1);

		/* Set the combo box to select the number cities */
		for(int cIter = 0; cIter < possibleSizes.length; cIter++) {
			this.citiesBox.addItem(cIter);
		}

		/* Set the combo box to select the urban ratio */
		for(int urIndex=0; urIndex < this.urbanRatios.length; urIndex++)
			this.urCBox.addItem((int) (this.urbanRatios[urIndex] * 100)); 

		/* Set the combo box to select the holding cost ratio */
		JPanel holdingPane = new JPanel(new GridLayout(2,1));
		this.holdingCBox.addItem(25); 
		this.holdingCBox.addItem(50); 
		this.holdingCBox.addItem(75); 
		this.holdingCBox.addItem(100); 
		this.holdingCBox.setSelectedIndex(this.holdingCBox.getItemCount()-1);
		
		/* Panel to select the parameters of the clients map */
		JPanel paramPane = new JPanel();
		paramPane.setLayout(new GridLayout(2,4));
		paramPane.add(new JLabel("Number of clients"));
		paramPane.add(this.nbClientsBox);
		/* Panel to select the holding cost of the clients */
		holdingPane.add(new JLabel("Holding cost ratio"));
		holdingPane.add(new JLabel("(% of depot holding cost):"));
		paramPane.add(holdingPane);
		paramPane.add(this.holdingCBox);
		/* Panel to select the cities parameters */
		paramPane.add(new JLabel("Number of cities"));
		paramPane.add(this.citiesBox);
		paramPane.add(new JLabel("Urban ratio (%):"));
		paramPane.add(this.urCBox);

		/* Panel for the main buttons */
		JPanel choicePane = new JPanel(new GridLayout(1,2));
		ButtonGroup mapBG = new ButtonGroup();
		mapBG.add(this.createButton);
		mapBG.add(this.selectButton);
		choicePane.add(this.createButton);
		choicePane.add(this.selectButton);
		this.createButton.addActionListener(this);
		this.selectButton.addActionListener(this);;

		/* Panel to group the main parameters and the button */
		JPanel mainPane = new JPanel(new GridLayout(2,1));
		mainPane.add(paramPane);
		mainPane.add(choicePane);

		/* Panel to select the probability of cities sizes */
		this.probaPane.setBorder(BorderFactory.createTitledBorder("City sizes probabilities"));
		this.probaPane.setLayout(new GridLayout(2,6));
		this.probaPane.add(new JLabel("City sizes:"));
		for(int cIter = 0; cIter < this.possibleSizes.length; cIter++) {
			this.probaPane.add(new JLabel(Integer.toString((int) (possibleSizes[cIter] * 100))));
		}
		this.probaPane.add(new JLabel("Drawing probability:"));
		for(int cIter = 0; cIter < this.possibleSizes.length; cIter++) {
			this.probaPane.add(probaSizes[cIter]);
		}
		this.probaPane.setVisible(false);

		/* Integrate the main panel and the optional probabilities panel to the one of the clients map */
		JPanel mapsPane = new JPanel(new GridLayout(2,1));
		mapsPane.add(mainPane);
		mapsPane.add(this.probaPane);

		/* Fill all the fields relative to the demand parameters */
		this.minField = new JFormattedTextField(doubleFormat);
		this.minField.setValue(0);
		this.maxField = new JFormattedTextField(doubleFormat);
		this.maxField.setValue(10);
		this.horizonField = new JFormattedTextField(integerFormat);
		this.horizonField.setValue(50);

		/* Buttons to select between uniform or normally distributed demand */
		ButtonGroup demandsBG = new ButtonGroup();
		demandsBG.add(this.uniformButton);
		demandsBG.add(this.normalButton);
		
		this.uniformButton.addActionListener(this);
		this.normalButton.addActionListener(this);

		/* Gives the possibility for the user to select a cycle length for the demand pattern */
		this.periodCBox.addItem(0);
		this.periodCBox.addItem(7);
		this.periodCBox.addItem(30);
		this.periodCBox.addItem(52);
		this.periodLabel.setVisible(false);
		this.periodCBox.setVisible(false);
		this.aperiodLabel.setVisible(false);
		
		/* Fill the demand panel with the corresponding information */
		JPanel demandsPane = new JPanel(new GridLayout(4,4));
		demandsPane.setBorder(BorderFactory.createTitledBorder("Demands"));
		demandsPane.add(new JLabel("Number of periods:"));
		demandsPane.add(this.horizonField);
		demandsPane.add(new JPanel());
		demandsPane.add(new JPanel());
		demandsPane.add(new JLabel("min"));
		demandsPane.add(this.minField);
		demandsPane.add(new JLabel("max"));
		demandsPane.add(this.maxField);
		demandsPane.add(new JLabel("Type of distribution:"));
		demandsPane.add(this.uniformButton);
		demandsPane.add(this.normalButton);
		demandsPane.add(new JPanel());
		demandsPane.add(this.periodLabel);
		demandsPane.add(this.periodCBox);
		demandsPane.add(this.aperiodLabel);
		demandsPane.add(new JPanel());
		
		this.add(mapsPane, BorderLayout.NORTH);
		this.add(demandsPane, BorderLayout.SOUTH);
	}
	
	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the number of clients on the map
	 */
	private int getNbSites() {
		return (int) this.nbClientsBox.getSelectedItem();
	}
	
	/**
	 * 
	 * @return	the number of cities on the map
	 */
	private int getNbCities() {
		return (int) this.citiesBox.getSelectedItem();
	}
	
	/**
	 * 
	 * @return	the urban ratio on this map
	 */
	public double getUrbanRatio() {
		return (double) (((int) this.urCBox.getSelectedItem())/100.0);
	}
	
	/**
	 * 
	 * @return	the array containing the city sizes
	 */
	public double[] getCitySizes() {
		this.updateSizesCDF();
		return this.selectCitiesSizes();
	}
	
	/**
	 * 
	 * @return	the holding cost (expressed as a fraction of the depots holding cost)
	 */
	private double getHoldingCost() {
		return (double) (((int) this.holdingCBox.getSelectedItem())/100.0);
	}
	
	/**
	 * 
	 * @return	the number of periods in the planning horizon
	 */
	public int getHorizon() {
		return ((Number) this.horizonField.getValue()).intValue();
	}
	
	/**
	 * 
	 * @return	the cycle length in the demand pattern
	 */
	private int getPeriod() {
		return (int) this.periodCBox.getSelectedItem();
	}
	
	/**
	 * 
	 * @return	True if the distribution of the demand is uniform, False if it is gaussian
	 */
	private boolean isUniform() {
		return this.uniformButton.isSelected();
	}
	
	/**
	 * 
	 * @return	the lower bound on the demand value in each period
	 */
	private double getMinDemand() {
		return ((Number) this.minField.getValue()).intValue();
	}
	
	/**
	 * 
	 * @return	the upper bound on the demand value in each period
	 */
	private double getMaxDemand() {
		return ((Number) this.maxField.getValue()).intValue();
	}

	/*
	 * METHODS
	 */
	/**
	 * Draw the cities sizes at random according to the weights chosen by the user for each size
	 * @return	the array containing the sizes selected for each city on the map
	 */
	private double[] selectCitiesSizes() {
		// Array to store the different cities sizes
		double[] citiesSizes = new double[this.getNbCities()];

		// Loop through the different cities to select their sizes
		for(int cIndex = 0; cIndex < citiesSizes.length; cIndex++) {
			int sizeIndex = 0;
			// Draw a random number
			double proba = Config.RAND.nextDouble();
			// Determine to which size it corresponds
			while(proba > this.sizesCDF[sizeIndex]) {
				sizeIndex++;
			}
			// Set its size accordingly
			citiesSizes[cIndex] = this.possibleSizes[sizeIndex];
		}
		return citiesSizes;
	}
	
	/**
	 * Updates the city sizes to ensure they sum up to one
	 */
	private void updateSizesCDF() {
		double totalWeight = 0;
		for(int sIndex = 0; sIndex < this.probaSizes.length; sIndex++) {
			int percentVal = ((Number) this.probaSizes[sIndex].getValue()).intValue();
			totalWeight += percentVal / 100.0;
			this.sizesCDF[sIndex] = totalWeight;
		}
		this.sizesCDF[0] /= totalWeight;
		this.probaSizes[0].setValue((int) (this.sizesCDF[0] * 100));
		for(int sIndex = 1; sIndex < this.probaSizes.length; sIndex++) {
			this.sizesCDF[sIndex] /= totalWeight;
			this.probaSizes[sIndex].setValue((int) ((this.sizesCDF[sIndex]-this.sizesCDF[sIndex-1]) * 100));
			try {
				this.probaSizes[sIndex].commitEdit();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Generate a clients map according to the parameters selected in the panel
	 * @return	a ClientsMap object generated from the choices of the user
	 */
	public ClientsMap generateClientsMap() {
		try {
			return new ClientsMap(Config.grid_size, this.getNbSites(), this.getCitySizes(), this.getUrbanRatio(), this.getHoldingCost(), this.getHorizon(), this.getPeriod(), this.isUniform(), this.getMinDemand(), this.getMaxDemand());
		}
		catch (IOException ioe) {
			System.out.println("ERROR :" + ioe.getMessage());
			System.exit(0);
		}
		
		return null;
	}
	
	/**
	 *	Listener to the action performed on the panel
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource() == this.selectButton) {
			this.probaPane.setVisible(false);
		}
		if(e.getSource() == this.createButton) {
			this.probaPane.setVisible(true);
		}
		if(e.getSource() == this.uniformButton) {
			this.periodLabel.setVisible(false);
			this.periodCBox.setVisible(false);
			this.aperiodLabel.setVisible(false);
		}
		if(e.getSource() == this.normalButton) {
			this.periodLabel.setVisible(true);
			this.periodCBox.setVisible(true);
			this.aperiodLabel.setVisible(true);
		}
		updateSizesCDF();
		this.revalidate();
		this.repaint();
	}
}
