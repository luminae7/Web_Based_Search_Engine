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
	private HashSet<String> sensitiveWords;
	
	public StopStem(String stopwords, String sensitivewords)
	{
		super();
		porter = new Porter();
		stopWords = new HashSet<String>();
		sensitiveWords = new HashSet<String>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(stopwords));
			String line;
			while ((line = br.readLine()) != null) {
				stopWords.add(line);
			}
			br.close();
			
			br = new BufferedReader(new FileReader(sensitivewords));
			while ((line = br.readLine()) != null) {
				sensitiveWords.add(line);
			}
			br.close();
		} catch (IOException e) {
			System.err.println(e.toString());
		} 
	}
	
	public boolean isStopWord(String str)
	{
		return stopWords.contains(str);	
	}
	
	public boolean isSensitiveWord(String str)
	{
		return sensitiveWords.contains(str);	
	}
	
	public String stem(String str)
	{
		return porter.stripAffixes(str);
	}
}
