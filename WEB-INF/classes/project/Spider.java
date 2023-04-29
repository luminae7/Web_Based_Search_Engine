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
import java.util.Collections;
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
	
	private static Database PageIDtoURL;
	private static Database URLtoPageID;
	private static int PageIndex;
	
	private static Database PageIDtoTitle;
	private static Database PageIDtoTime;
	
	private static Database ParenttoChild;
	private static Database ChildtoParent;
	
	private static Vector<String> pages_queue;
	private static Vector<String> visited_pages;
	
	private static Indexer indexer;
	
	private static Database PageIDtoTFxIDF;
	private static double[][] tfxidf;
	
	public Spider(String _url) throws IOException
	{
		url = _url;
		
		// create mapping table for PageID and URL
		PageIDtoURL = new Database("PageIDtoURL", "1");
		URLtoPageID = new Database("URLtoPageID", "1");
		PageIndex = PageIDtoURL.size();
		
		// create mapping table for PageID and Header
		PageIDtoTitle = new Database("PageIDtoTitle", "1");
		PageIDtoTime = new Database("PageIDtoTime", "1");
		
		// create forward and backward index for Parent and Child
		ParenttoChild = new Database("ParenttoChild", "1");
		ChildtoParent = new Database("ChildtoParent", "1");
		
		// initialize pages_queue and visitied_pages
		// vector for storing next pages (breadth-first-search)
		pages_queue = new Vector<String>();
		visited_pages = new Vector<String>();
		
		// create indexer for storing words in page and title
		indexer = new Indexer();
		
		// create forward index for PageID and TFxIDF
		PageIDtoTFxIDF = new Database("PageIDtoTFxIDF", "1");
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
		
		// store the title words
		indexer.storeTitle(PageID, title);
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
	
	public static void tfxidf() throws IOException
	{	
		tfxidf = new double[PageIDtoTitle.size()][indexer.WordIDtoWord.size()];
		FastIterator iter = indexer.WordIDtoWord.getKeys();
		String WordID;
		
		// calculate tfxidf
		while ((WordID = (String)iter.next()) != null) {
			
			if (WordID != null) {
				
				int wordID = Integer.valueOf(WordID);
				
				// tf idf
				// get pages that contain this word
				if (indexer.WordIDtoPageID.get(WordID).compareTo("") != 0) {
					
					String[] pageIDs = indexer.WordIDtoPageID.get(WordID).split(";");
					
					// tf idf
					double df = pageIDs.length;
					double N = PageIDtoTitle.size();
					for (int i = 0; i < pageIDs.length; i++) {
						String[] pages_freq = pageIDs[i].split(" ");
						int pageID = Integer.valueOf(pages_freq[0]);
						int freq = Integer.valueOf(pages_freq[1]);
						// tf
						tfxidf[pageID][wordID] = freq;
						// idf
						tfxidf[pageID][wordID] *= Math.log(N / df) / Math.log(2);
						// max(tf)
						String[] words = indexer.PageIDtoTopFiveWordID.get(pages_freq[0]).split(";");
						String maxFreq = words[0].split(" ")[1];
						tfxidf[pageID][wordID] /= Integer.valueOf(maxFreq);
					}
				}
			}
		}
		
		iter = PageIDtoTitle.getKeys();
		String PageID;
		
		// store into database
		while ((PageID = (String)iter.next()) != null) {
			
			int pageID = Integer.valueOf(PageID);
			String result = "";
			
			for (double val : tfxidf[pageID]) {
				result += val + ";";
			}
			
			PageIDtoTFxIDF.add(PageID, result);
		}
	}
	
	public static Vector<String> print() throws IOException
	{
		Vector<String> results = new Vector<String>();
		FastIterator iter = PageIDtoTitle.getKeys();
		String PageID;
		while ((PageID = (String)iter.next()) != null) {
			
			String result = "";
			
			String _url = PageIDtoURL.get(PageID);
			
			// print Title
			result += "=== " + "<b>" + "<a href="+_url+">" + PageIDtoTitle.get(PageID) + "</a>" + "</b>" + " ===" + "<BR><BR>";
			
			// print Title words
			// if (PageIDtoTitleWordID.get(PageID) != null) {
			// 	String[] wordIDs = PageIDtoTitleWordID.get(PageID).split(";");
			//	for (int i = 0; i < wordIDs.length; i++) {
			//		String[] wordID_freq = wordIDs[i].split(" ");
			//		String word = WordIDtoWord.get(wordID_freq[0]);
			//		result += word+" "+wordID_freq[1]+"; ";
			//	}
			//	result += "<BR>";
			// }
			
			// print URL
			result += "<a href="+_url+">" + PageIDtoURL.get(PageID) + "</a>" + "<BR><BR>";
			
			// print Last Modified Date
			result += PageIDtoTime.get(PageID);
			// print Size of Page
			String[] Length = indexer.PageIDtoLength.get(PageID).split(";");
			result += ", "+Length[0]+" (Content-Length), "+Length[1]+" (HTML Length)" + "<BR>";
			// print number of words
			result += Length[2] + " (Number of Words)" + "<BR><BR>";
			
			// print word with freq (up to 10)
			// if (indexer.PageIDtoWordID.get(PageID) != null) {
			// 	 String[] wordIDs = indexer.PageIDtoWordID.get(PageID).split(";");
			//	 for (int i = 0; i < 10; i++) {
			//		 String[] wordID_freq = wordIDs[i].split(" ");
			//		 String word = indexer.WordIDtoWord.get(wordID_freq[0]);
			//		 System.out.print(word+" "+wordID_freq[1]+"; ");
			//	 }
			//	 System.out.println("");
			// }
			
			// print word with top five freq (up to 5)
			if (indexer.PageIDtoTopFiveWordID.get(PageID) != null) {
				String[] wordIDs = indexer.PageIDtoTopFiveWordID.get(PageID).split(";");
				for (int i = 0; i < 5; i++) {
					String[] wordID_freq = wordIDs[i].split(" ");
					String word = indexer.WordIDtoWord.get(wordID_freq[0]);
					result += word+" "+wordID_freq[1]+"<BR>";
				}
				result += "<BR>";
			}
			
			// print parent links (up to 10)
			result += "Parent Links:" + "<BR>";
			if (ChildtoParent.get(PageID) != null) {
				String[] linkIDs = ChildtoParent.get(PageID).split(";");
				for (int i = 0; i < Math.min(10, linkIDs.length); i++) {
					String url = PageIDtoURL.get(linkIDs[i]);
					result += i+1+": "+"<a href="+url+">" + url + "</a>" + "<BR>";
				}
			}
			result += "<BR>";
				
			// print child links (up to 10)
			result += "Child Links:" + "<BR>";
			if (ParenttoChild.get(PageID) != null) {
				String[] linkIDs = ParenttoChild.get(PageID).split(";");
				for (int i = 0; i < Math.min(10, linkIDs.length); i++) {
					String url = PageIDtoURL.get(linkIDs[i]);
					result += i+1+": "+"<a href="+url+">" + url + "</a>" + "<BR>";
				}
			}
			result += "<BR>";
			
			results.add(result);
		}
		return results;
	}
	
	public static Vector<String> printWords() throws IOException
	{
		Vector<String> results = new Vector<String>();
		FastIterator iter = indexer.WordtoWordID.getKeys();
		String Word;
		while ((Word = (String)iter.next()) != null) {
			results.add(Word);
		}
		Collections.sort(results);
		return results;
	}
	
	public static void saveDatabase() throws IOException
	{		 
		// save all the tables and indexes
		PageIDtoURL.save();
		URLtoPageID.save();
		PageIDtoTitle.save();
		PageIDtoTime.save();
		indexer.PageIDtoLength.save();
		indexer.WordIDtoWord.save();
		indexer.WordtoWordID.save();
		ParenttoChild.save();
		ChildtoParent.save();
		indexer.PageIDtoWordID.save();
		indexer.WordIDtoPageID.save();
		// new
		indexer.PageIDtoTopFiveWordID.save();
		indexer.PageIDtoTitleWordID.save();
		indexer.TitleWordIDtoPageID.save();
		PageIDtoTFxIDF.save();
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
					break;
				
				
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
							while (visited_pages.contains(url = pages_queue.remove(0))) {
								if (pages_queue.isEmpty()) {
									url = null;
									break;
								}
								continue;
							}
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
				indexer.storeWords(PageID, url);
				
				// store the links from the page
				storeLinks(PageID);
				
				// add to visited pages
				visited_pages.add(url);
				
				// assign next page to url
				if (!pages_queue.isEmpty()) {
					// if visited this round, then next page
					while (visited_pages.contains(url = pages_queue.remove(0))) {
						if (pages_queue.isEmpty()) {
							url = null;
							break;
						}
						continue;
					}
				}
				else url = null;
			}
		} else {
			System.out.println("Usage: java -cp combined.jar:. project.main [-links] url [-num] NumOfPages");
		}
	}
}

	
