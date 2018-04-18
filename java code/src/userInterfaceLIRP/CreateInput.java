package userInterfaceLIRP;

import java.io.IOException;

import org.json.JSONObject;

import tools.JSONParser;

public class CreateInput {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JSONObject jsonInput = new JSONObject();
		jsonInput.put("inst dir", "Instances/Complete/Small/");
		jsonInput.put("log dir", "Log files/");
		jsonInput.put("sol dir", "Solutions/");
		jsonInput.put("desc", "simpleCity_");

		try {
			JSONParser.writeJSONToFile(jsonInput, "../input.json");
		}
		catch (IOException e) {
			System.out.print("ERR while trying to write the input JSON file: ");
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

}
