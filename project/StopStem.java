package project;

import java.io.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

public class StopStem
{
	private Porter porter;
	private HashSet<String> stopWords;
	public boolean isStopWord(String str)
	{
		return stopWords.contains(str);	
	}
	public StopStem(String str)
	{
		super();
		porter = new Porter();
		stopWords = new HashSet<String>();
				
		// use BufferedReader to extract the stopwords in stopwords.txt (path passed as parameter str)
		// add them to HashSet<String> stopWords
		// MODIFY THE BELOW CODE AND ADD YOUR CODES HERE
		
		// stopWords.add("is");
		// stopWords.add("am");
		// stopWords.add("are");
		// stopWords.add("was");
		// stopWords.add("were");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(str));
			String line;
			while ((line = br.readLine()) != null) {
				stopWords.add(line);
			}
		} catch (IOException e) {
			System.err.println(e.toString());
		} 
	}
	public String stem(String str)
	{
		return porter.stripAffixes(str);
	}
}
