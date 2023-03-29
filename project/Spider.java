package project;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import org.htmlparser.beans.StringBean;
import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.HTMLLinkBean;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import org.jsoup.Jsoup;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Spider
{
	private static String url;
	
	private static Index PageIDtoURL;
	private static Index URLtoPageID;
	private static int PageIndex;
	
	private static Index PageIDtoTitle;
	private static Index PageIDtoTime;
	private static Index PageIDtoLength;
	
	private static Index WordIDtoWord;
	private static Index WordtoWordID;
	private static int WordIndex;
	
	private static Index ParenttoChild;
	private static Index ChildtoParent;
	
	private static Index PageIDtoWordID;
	private static Index WordIDtoPageID;
	
	private static Vector<String> pages_queue;
	private static Vector<String> visited_pages;
	
	Spider(String _url) throws IOException
	{
		url = _url;
		
		// create mapping table for PageID and URL
		PageIDtoURL = new Index("PageIDtoURL", "1");
		URLtoPageID = new Index("URLtoPageID", "1");
		PageIndex = PageIDtoURL.size();
		
		// create mapping table for PageID and Header
		PageIDtoTitle = new Index("PageIDtoTitle", "1");
		PageIDtoTime = new Index("PageIDtoTime", "1");
		PageIDtoLength = new Index("PageIDtoLength", "1");
		
		// create mapping table for WordID and Word
		WordIDtoWord = new Index("WordIDtoWord", "1");
		WordtoWordID = new Index("WordtoWordID", "1");
		WordIndex = WordIDtoWord.size();
		
		// create forward and backward index for Parent and Child
		ParenttoChild = new Index("ParenttoChild", "1");
		ChildtoParent = new Index("ChildtoParent", "1");
		
		// create forward and backward index for PageID and WordID
		PageIDtoWordID = new Index("PageIDtoWordID", "1");
		WordIDtoPageID = new Index("WordIDtoPageID", "1");
		
		// initialize pages_queue and visitied_pages
		// vector for storing next pages (breadth-first-search)
		pages_queue = new Vector<String>();
		visited_pages = new Vector<String>();
	}
	
	public static Date extractDate() throws IOException
	{
		URL urll = new URL(url);
		URLConnection connection = urll.openConnection();
		long time = connection.getLastModified();
		Date date;
		
		// if cannot get time, then return the time of visiting the web page
		if (time == 0) {
			date = new Date();
		} else {
			date = new Date(time);
		}
		return date;
	}
	
	public static String extractHTMLLength() throws Exception
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
	
	public static Vector<String> extractWords() throws ParserException
	{
		// extract words in url and return them
		Vector<String> words = new Vector<String>();
		StringBean sb =  new StringBean();
		sb.setLinks(true);
		sb.setURL(url);
		
		// split with space characters
		String[] strings = sb.getStrings().split("\\s+");
		
		// add the strings into vector of strings
		for (String string : strings) {
			words.add(string);
		}
		return words;
	}
	
	public static Vector<String> extractLinks() throws ParserException
	{
		// extract links in url and return them
		Vector<String> links = new Vector<String>();
		LinkBean lb = new LinkBean();
		lb.setURL(url);
		URL[] url = lb.getLinks();
		for (int i = 0; i < url.length; i++) {
			links.add(url[i].toString());
		}
		return links;
	}
	
	public static void storeTitle(String PageID) throws IOException
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
		// store the title to PageIDtoTitle
		PageIDtoTitle.add(PageID, title);
	}
	
	public static void storeWords(String PageID) throws Exception
	{
		// get the text on the web page
		Vector<String> words = extractWords();
		
		// print the size of the page
		URL urll = new URL(url);
		URLConnection connection = urll.openConnection();
		String context_size = connection.getHeaderField("Content-Length");
		// save all context-length, HTML Length and number of words extracted
		PageIDtoLength.add(PageID, context_size+";"+extractHTMLLength()+";"+words.size());
		
		// count the word frequency
		Index wordfreq = new Index("wordfreq", "1");
		HTree hashtable = wordfreq.countWords(words);
		// print the text frequency
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
				
				// add to WordIDtoPageID
				WordIDtoPageID.add(WordID, PageID+" "+freq+";");
			} else {
				// add to WordIDtoPageID
				WordIDtoPageID.appendFreq(WordID, PageID, freq);
			}
			
			// add to PageIDtoWordID
			if (count == 0)
				PageIDtoWordID.add(PageID, WordID+" "+freq+";");
			else
				PageIDtoWordID.appendFreq(PageID, WordID, freq);
			
			count++;
		}
	}
	
	public static void storeLinks(String PageID) throws ParserException, IOException
	{
		// get the child links
		Vector<String> links = extractLinks();
		String ChildPageURL;
		String ChildPageID;
		// print the links
		for(int j = 0; j < links.size(); j++) {
			// add all links to vector for next pages
			ChildPageURL = links.get(j);
			if (!pages_queue.contains(ChildPageURL))
				pages_queue.add(ChildPageURL);
			
			// put child page to PageIDtoURL tables
			ChildPageID = URLtoPageID.get(ChildPageURL);
			if (ChildPageID == null) {
				Date date = new Date(0);
				PageIDtoURL.add(Integer.toString(PageIndex), ChildPageURL);
				URLtoPageID.add(ChildPageURL, Integer.toString(PageIndex));
				PageIDtoTime.add(Integer.toString(PageIndex), ""+date);
				ChildPageID = Integer.toString(PageIndex);
				PageIndex++;
			}
			
			// add child string to ParenttoChild
			if (ParenttoChild.get(PageID) == null) {
				ParenttoChild.add(PageID, ChildPageID+";");
			} else {
				ParenttoChild.append(PageID, ChildPageID);
			}
			
			// add parent string to ChildtoParent
			if (ChildtoParent.get(ChildPageID) == null) {
				ChildtoParent.add(ChildPageID, PageID+";");
			} else {
				ChildtoParent.append(ChildPageID, PageID);
			}
		}
	}
	
	public static void print() throws IOException
	{
		FastIterator iter = PageIDtoTitle.getKeys();
		String PageID;
		while ((PageID = (String)iter.next()) != null) {
			// print Title
			System.out.println(PageIDtoTitle.get(PageID));
			
			// print URL
			System.out.println(PageIDtoURL.get(PageID));
			
			// print Last Modified Date
			System.out.print(PageIDtoTime.get(PageID));
			// print Size of Page
			String[] Length = PageIDtoLength.get(PageID).split(";");
			System.out.println(", "+Length[0]+" (Content-Length), "+Length[1]+" (HTML Length)");
			// print number of words
			System.out.println(Length[2]+" (Number of Words)");
			
			// print word with freq (up to 10)
			if (PageIDtoWordID.get(PageID) != null) {
				String[] wordIDs = PageIDtoWordID.get(PageID).split(";");
				for (int i = 0; i < 10; i++) {
					String[] wordID_freq = wordIDs[i].split(" ");
					String word = WordIDtoWord.get(wordID_freq[0]);
					System.out.print(word+" "+wordID_freq[1]+"; ");
				}
				System.out.println("");
			}
				
			// print child links (up to 10)
			if (ParenttoChild.get(PageID) != null) {
				String[] linkIDs = ParenttoChild.get(PageID).split(";");
				for (int i = 0; i < 10; i++) {
					System.out.println(PageIDtoURL.get(linkIDs[i]));
				}
			}
			
			// print the dividing line
			System.out.println("--------------------------------------------------");
		}
	}
	
	public static void saveDatabase() throws IOException
	{
		// print
		print();
		
		// print everything in db for checking
		System.out.println("===== PageID to URL =====")
				 
		// save all the tables and indexes
		PageIDtoURL.save();
		URLtoPageID.save();
		PageIDtoTitle.save();
		PageIDtoTime.save();
		PageIDtoLength.save();
		WordIDtoWord.save();
		WordtoWordID.save();
		ParenttoChild.save();
		ChildtoParent.save();
		PageIDtoWordID.save();
		WordIDtoPageID.save();
	}
	
	public void crawl(int num) throws Exception
	{
		if (url != null) {
			
			// initialize pages_queue and visitied_pages
			pages_queue = new Vector<String>();
			visited_pages = new Vector<String>();
			
			// crawl num number of pages and store into tables and indexes
			for (int i = 0; i < num; i++) {
				
				// for tracking process
				// System.out.println(i+"...");
				
				// if breadth first search has done
				if (url == null)
					continue;
				
				
				String PageID = URLtoPageID.get(url);
				// Case 1
				// if URL not exists in table,
				// store fetching page into mapping table of PageID and URL
				if (PageID == null) {
					PageIDtoURL.add(Integer.toString(PageIndex), url);
					URLtoPageID.add(url, Integer.toString(PageIndex));
					PageIDtoTime.add(Integer.toString(PageIndex), ""+extractDate());
					PageID = Integer.toString(PageIndex);
					PageIndex++;
				} else {
					DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
					Date date = (Date) format.parse(PageIDtoTime.get(PageID));
					// Case 2
					// if URL already exists in table, and the last modified date
					// is earlier than or same as the one in record, then ignore
					if (extractDate().compareTo(date) <= 0) {
						// get the links and then ignore
						Vector<String> links = extractLinks();
						// append the links to pages
						for(int j = 0; j < links.size(); j++)
							pages_queue.add(links.get(j));
						// i-- as this page is not fetched
						i--;
						// assign next page to url
						if (!pages_queue.isEmpty())
							// if visited this round, then next page
							while (visited_pages.contains(url = pages_queue.remove(0)))
								continue;
						else
							url = null;
						continue;
					}	
					// Case 3
					// if URL already exists in table, and the last modified date
					// is later than the one in record, then fetch and modify time
					PageIDtoTime.add(URLtoPageID.get(url), ""+extractDate());
				}
				
				// store the title
				storeTitle(PageID);
				
				// store the words from the page
				storeWords(PageID);
				
				// store the links from the page
				storeLinks(PageID);
				
				// add to visited pages
				visited_pages.add(url);
				
				// assign next page to url
				if (!pages_queue.isEmpty()) {
					// if visited this round, then next page
					while (visited_pages.contains(url = pages_queue.remove(0)))
						continue;
				}
				else url = null;
			}
			
			// save the databases
			saveDatabase();
			
		} else {
			System.out.println("Usage: java -cp combined.jar:. project.main [-links] url [-num] NumOfPages");
		}
	}
}

	
