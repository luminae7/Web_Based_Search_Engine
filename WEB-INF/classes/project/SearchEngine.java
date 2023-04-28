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

public class SearchEngine
{
	private static String query;
	private static Vector<String> stopStemQuery;
	
	private static Vector<String> pages;
	
	private static Database PageIDtoURL;
	private static Database URLtoPageID;
	
	private static Database PageIDtoTitle;
	private static Database PageIDtoTime;
	private static Database PageIDtoLength;
	
	private static Database WordIDtoWord;
	private static Database WordtoWordID;
	
	private static Database ParenttoChild;
	private static Database ChildtoParent;
	
	public static Database PageIDtoTitleWordID;
	public static Database TitleWordIDtoPageID;
	
	public static Database PageIDtoWordID;
	public static Database WordIDtoPageID;
	public static Database PageIDtoTopFiveWordID;
	
	private static StopStem stopStem;
	
	public SearchEngine(String _query) throws IOException
	{
		
		// the query entered by the user
		query = _query;
		stopStemQuery = new Vector<String>();
		
		// the pages fit the query
		pages = new Vector<String>();
		
		// find mapping table for PageID and URL
		PageIDtoURL = new Database("PageIDtoURL", "1");
		URLtoPageID = new Database("URLtoPageID", "1");
		
		// find mapping table for PageID and Header
		PageIDtoTitle = new Database("PageIDtoTitle", "1");
		PageIDtoTime = new Database("PageIDtoTime", "1");
		PageIDtoLength = new Database("PageIDtoLength", "1");
		
		// find mapping table for WordID and Word
		WordIDtoWord = new Database("WordIDtoWord", "1");
		WordtoWordID = new Database("WordtoWordID", "1");
		
		// find forward and backward index for Parent and Child
		ParenttoChild = new Database("ParenttoChild", "1");
		ChildtoParent = new Database("ChildtoParent", "1");
		
		// find forward and backward index for PageID and Title's WordID
		PageIDtoTitleWordID = new Database("PageIDtoTitleWordID", "1");
		TitleWordIDtoPageID = new Database("TitleWordIDtoPageID", "1");
		
		// find forward and backward index for PageID and WordID
		PageIDtoWordID = new Database("PageIDtoWordID", "1");
		WordIDtoPageID = new Database("WordIDtoPageID", "1");
		// find forward index for PageID and Top Five WordID
		PageIDtoTopFiveWordID = new Database("PageIDtoTopFiveWordID", "1");
		
		stopStem = new StopStem("stopwords.txt");
		
	}
	
	private static void stopStem()
	{	
		// split with space characters
		String[] strings = query.split("\\s+");
		
		// stop and stem the string
		// add the strings into vector of strings
		for (String string : strings) {
			string = string.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (string.compareTo("")!=0)
				if (!stopStem.isStopWord(string))
					stopStemQuery.add(stopStem.stem(string));
		}
	}
	
	public void search() throws IOException
	{	
		stopStem();
		
		for (String word : stopStemQuery) {
			// get wordID of each word
			String wordID = WordtoWordID.get(word);
			if (wordID != null) {
				
				// get pages whose title contains this word
				if (TitleWordIDtoPageID.get(wordID).compareTo("") != 0) {
					String[] pageIDs_title = TitleWordIDtoPageID.get(wordID).split(";");
					for (int i = 0; i < pageIDs_title.length; i++) {
						String[] pages_freq = pageIDs_title[i].split(" ");
						if (!pages.contains(pages_freq[0]))
							pages.add(pages_freq[0]);
					}
				}
				
				// get pages that contain this word
				if (WordIDtoPageID.get(wordID).compareTo("") != 0) {
					String[] pageIDs = WordIDtoPageID.get(wordID).split(";");
					for (int i = 0; i < pageIDs.length; i++) {
						String[] pages_freq = pageIDs[i].split(" ");
						if (!pages.contains(pages_freq[0]))
							pages.add(pages_freq[0]);
					}
				}
			}
		}
	}
	
	public static String getStopStemQuery()
	{
		String result = "";
		for (String query : stopStemQuery)
			result += query + " ";
		return result;
	}
	
	public static String getPageID(int i)
	{
		return pages.elementAt(i);
	}
	
	public static Vector<String> print() throws IOException
	{	
		Vector<String> results = new Vector<String>();
		
		int num = 0;
		
		for (String PageID : pages) {
			
			if (num == 50)
				break;
			
			String result = "";
			
			String _url = PageIDtoURL.get(PageID);
			
			// print Title
			result += "=== " + "<b>" + "<a href="+_url+">" + PageIDtoTitle.get(PageID) + "</a>" + "</b>" + " ===" + "<BR><BR>";
			
			// print URL
			result += "<a href="+_url+">" + PageIDtoURL.get(PageID) + "</a>" + "<BR><BR>";
			
			// print Last Modified Date
			result += PageIDtoTime.get(PageID);
			// print Size of Page
			String[] Length = PageIDtoLength.get(PageID).split(";");
			result += ", "+Length[0]+" (Content-Length), "+Length[1]+" (HTML Length)" + "<BR>";
			// print number of words
			result += Length[2] + " (Number of Words)" + "<BR><BR>";
			
			// print word with top five freq (up to 5)
			if (PageIDtoTopFiveWordID.get(PageID) != null) {
				String[] wordIDs = PageIDtoTopFiveWordID.get(PageID).split(";");
				for (int i = 0; i < 5; i++) {
					String[] wordID_freq = wordIDs[i].split(" ");
					String word = WordIDtoWord.get(wordID_freq[0]);
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
			
			num++;
		}
		return results;
	}
}

	
