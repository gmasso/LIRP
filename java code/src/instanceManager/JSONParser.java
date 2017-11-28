package instanceManager;

import java.io.*;
import org.json.JSONObject;

public final class JSONParser {
	
	private JSONParser() {}
    
	public static JSONObject readJSONFromFile(String filename) {
	    String jsonString = "";
	    try {
	        BufferedReader br = new BufferedReader(new FileReader(filename));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        while (line != null) {
	            sb.append(line);
	            line = br.readLine();
	        }
	        jsonString = sb.toString();
	        br.close();
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	    return new JSONObject(jsonString);
	}

	public static void writeJSONToFile(JSONObject jsonToWrite, String filename) throws IOException{
		// Create a new file
		File jsonFile = new File(filename);
		if(!jsonFile.exists()) {
			jsonFile.createNewFile();
		}
		
		// Create a new FileWriter object
		FileWriter fileWriter = new FileWriter(jsonFile);

		// Write the jsonObject into the file
		fileWriter.write(jsonToWrite.toString());
		fileWriter.close();
	}

	// To count the number of files in a folder
	public static int countFiles(String pathToFolder) {
		File f = new File(pathToFolder);
		int count = 0;
		for (File file : f.listFiles()) {
			if (file.isFile()) {
				count++;
			}
		}
		return count;
	}
	
}
