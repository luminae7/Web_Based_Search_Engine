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
import java.util.Arrays;
import java.util.Comparator;
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
	private static Vector<String> Phrases;
	private static Vector<String> stopStemQuery;
	
	private static double[][] fiftyPagesVal;
	private static Vector<String> fiftyPages;
	private static Vector<Double> fiftyVal;
	
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
	
	public Database PageIDtoBiTitleWordID;
	public Database BiTitleWordIDtoPageID;
	public Database PageIDtoTriTitleWordID;
	public Database TriTitleWordIDtoPageID;
	
	public Database PageIDtoBiWordID;
	public Database BiWordIDtoPageID;
	public Database PageIDtoTriWordID;
	public Database TriWordIDtoPageID;
	
	private static Database PageIDtoTFxIDF;
	
	private static StopStem stopStem;
	private static Vector<String> sensitiveWords;
	
	public SearchEngine(String _query) throws IOException
	{
		
		// the query entered by the user
		query = _query;
		Phrases = new Vector<String>();
		stopStemQuery = new Vector<String>();
		
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
		
		// find forward index for PageID and TFxIDF
		PageIDtoTFxIDF = new Database("PageIDtoTFxIDF", "1");
		
		stopStem = new StopStem("stopwords.txt", "sensitivewords.txt");
		sensitiveWords = new Vector<String>();
		
		// top 50 pages and val
		fiftyPagesVal = new double[PageIDtoTitle.size()][2];
		for (int i = 0; i < PageIDtoTitle.size(); i++) {
			fiftyPagesVal[i][0] = i;
		}
		fiftyPages = new Vector<String>();
		fiftyVal = new Vector<Double>();
	}
	
	public static void phrase()
	{	
		// split with " characters
		String[] strings = query.split("\"");
		
		// stop and stem the string
		// add the strings into phrase
		int count = 0;
		for (String string : strings) {
			count++;
			
			// outside ""
			if (count % 2 == 1)
				continue;
			
			// inside ""
			String[] phrases = string.split("\\s+");
			String wholePhrase = "";
			for (String phrase : phrases) {
				phrase = phrase.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
				if (phrase.compareTo("")!=0)
					if (!stopStem.isStopWord(phrase))
						wholePhrase += stopStem.stem(phrase) + " ";
			}
			wholePhrase = wholePhrase.strip();
			if (!Phrases.contains(wholePhrase))
				Phrases.add(wholePhrase);
		}
	}
	
	public static void stopStem()
	{	
		// split with space characters
		String[] strings = query.split("\\s+");
		
		// stop and stem the string
		// add the strings into vector of strings
		for (String string : strings) {
			string = string.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase();
			if (string.compareTo("")!=0)
				if (!stopStem.isStopWord(string) &&
					!stopStemQuery.contains(stopStem.stem(string)))
					stopStemQuery.add(stopStem.stem(string));
		}
	}
	
	public void search() throws IOException
	{	
		// scores in query
		for (String word : stopStemQuery) {
			
			// get wordID of each word
			String wordID = WordtoWordID.get(word);
			
			if (wordID != null) {
				
				// get pages whose title contains this word
				if (TitleWordIDtoPageID.get(wordID).compareTo("") != 0) {
					String[] pageIDs_title = TitleWordIDtoPageID.get(wordID).split(";");
					double df = pageIDs_title.length;
					double N = PageIDtoTitle.size();
					for (int i = 0; i < pageIDs_title.length; i++) {
						String[] pages_freq = pageIDs_title[i].split(" ");
							
						fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] += 1 * Math.log(N / df) / Math.log(2);
					}
				}
				
				// get pages that contains this word
				if (WordIDtoPageID.get(wordID).compareTo("") != 0) {
					String[] pageIDs = WordIDtoPageID.get(wordID).split(";");
					for (int i = 0; i < pageIDs.length; i++) {
						String[] pages_freq = pageIDs[i].split(" ");
						String[] page_scores = PageIDtoTFxIDF.get(pages_freq[0]).split(";");
						double page_score = Double.valueOf(page_scores[Integer.valueOf(wordID)]);
							
						fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] += page_score;
					}
				}
			}
		}
		
		// scores in phrases
		for (String phrase : Phrases) {
			
			String[] words = phrase.trim().split("\\s+");
			
			// phrase contains 1 word
			if (words.length == 1) {
			
				// get wordID of each word
				String wordID = WordtoWordID.get(phrase);
			
				if (wordID != null) {
					
					// get pages whose title contains this word
					if (TitleWordIDtoPageID.get(wordID).compareTo("") != 0) {
						String[] pageIDs_title = TitleWordIDtoPageID.get(wordID).split(";");
						double df = pageIDs_title.length;
						double N = PageIDtoTitle.size();
						for (int i = 0; i < pageIDs_title.length; i++) {
							String[] pages_freq = pageIDs_title[i].split(" ");
								
							fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] += 2 * 1 * Math.log(N / df) / Math.log(2);
						}
					}
					
					// get pages that contains this word
					if (WordIDtoPageID.get(wordID).compareTo("") != 0) {
						String[] pageIDs = WordIDtoPageID.get(wordID).split(";");
						for (int i = 0; i < pageIDs.length; i++) {
							String[] pages_freq = pageIDs[i].split(" ");
							String[] page_scores = PageIDtoTFxIDF.get(pages_freq[0]).split(";");
							double page_score = Double.valueOf(page_scores[Integer.valueOf(wordID)]);
								
							fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] += 2 * page_score;
						}
					}
				}
				
			// phrase contains 2 words
			} else if (words.length == 2) {
				
				// get wordID of each word
				String firstwordID = WordtoWordID.get(words[0]);
				String secondwordID = WordtoWordID.get(words[1]);
			
				if (firstwordID != null && secondwordID != null) {
					
					String BigramID = firstwordID + " " + secondwordID;
					
					// get pages whose title contains this word
					if (BiTitleWordIDtoPageID.get(BigramID) != null) {
						String[] pageIDs_title = BiTitleWordIDtoPageID.get(BigramID).split(";");
						double df = pageIDs_title.length;
						double N = PageIDtoTitle.size();
						for (int i = 0; i < pageIDs_title.length; i++) {
							String[] pages_freq = pageIDs_title[i].split(" ");
								
							fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] += 2 * 1 * Math.log(N / df) / Math.log(2);
						}
					}
					
					// get pages that contains this word
					if (BiWordIDtoPageID.get(BigramID) != null) {
						String[] pageIDs = BiWordIDtoPageID.get(BigramID).split(";");
						double df = pageIDs.length;
						double N = PageIDtoTitle.size();
						double max = 0;
						for (int i = 0; i < pageIDs.length; i++) {
							String[] pages_freq = pageIDs[i].split(" ");
							if (max < Double.valueOf(pages_freq[1])) {
								max = Double.valueOf(pages_freq[1]);
							}
						}
						for (int i = 0; i < pageIDs.length; i++) {
							String[] pages_freq = pageIDs[i].split(" ");
								
							fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] +=
									2 * Double.valueOf(pages_freq[1]) / max * Math.log(N / df) / Math.log(2);
						}
					}
				}
				
			// phrase contains 3 words
			} else if (words.length == 3) {
				
				// get wordID of each word
				String firstwordID = WordtoWordID.get(words[0]);
				String secondwordID = WordtoWordID.get(words[1]);
				String thirdwordID = WordtoWordID.get(words[2]);
			
				if (firstwordID != null && secondwordID != null && thirdwordID != null) {
					
					String TrigramID = firstwordID + " " + secondwordID + " " + thirdwordID;
					
					// get pages whose title contains this word
					if (TriTitleWordIDtoPageID.get(TrigramID) != null) {
						String[] pageIDs_title = TriTitleWordIDtoPageID.get(TrigramID).split(";");
						double df = pageIDs_title.length;
						double N = PageIDtoTitle.size();
						for (int i = 0; i < pageIDs_title.length; i++) {
							String[] pages_freq = pageIDs_title[i].split(" ");
								
							fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] += 2 * 1 * Math.log(N / df) / Math.log(2);
						}
					}
					
					// get pages that contains this word
					if (TriWordIDtoPageID.get(TrigramID) != null) {
						String[] pageIDs = TriWordIDtoPageID.get(TrigramID).split(";");
						double df = pageIDs.length;
						double N = PageIDtoTitle.size();
						double max = 0;
						for (int i = 0; i < pageIDs.length; i++) {
							String[] pages_freq = pageIDs[i].split(" ");
							if (max < Double.valueOf(pages_freq[1])) {
								max = Double.valueOf(pages_freq[1]);
							}
						}
						for (int i = 0; i < pageIDs.length; i++) {
							String[] pages_freq = pageIDs[i].split(" ");
								
							fiftyPagesVal[Integer.valueOf(pages_freq[0])][1] +=
									2 * Double.valueOf(pages_freq[1]) / max * Math.log(N / df) / Math.log(2);
						}
					}
				}
			}
		}
		
		// find and normalize top 50 score to [0, 100]
		Arrays.sort(fiftyPagesVal, (a, b) -> Double.compare(b[1], a[1]));
		
		for (int i = 0; i < Math.min(50, PageIDtoTitle.size()); i++) {
			fiftyPages.add(String.valueOf((int)fiftyPagesVal[i][0]));
			fiftyVal.add(fiftyPagesVal[i][1]);
		}
	}
	
	public static String getPhrases()
	{
		String result = "";
		for (String string : Phrases)
			result += string + ", ";
		return result;
	}
	
	public static String getStopStemQuery()
	{
		String result = "";
		for (String string : stopStemQuery)
			result += string + " ";
		return result;
	}
	
	public static Vector<String> getSensitiveWords()
	{
		for (String string : stopStemQuery) {
			if (stopStem.isSensitiveWord(string)) {
				sensitiveWords.add(string);
			}
		}
		return sensitiveWords;
	}
	
	public static String getPageID(int i)
	{
		return fiftyPages.elementAt(i);
	}
	
	public static Vector<Double> printScores()
	{
		return fiftyVal;
	}
	
	public static Vector<String> printResults() throws IOException
	{	
		Vector<String> results = new Vector<String>();
		
		int num = 0;
		
		for (String PageID : fiftyPages) {
			
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

	
