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
	
	public Database PageIDtoTitle;
	public Database PageIDtoLength;
	
	public Database WordIDtoWord;
	public Database WordtoWordID;
	private static int WordIndex;
	
	public Database PageIDtoTitleWordID;
	public Database TitleWordIDtoPageID;
	
	public Database PageIDtoWordID;
	public Database WordIDtoPageID;
	public Database PageIDtoTopFiveWordID;
	
	public Database PageIDtoBiTitleWordID;
	public Database BiTitleWordIDtoPageID;
	public Database PageIDtoTriTitleWordID;
	public Database TriTitleWordIDtoPageID;
	
	public Database PageIDtoBiWordID;
	public Database BiWordIDtoPageID;
	public Database PageIDtoTriWordID;
	public Database TriWordIDtoPageID;
	
	private static HTree titleHashtable;
	private static HTree BititleHashtable;
	private static HTree TrititleHashtable;
	
	private static StopStem stopStem;
	
	public Indexer() throws IOException
	{
		// create mapping table for PageID and Header
		PageIDtoTitle = new Database("PageIDtoTitle", "1");
		PageIDtoLength = new Database("PageIDtoLength", "1");
		
		// create mapping table for WordID and Word
		WordIDtoWord = new Database("WordIDtoWord", "1");
		WordtoWordID = new Database("WordtoWordID", "1");
		WordIndex = WordIDtoWord.size();
		
		// create forward and backward index for PageID and Title's WordID
		PageIDtoTitleWordID = new Database("PageIDtoTitleWordID", "1");
		TitleWordIDtoPageID = new Database("TitleWordIDtoPageID", "1");
				
		// create forward and backward index for PageID and WordID
		PageIDtoWordID = new Database("PageIDtoWordID", "1");
		WordIDtoPageID = new Database("WordIDtoPageID", "1");
		// create forward index for PageID and Top Five WordID
		PageIDtoTopFiveWordID = new Database("PageIDtoTopFiveWordID", "1");
		
		// create forward and backward index for bigrams and trigrams for title
		PageIDtoBiTitleWordID = new Database("PageIDtoBiTitleWordID", "1");
		BiTitleWordIDtoPageID = new Database("BiTitleWordIDtoPageID", "1");
		PageIDtoTriTitleWordID = new Database("PageIDtoTriTitleWordID", "1");
		TriTitleWordIDtoPageID = new Database("TriTitleWordIDtoPageID", "1");
		
		// create forward and backward index for bigrams and trigrams for words
		PageIDtoBiWordID = new Database("PageIDtoBiWordID", "1");
		BiWordIDtoPageID = new Database("BiWordIDtoPageID", "1");
		PageIDtoTriWordID = new Database("PageIDtoTriWordID", "1");
		TriWordIDtoPageID = new Database("TriWordIDtoPageID", "1");
		
		stopStem = new StopStem("stopwords.txt", "sensitivewords.txt");
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
	
	public static String extractTitle(String url) throws IOException
	{
		// print the title
		String title;
		try {
			title = Jsoup.connect(url).get().title();
		}
		// if cannot get the title, certification error
		catch (Exception e) {
			title = "No Title";
		}
		
		return title;
	}
	
	public void storeTitle(String PageID, String url) throws IOException
	{
		// get title
		String title = extractTitle(url);
		
		// store the title to PageIDtoTitle
		PageIDtoTitle.add(PageID, title);
		
		// count the word frequency
		String[] strings = title.split("\\s+");
		Vector<String> words = new Vector<String>();
		// stop and stem the words
		for (String string : strings) {
			string = string.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (string.compareTo("")!=0)
				if (!stopStem.isStopWord(string))
					words.add(stopStem.stem(string));
		}
		Database titleWordFreq = new Database("titlewordfreq", "1");
		titleHashtable = titleWordFreq.countWords(words);
		
		// save the text frequency
		FastIterator iter = titleHashtable.keys();
		String key;
		String WordID;
		String freq;
		int count = 0;
		while((key = (String) iter.next()) != null)
		{
			freq = (String) titleHashtable.get(key);
			
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
	
	public static Vector<String> extractWords(String url) throws ParserException, IOException
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
			string = string.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (string.compareTo("")!=0)
				if (!stopStem.isStopWord(string))
					words.add(stopStem.stem(string));
		}
		
		// remove title in words
		FastIterator iter = titleHashtable.keys();
		String key;
		while((key = (String) iter.next()) != null) {
			words.remove(key);
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
		Database wordFreq = new Database("wordfreq", "1");
		HTree hashtable = wordFreq.countWords(words);
		// find the top five stemmed words and their frequencies
		String[] TopFive = wordFreq.TopFive;
		int[] TopFiveVal = wordFreq.TopFiveVal;
		
		// save the text frequency
		FastIterator iter = hashtable.keys();
		String key;
		String WordID;
		String freq;
		int count = 0;
		while((key = (String) iter.next()) != null)
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
	
	public void storeTitleBigram(String PageID, String url) throws IOException
	{
		// get title
		String title = extractTitle(url);
		
		// count the word frequency
		String[] strings = title.split("\\s+");
		Vector<String> words = new Vector<String>();
		// stop and stem the words (only bigrams without stopwords)
		for (int i = 0; i < strings.length - 1; i++) {
			String firstWord = strings[i].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			String secondWord = strings[i+1].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (firstWord.compareTo("")!=0 && secondWord.compareTo("")!=0)
				if (!stopStem.isStopWord(firstWord) && !stopStem.isStopWord(secondWord))
					if (stopStem.stem(firstWord).compareTo("")!=0 && stopStem.stem(secondWord).compareTo("")!=0)
						words.add(stopStem.stem(firstWord) + " " + stopStem.stem(secondWord));
		}
		Database titleWordFreq = new Database("titlewordfreq", "1");
		BititleHashtable = titleWordFreq.countWords(words);
		
		// save the text frequency
		FastIterator iter = BititleHashtable.keys();
		String key;
		String firstWordID;
		String secondWordID;
		String BigramID;
		String freq;
		String result = "";
		while((key = (String) iter.next()) != null)
		{
			freq = (String) BititleHashtable.get(key);
			
			firstWordID = WordtoWordID.get(key.split(" ")[0]);
			secondWordID = WordtoWordID.get(key.split(" ")[1]);
			BigramID = firstWordID + " " + secondWordID;
			
			// add to BigramTitleWordIDtoPageID
			if (BiTitleWordIDtoPageID.get(BigramID) == null) {
				BiTitleWordIDtoPageID.add(BigramID, PageID+" "+freq+";");
			} else {
				BiTitleWordIDtoPageID.appendFreq(BigramID, PageID, freq);
			}
			
			// add to PageIDtoBiTitleWordID
			result += BigramID+" "+freq+";";
		}
		if (result.compareTo("") != 0)
			PageIDtoBiTitleWordID.add(PageID, result);
	}
	
	public static Vector<String> extractWordsBigram(String url) throws ParserException, IOException
	{
		// extract words in url and return them
		Vector<String> words = new Vector<String>();
		StringBean sb =  new StringBean();
		sb.setLinks(true);
		sb.setURL(url);
		
		// split with space characters
		String[] strings = sb.getStrings().split("\\s+");
		
		// stop and stem the words (only bigrams without stopwords)
		for (int i = 0; i < strings.length - 1; i++) {
			String firstWord = strings[i].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			String secondWord = strings[i+1].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (firstWord.compareTo("")!=0 && secondWord.compareTo("")!=0)
				if (!stopStem.isStopWord(firstWord) && !stopStem.isStopWord(secondWord))
					if (stopStem.stem(firstWord).compareTo("")!=0 && stopStem.stem(secondWord).compareTo("")!=0)
						words.add(stopStem.stem(firstWord) + " " + stopStem.stem(secondWord));
		}
		
		// remove title bigram in words bigram
		FastIterator iter = BititleHashtable.keys();
		String key;
		while((key = (String) iter.next()) != null) {
			words.remove(key);
		}
		
		return words;
	}
	
	public void storeWordsBigram(String PageID, String url) throws Exception
	{
		// get the text on the web page
		Vector<String> words = extractWordsBigram(url);
		
		// count the word frequency
		Database wordFreq = new Database("wordfreq", "1");
		HTree hashtable = wordFreq.countWords(words);
		
		// save the text frequency
		FastIterator iter = hashtable.keys();
		String key;
		String firstWordID;
		String secondWordID;
		String BigramID;
		String freq;
		String result = "";
		while((key = (String) iter.next()) != null)
		{
			freq = (String) hashtable.get(key);
			
			firstWordID = WordtoWordID.get(key.split(" ")[0]);
			secondWordID = WordtoWordID.get(key.split(" ")[1]);
			BigramID = firstWordID + " " + secondWordID;
			
			// add to BigramTitleWordIDtoPageID
			if (BiWordIDtoPageID.get(BigramID) == null) {
				BiWordIDtoPageID.add(BigramID, PageID+" "+freq+";");
			} else {
				BiWordIDtoPageID.appendFreq(BigramID, PageID, freq);
			}
			
			// add to PageIDtoBiTitleWordID
			result += BigramID+" "+freq+";";
		}
		if (result.compareTo("") != 0)
			PageIDtoBiWordID.add(PageID, result);
	}
	
	public void storeTitleTrigram(String PageID, String url) throws IOException
	{
		// get title
		String title = extractTitle(url);
		
		// count the word frequency
		String[] strings = title.split("\\s+");
		Vector<String> words = new Vector<String>();
		// stop and stem the words (only trigrams without stopwords)
		for (int i = 0; i < strings.length - 2; i++) {
			String firstWord = strings[i].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			String secondWord = strings[i+1].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			String thirdWord = strings[i+2].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (firstWord.compareTo("")!=0 && secondWord.compareTo("")!=0 && thirdWord.compareTo("")!=0)
				if (!stopStem.isStopWord(firstWord) && !stopStem.isStopWord(secondWord) && !stopStem.isStopWord(thirdWord))
					if (stopStem.stem(firstWord).compareTo("")!=0 && stopStem.stem(secondWord).compareTo("")!=0 && stopStem.stem(thirdWord).compareTo("")!=0)
						words.add(stopStem.stem(firstWord) + " " + stopStem.stem(secondWord) + " " + stopStem.stem(thirdWord));
		}
		Database titleWordFreq = new Database("titlewordfreq", "1");
		TrititleHashtable = titleWordFreq.countWords(words);
		
		// save the text frequency
		FastIterator iter = TrititleHashtable.keys();
		String key;
		String firstWordID;
		String secondWordID;
		String thirdWordID;
		String TrigramID;
		String freq;
		String result = "";
		while((key = (String) iter.next()) != null)
		{
			freq = (String) TrititleHashtable.get(key);
			
			firstWordID = WordtoWordID.get(key.split(" ")[0]);
			secondWordID = WordtoWordID.get(key.split(" ")[1]);
			thirdWordID = WordtoWordID.get(key.split(" ")[2]);
			TrigramID = firstWordID + " " + secondWordID + " " + thirdWordID;
			
			// add to TriTitleWordIDtoPageID
			if (TriTitleWordIDtoPageID.get(TrigramID) == null) {
				TriTitleWordIDtoPageID.add(TrigramID, PageID+" "+freq+";");
			} else {
				TriTitleWordIDtoPageID.appendFreq(TrigramID, PageID, freq);
			}
			
			// add to PageIDtoTriTitleWordID
			result += TrigramID+" "+freq+";";
		}
		if (result.compareTo("") != 0)
			PageIDtoTriTitleWordID.add(PageID, result);
	}
	
	public static Vector<String> extractWordsTrigram(String url) throws ParserException, IOException
	{
		// extract words in url and return them
		Vector<String> words = new Vector<String>();
		StringBean sb =  new StringBean();
		sb.setLinks(true);
		sb.setURL(url);
		
		// split with space characters
		String[] strings = sb.getStrings().split("\\s+");
		
		// stop and stem the words (only trigrams without stopwords)
		for (int i = 0; i < strings.length - 2; i++) {
			String firstWord = strings[i].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			String secondWord = strings[i+1].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			String thirdWord = strings[i+2].replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (firstWord.compareTo("")!=0 && secondWord.compareTo("")!=0 && thirdWord.compareTo("")!=0)
				if (!stopStem.isStopWord(firstWord) && !stopStem.isStopWord(secondWord) && !stopStem.isStopWord(thirdWord))
					if (stopStem.stem(firstWord).compareTo("")!=0 && stopStem.stem(secondWord).compareTo("")!=0 && stopStem.stem(thirdWord).compareTo("")!=0)
						words.add(stopStem.stem(firstWord) + " " + stopStem.stem(secondWord) + " " + stopStem.stem(thirdWord));
		}
		
		// remove title trigram in words trigram
		FastIterator iter = TrititleHashtable.keys();
		String key;
		while((key = (String) iter.next()) != null) {
			words.remove(key);
		}
		
		return words;
	}
	
	public void storeWordsTrigram(String PageID, String url) throws Exception
	{
		// get the text on the web page
		Vector<String> words = extractWordsTrigram(url);
		
		// count the word frequency
		Database wordFreq = new Database("wordfreq", "1");
		HTree hashtable = wordFreq.countWords(words);
		
		// save the text frequency
		FastIterator iter = hashtable.keys();
		String key;
		String firstWordID;
		String secondWordID;
		String thirdWordID;
		String TrigramID;
		String freq;
		String result = "";
		while((key = (String) iter.next()) != null)
		{
			freq = (String) hashtable.get(key);
			
			firstWordID = WordtoWordID.get(key.split(" ")[0]);
			secondWordID = WordtoWordID.get(key.split(" ")[1]);
			thirdWordID = WordtoWordID.get(key.split(" ")[2]);
			TrigramID = firstWordID + " " + secondWordID + " " + thirdWordID;
			
			// add to BigramTitleWordIDtoPageID
			if (TriWordIDtoPageID.get(TrigramID) == null) {
				TriWordIDtoPageID.add(TrigramID, PageID+" "+freq+";");
			} else {
				TriWordIDtoPageID.appendFreq(TrigramID, PageID, freq);
			}
			
			// add to PageIDtoBiTitleWordID
			result += TrigramID+" "+freq+";";
		}
		if (result.compareTo("") != 0)
			PageIDtoTriWordID.add(PageID, result);
	}
}