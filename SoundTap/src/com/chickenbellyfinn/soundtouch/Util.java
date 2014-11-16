package com.chickenbellyfinn.soundtouch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Util {
	
	public static String readStream(InputStream is){
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String response = "";
		String line;
		try {
			while((line = reader.readLine()) != null){
				response += line;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}

}
