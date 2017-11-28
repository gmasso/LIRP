package instanceManager;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Visualizer extends javax.swing.JFrame {

	private Graphics2D g2d;
	private JPanel panel;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static double diam = 1;

	public Visualizer(Instance inst) {

		super("Scatterplot");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		if(panel!= null)
			panel.removeAll();

		panel = new JPanel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g2d = (Graphics2D) g.create();
				// Draw clients as blue circles on the map
				g.setColor(Color.BLUE);
				for (int cIndex = 0; cIndex < inst.getNbClients(); cIndex++) {
					Point2D.Double pt = (Point2D.Double) inst.getClient(cIndex).getCoordinates();
					Ellipse2D dot = new Ellipse2D.Double(pt.x - diam/2, pt.y - diam/2, diam, diam);
					g2d.fill(dot);
				}
				// Draw depots as red triangles on the map
				g.setColor(Color.RED);
				for (int dIndex = 0; dIndex < inst.getNbDepots(); dIndex++) {
					Point2D.Double pt = (Point2D.Double) inst.getDepot(dIndex).getCoordinates();
					GeneralPath triangle = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
					triangle.moveTo(pt.x - Math.sqrt(3) * diam / 2, pt.y - diam/2);
					triangle.lineTo(pt.x, pt.y + diam);
					triangle.moveTo(pt.x + Math.sqrt(3) * diam / 2, pt.y - diam/2);
					triangle.closePath();
					g2d.fill(triangle);
				}
				g2d.dispose();
			}
		};

		setContentPane(panel);
		setBounds(100, 100, 200, 200);

		setVisible(true);
	}

//	private void drawElements(Layer map) {
//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//		panel = new JPanel() {
//			/**
//			 * 
//			 */
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void paintComponent(Graphics g) {
//				super.paintComponent(g);
//				g2d = (Graphics2D) g.create();
//				// Draw clients as blue circles on the map
//				for (int locIndex = 0; locIndex < map.getNbSites(); locIndex++) {
//					Point2D.Double pt = (Point2D.Double) map.getSite(locIndex).getCoordinates();
//					if(map instanceof ClientsMap){
//						g.setColor(Color.BLUE);
//						Ellipse2D dot = new Ellipse2D.Double(pt.x - diam/2, pt.y - diam/2, diam, diam);
//						g2d.fill(dot);
//					}
//					else if(map instanceof DepotsMap) {
//						// Draw depots as red triangles on the map
//						g.setColor(Color.RED);
//						GeneralPath triangle = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
//						triangle.moveTo(pt.x - Math.sqrt(3) * diam / 2, pt.y - diam/2);
//						triangle.lineTo(pt.x, pt.y + diam);
//						triangle.moveTo(pt.x + Math.sqrt(3) * diam / 2, pt.y - diam/2);
//						triangle.closePath();
//						g2d.fill(triangle);
//					}
//					g2d.dispose();
//				}
//			}
//		};
//
//		setContentPane(panel);
//		setBounds(100, 100, 200, 200);
//
//		setVisible(true);
//	}
}