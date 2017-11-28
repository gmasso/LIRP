package userInterfaceLIRP;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InstanceChooser extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private final String newline = "\n";
    private JButton openButton, cancelButton;
    private JTextArea log;
    private JFileChooser fc;
 
    public InstanceChooser() {
        super(new BorderLayout());
 
        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
 
        //Create a file chooser
        fc = new JFileChooser();
 
        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
 
        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        this.openButton = new JButton("Select instances...");
        this.openButton.addActionListener(this);
 
        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        //saveButton = new JButton("Save a File...");
        //saveButton.addActionListener(this);
        this.cancelButton = new JButton("Cancel");
        this.cancelButton.addActionListener(this);
        
        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);
        buttonPanel.add(cancelButton);
 
        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }
 
    public void actionPerformed(ActionEvent e) {
 
        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(InstanceChooser.this);
            fc.setMultiSelectionEnabled(true);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	File[] files = fc.getSelectedFiles();
            	for(int findex = 0; findex < files.length; findex++) {
            		//This is where a real application would open the file.
            		log.append("Opening: " + files[findex].getName() + "." + newline);
            	}
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
 
        //Handle save button action.
        }
    }
}
