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
import java.net.HttpURLConnection;
import org.jsoup.Jsoup;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Spider
{
	private static String url;
	
	Spider(String _url)
	{
		url = _url;
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
	
	public static void printHeader() throws IOException
	{
		// print the title
		String title;
		try {
			title = Jsoup.connect(url).get().title();
		}
		catch (Exception e) {
			title = "No Title";
		}
		System.out.println(title);
		
		// print the url
		System.out.println(url);
		
		// print the last modified date
		Date date = extractDate();
		System.out.print(date);
	}
	
	public static void crawl(int num) throws Exception
	{
		if (url != null) {
			
			// vector for storing next pages (breadth-first-search)
			Vector<String> pages = new Vector<String>();
			Vector<String> visited_pages = new Vector<String>();
			
			// create mapping table for PageID and URL
			Index PageIDtoURL = new Index("PageIDtoURL", "1");
			Index URLtoPageID = new Index("URLtoPageID", "1");
			int PageIndex = PageIDtoURL.size();
			
			// create mapping table for PageID and LastModifiedTime
			Index PageIDtoTime = new Index("PageIDtoTime", "1");
			
			// create mapping table for WordID and Word
			Index WordIDtoWord = new Index("WordIDtoWord", "1");
			Index WordtoWordID = new Index("WordtoWordID", "1");
			int WordIndex = WordIDtoWord.size();
			
			// create forward index for Parent and Child
			Index ParenttoChild = new Index("ParenttoChild", "1");
			
			// create forward and backward index for PageID and WordID
			Index PageIDtoWordID = new Index("PageIDtoWordID", "1");
			Index WordIDtoPageID = new Index("WordIDtoPageID", "1");
			
			// crawl num number of pages and store into tables and indexes
			for (int i = 0; i < num; i++) {
				
				// if breadth first search has done
				if (url == null)
					continue;
				
				
				String PageID = URLtoPageID.get(url);
				// Case 1
				// if URL not exists in table,
				// store fetched page into mapping table of PageID and URL
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
							pages.add(links.get(j));
						// i-- as this page is not fetched
						i--;
						// assign next page to url
						if (!pages.isEmpty())
							// if visited this round, then next page
							while (visited_pages.contains(url = pages.remove(0)))
								continue;
						else
							url = null;
						continue;
					}	
					// Case 3
					// if URL already exists in table, and the last modified date
					// is later than the one in record, then extract and modify time
					PageIDtoTime.add(URLtoPageID.get(url), ""+extractDate());
				}
				
				
				// print the headers
				// (page title, URL, Last modification date)
				printHeader();
				
				
				// get the text on the web page
				Vector<String> words = extractWords();
				// print the size of the page
				System.out.println(", "+words.size());
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
					
					
					if (count < 10)
						System.out.print(key + " " + freq + "; ");
					
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
						WordIDtoPageID.append(WordID, PageID, freq);
					}
					
					// add to PageIDtoWordID
					if (count == 0)
						PageIDtoWordID.add(PageID, WordID+" "+freq+";");
					else
						PageIDtoWordID.append(PageID, WordID, freq);
					
					count++;
				}
				System.out.println();
				
				
				// get the child links
				Vector<String> links = extractLinks();
				String ChildPageURL;
				String ChildPageID;
				String Child = "";
				// print the links
				for(int j = 0; j < links.size(); j++) {
					// add all links to vector for next pages
					ChildPageURL = links.get(j);
					pages.add(ChildPageURL);
					// print the first 10
					if (j < 10)
						System.out.println(ChildPageURL);
					
					// construct child string
					ChildPageID = URLtoPageID.get(ChildPageURL);
					if (ChildPageID == null) {
						Date date = new Date(0);
						PageIDtoURL.add(Integer.toString(PageIndex), ChildPageURL);
						URLtoPageID.add(ChildPageURL, Integer.toString(PageIndex));
						PageIDtoTime.add(Integer.toString(PageIndex), ""+date);
						ChildPageID = Integer.toString(PageIndex);
						PageIndex++;
					}
					Child += ChildPageID + ";";
				}
				// add child string to ParenttoChild
				ParenttoChild.add(PageID, Child);
				
				
				// print the dividing line
				System.out.println("--------------------------------------------------");
				
				
				// add to visited pages
				visited_pages.add(url);
				
				
				// assign next page to url
				if (!pages.isEmpty()) {
					// if visited this round, then next page
					while (visited_pages.contains(url = pages.remove(0)))
						continue;
				}
				else url = null;
			}
			
			// save all the tables and indexes
			PageIDtoURL.save();
			URLtoPageID.save();
			PageIDtoTime.save();
			WordIDtoWord.save();
			WordtoWordID.save();
			ParenttoChild.save();
			PageIDtoWordID.save();
			WordIDtoPageID.save();
			
			// print
			// System.out.println("===== PageID to URL =====");
			// PageIDtoURL.print();
			// System.out.println("===== URL to PageID =====");
			// URLtoPageID.print();
			// System.out.println("===== PageID to Time =====");
			// PageIDtoTime.print();
			// System.out.println("===== WordID to Word =====");
			// WordIDtoWord.print();
			// System.out.println("===== Word to WordID =====");
			// WordtoWordID.print();
			// System.out.println("===== Parent to Child =====");
			// ParenttoChild.print();
			// System.out.println("===== PageID to WordID =====");
			// PageIDtoWordID.print();
			// System.out.println("===== WordID to PageID =====");
			// WordIDtoPageID.print();
			
		} else {
			System.out.println("Usage: java -cp combined.jar:. project.main [-links] url [-num] NumOfPages");
		}
	}
}

	
