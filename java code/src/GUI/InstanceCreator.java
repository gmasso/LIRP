package GUI;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Dialog;
import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import instanceManager.ClientsMap;
import instanceManager.DepotsMap;
import instanceManager.Instance;
import instanceManager.Parameters;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.BorderFactory;

import java.text.NumberFormat;

public class InstanceCreator extends JDialog implements ActionListener, PropertyChangeListener {
	public static final long serialVersionUID = 42L;
	
	private JButton cancelB = new JButton("Cancel");
	private JButton genB = new JButton("Generate!");
	private JPanel mapsPane;
	private VehiclesPanel vehiclesPane;
	private DepotsPanel depotsPane;
	private ClientsPanel clientsPane;
	private JFormattedTextField  nbMapsField = new JFormattedTextField(NumberFormat.getIntegerInstance());
	

	private static WindowListener closeWindow = new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
			e.getWindow().dispose();
		}
	};

	public InstanceCreator(JFrame instFrame) {
		// Create the app to generate the instance
		super(instFrame, "Instance creator", Dialog.ModalityType.DOCUMENT_MODAL);
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.setLayout(new BorderLayout());
		this.setBounds(instFrame.getBounds());

		this.mapsPane = new JPanel(new BorderLayout());
		this.mapsPane.setBorder(BorderFactory.createTitledBorder("General"));
		this.nbMapsField.setValue(1);
		this.mapsPane.add(new JLabel("Number of instances to generate:"), BorderLayout.LINE_START);
		this.mapsPane.add(this.nbMapsField);
		
		JPanel layersPane = new JPanel();
		layersPane.setLayout(new GridLayout(1,2));

		try {
			this.depotsPane = new DepotsPanel();
			this.clientsPane = new ClientsPanel();
			this.vehiclesPane = new VehiclesPanel();
		}
		catch (IOException ioe) {
			System.out.println("ERROR :" + ioe.getMessage());
			System.exit(0);
		}

		// Add the panels to the final instance creator panel
		layersPane.add(this.depotsPane);
		layersPane.add(this.clientsPane);
		
		// Position the buttons at the bottom of the window
		JPanel bottomPane = new JPanel(new GridLayout(1,2));
		bottomPane.add(this.vehiclesPane);
		JPanel finalPane = new JPanel(new BorderLayout());
		finalPane.add(createButtonPanel(), BorderLayout.PAGE_END);
		bottomPane.add(finalPane);

		JPanel mainPane = new JPanel();
		mainPane.setLayout(new BorderLayout());
		mainPane.add(this.mapsPane, BorderLayout.PAGE_START);
		mainPane.add(layersPane, BorderLayout.CENTER);
		mainPane.add(bottomPane, BorderLayout.PAGE_END);

//		// Define the directory in which to store the clients map
//		String pathToCFolder = "Instances/Layers/Clients/" + nbCities+"_cities";
//		if(nbDepots > 0) {
//			pathToCFolder += "/ur" + (int) (urbanRatio * 100);
//		}

//		//If it does not exist, create it
//		File layerCFolder = new File(pathToCFolder);
//		layerCFolder.mkdirs();
//		// Define the index of the clients map
//		int cmapIndex = JSONParser.countFiles(pathToCFolder);
//		// Create the clients layer
//	//	ClientsMap cMap = new ClientsMap(gridSize, nbClients, citiesSizes, urbanRatio);
//		// Write the layer obtained to a JSON file
//	//	cMap.writeToJSONFile(pathToCFolder + "/cMap_" + cmapIndex + ".json");
//		System.out.println("Done.");
		
		// Tell the frame that it contains the panel 
		this.setContentPane(mainPane);               
		this.pack();
	}

	private JPanel createButtonPanel() {
		// Add the buttons to the bottom of the panel
		this.genB.addActionListener(this);
		this.cancelB.addActionListener(this);

		JPanel buttonPane = new JPanel();
		buttonPane.add(this.genB);
		buttonPane.add(this.cancelB);
		
		JPanel resultPane = new JPanel(new BorderLayout());
		resultPane.add(buttonPane, BorderLayout.LINE_END);
		return resultPane;
	}


	public void actionPerformed(ActionEvent e) {

		if(e.getSource() == this.genB) {
			int nbMaps = (int) this.nbMapsField.getValue();
			
			int dMapsIndex = new File("../Instances/Layers/Depots").listFiles().length;
			int cMapsIndex = new File("../Instances/Layers/Clients/"+this.clientsPane.getCitySizes().length+"_cities/").listFiles().length;

			try {
				for(int instIndex = 0; instIndex < nbMaps; instIndex++) {
					ClientsMap cMap = this.clientsPane.generateClientsMap();
					DepotsMap dMap = this.depotsPane.generateDepotsMap();
					double[] vCapacities = new double[this.vehiclesPane.getNbVehicles()];
					for (int vIter = 0; vIter < vCapacities.length; vIter++) {
						vCapacities[vIter] = this.vehiclesPane.getCapa();
					}
					Instance inst = new Instance(Parameters.grid_size, clientsPane.getHorizon(), dMap, cMap, vCapacities);
					System.out.println(this.clientsPane.getCitySizes().length);
					System.out.println((int) this.clientsPane.getUrbanRatio());
					System.out.println("Instances/Layers/Clients/"+this.clientsPane.getCitySizes().length+"_cities/ur"+ ((int) this.clientsPane.getUrbanRatio()));
					System.out.println(new File("Instances/Layers/Clients/"+this.clientsPane.getCitySizes().length+"_cities/ur"+ (int) this.clientsPane.getUrbanRatio()).listFiles().length);
					System.out.println("Instances/Layers/Depots");
					System.out.println(new File("Instances/Layers/Depots").listFiles().length);
					//inst.writeToJSONFile(instFilename);
				}

			}
			catch (IOException ioe) {
				System.out.println("ERROR :" + ioe.getMessage());
				System.exit(0);
			}
		}
		else {
			this.dispose();
		}
	}

	public double getGridSize() {
		return this.gridSize;
	}


	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
	//	this.gridSize = ((Number) this.sizeField.getValue()).doubleValue();
	}
}
