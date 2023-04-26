package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import org.htmlparser.beans.StringBean;
import org.htmlparser.util.ParserException;
import org.jsoup.Jsoup;

import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

public class Indexer {
	
	public Database PageIDtoLength;
	
	public Database WordIDtoWord;
	public Database WordtoWordID;
	private static int WordIndex;
	
	public Database PageIDtoWordID;
	public Database WordIDtoPageID;
	public Database PageIDtoTopFiveWordID;
	
	public Database PageIDtoTitleWordID;
	public Database TitleWordIDtoPageID;
	
	private static StopStem stopStem;
	
	public Indexer() throws IOException
	{
		// create mapping table for PageID and Header
		PageIDtoLength = new Database("PageIDtoLength", "1");
		
		// create mapping table for WordID and Word
		WordIDtoWord = new Database("WordIDtoWord", "1");
		WordtoWordID = new Database("WordtoWordID", "1");
		WordIndex = WordIDtoWord.size();
				
		// create forward and backward index for PageID and WordID
		PageIDtoWordID = new Database("PageIDtoWordID", "1");
		WordIDtoPageID = new Database("WordIDtoPageID", "1");
		// create forward index for PageID and Top Five WordID
		PageIDtoTopFiveWordID = new Database("PageIDtoTopFiveWordID", "1");
		
		// create forward and backward index for PageID and Title's WordID
		PageIDtoTitleWordID = new Database("PageIDtoTitleWordID", "1");
		TitleWordIDtoPageID = new Database("TitleWordIDtoPageID", "1");
		
		stopStem = new StopStem("project/stopwords.txt");
	}
	
	public static String extractHTMLLength(String url) throws Exception
	{
		try {
			// get the raw HTML from url
	        StringBuilder result = new StringBuilder();
	        URL urll = new URL(url);
	        HttpURLConnection connection = (HttpURLConnection) urll.openConnection();
	        connection.setRequestMethod("GET");
	        try (BufferedReader reader = new BufferedReader(
	        		new InputStreamReader(connection.getInputStream()))) {
	    		for (String line; (line = reader.readLine()) != null; ) {
	    			result.append(line);
	        	}
	        }
	        return Integer.toString(result.toString().length());
		}
		catch(Exception e) {
			// if cannot get the HTML, certification error
			// return null
			return null;
		}
	}
	
	public void storeTitle(String PageID, String title) throws IOException
	{
		// count the word frequency
		String[] strings = title.split("\\s+");
		Vector<String> words = new Vector<String>();
		// stop and stem the words
		for (String string : strings)
			if (!stopStem.isStopWord(string.toLowerCase()))
				words.add(stopStem.stem(string.toLowerCase()));
		Database wordfreq = new Database("titlewordfreq", "1");
		HTree hashtable = wordfreq.countWords(words);
		
		// save the text frequency
		FastIterator iter = hashtable.keys();
		String key;
		String WordID;
		String freq;
		int count = 0;
		while((key = (String)iter.next())!=null)
		{
			freq = (String) hashtable.get(key);
			
			WordID = WordtoWordID.get(key);
			// if new word then add to WordIDtoWord table
			if (WordID == null) {
				WordIDtoWord.add(Integer.toString(WordIndex), key);
				WordtoWordID.add(key, Integer.toString(WordIndex));
				WordID = Integer.toString(WordIndex);
				WordIndex++;
				
				// add to WordIDtoPageID and TitleWordIDtoPageID
				WordIDtoPageID.add(WordID, "");
				TitleWordIDtoPageID.add(WordID, PageID+" "+freq+";");
			} else {
				// add to TitleWordIDtoPageID
				TitleWordIDtoPageID.appendFreq(WordID, PageID, freq);
			}
			
			// add to PageIDtoTitleWordID
			if (count == 0)
				PageIDtoTitleWordID.add(PageID, WordID+" "+freq+";");
			else
				PageIDtoTitleWordID.appendFreq(PageID, WordID, freq);
			
			count++;
		}
	}
	
	public static Vector<String> extractWords(String url) throws ParserException
	{
		// extract words in url and return them
		Vector<String> words = new Vector<String>();
		StringBean sb =  new StringBean();
		sb.setLinks(true);
		sb.setURL(url);
		
		// split with space characters
		String[] strings = sb.getStrings().split("\\s+");
		
		// stop and stem the string
		// add the strings into vector of strings
		for (String string : strings) {
			if (!stopStem.isStopWord(string.toLowerCase())) {
				words.add(stopStem.stem(string.toLowerCase()));
			}
		}
		
		return words;
	}
	
	public void storeWords(String PageID, String url) throws Exception
	{
		// get the text on the web page
		Vector<String> words = extractWords(url);
		
		// print the size of the page
		URL urll = new URL(url);
		URLConnection connection = urll.openConnection();
		String context_size = connection.getHeaderField("Content-Length");
		// save all context-length, HTML Length and number of words extracted
		PageIDtoLength.add(PageID, context_size+";"+extractHTMLLength(url)+";"+words.size());
		
		// count the word frequency
		Database wordfreq = new Database("wordfreq", "1");
		HTree hashtable = wordfreq.countWords(words);
		// find the top five stemmed words and their frequencies
		String[] TopFive = wordfreq.TopFive;
		int[] TopFiveVal = wordfreq.TopFiveVal;
		
		// save the text frequency
		FastIterator iter = hashtable.keys();
		String key;
		String WordID;
		String freq;
		int count = 0;
		while((key = (String)iter.next())!=null)
		{
			freq = (String) hashtable.get(key);
			
			WordID = WordtoWordID.get(key);
			// if new word then add to WordIDtoWord table
			if (WordID == null) {
				WordIDtoWord.add(Integer.toString(WordIndex), key);
				WordtoWordID.add(key, Integer.toString(WordIndex));
				WordID = Integer.toString(WordIndex);
				WordIndex++;
				
				// add to WordIDtoPageID and TitleWordIDtoPageID
				WordIDtoPageID.add(WordID, PageID+" "+freq+";");
				TitleWordIDtoPageID.add(WordID, "");
			} else {
				// add to WordIDtoPageID
				WordIDtoPageID.appendFreq(WordID, PageID, freq);
			}
			
			// add to PageIDtoWordID
			if (count == 0) {
				PageIDtoWordID.add(PageID, WordID+" "+freq+";");
			}
			else {
				PageIDtoWordID.appendFreq(PageID, WordID, freq);
			}
			
			count++;
		}
		
		// save the top five words
		for (int i = 0; i < 5; i++) {
			WordID = WordtoWordID.get(TopFive[i]);
			freq = String.valueOf(TopFiveVal[i]);
			if (i == 0)
				PageIDtoTopFiveWordID.add(PageID, WordID+" "+freq+";");
			else
				PageIDtoTopFiveWordID.appendFreq(PageID, WordID, freq);
		}
	}
}