package instanceManager;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class InstanceFilter extends FileFilter {

	/**
	 * Accept a file only if it displays the right extension
	 */
	@Override
	public boolean accept(File f) {
		 if (f.isDirectory()) {
	            return true;
	        }

	        String extension = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');
	        if (i > 0 &&  i < s.length() - 1) {
	            extension = s.substring(i+1).toLowerCase();
	        }
	        
	        if (extension != null) {
	            if (extension.equals("json")) {
	                    return true;
	            } else {
	                return false;
	            }
	        }

	        return false;
	}

	/**
	 * Message to display if a file extension is not supported
	 */
	@Override
	public String getDescription() {
		return "Only JSON files";
	}

}
