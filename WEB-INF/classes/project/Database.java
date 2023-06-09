package project;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;

public class Database
{
	private RecordManager recman;
	private HTree hashtable;
	public String[] TopFive;
	public int[] TopFiveVal;

	public Database(String recordmanager, String objectname) throws IOException
	{
		System.out.println(1);
		recordmanager = "database/" + recordmanager;
		recman = RecordManagerFactory.createRecordManager(recordmanager);
		long recid = recman.getNamedObject(objectname);
		
		if (recid != 0)
			hashtable = HTree.load(recman, recid);
		else
		{
			hashtable = HTree.createInstance(recman);
			recman.setNamedObject(objectname, hashtable.getRecid() );
		}
		
		TopFive = new String[5];
		TopFiveVal = new int[5];
		for (int i = 0; i < 5; i++) {
			TopFive[i] = "";
			TopFiveVal[i] = 0;
		}
	}
	
	public String get(String word) throws IOException
	{
		// get the value given the key
		return (String) hashtable.get(word);
	}
	
	public FastIterator getKeys() throws IOException
	{
		// return all the keys in the hashtable
		return hashtable.keys();
	}

	public void add(String key, String value) throws IOException
	{
		// Add an entry for the key "word" into hashtable
		hashtable.put(key, value);
	}
	
	public void del(String key) throws IOException
	{
		// delete the word and its list from the hashtable
		hashtable.remove(key);

	}
	
	public void append(String key, String value) throws IOException
	{
		// the format would be
		// (key : value;value;value;...)
		// need to find whether it contains the same value already
		// if yes, ignore it
		// if no, append it at the end
		String[] texts = ((String) hashtable.get(key)).split(";");
		
		for (String text : texts) {
			// if same word then ignore
			if (text.compareTo(value)==0)
				return;
		}
		hashtable.put(key, hashtable.get(key)+value+";");
	}
	
	public void appendFreq(String key, String word, String value) throws IOException
	{
		// the format would be
		// (key : word value; word value; word value;...)
		// need to find the one with the same word
		// delete it and append the new value at the back
		String[] texts = ((String) hashtable.get(key)).split(";");
		String result = "";
		
		String text_word;
		for (String text : texts) {
			text_word = text.split(" ")[0];
			if (text_word.compareTo("")!=0) {
				// if same word then not add to result
				if (text_word.compareTo(word)==0)
					continue;
				result += text + ";";
			}
		}
		result += word + " " + value + ";";
		
		hashtable.put(key, result);
	}
	
	public int size() throws IOException
	{
		// return the size of the hashtable
		int count = 0;
		FastIterator iter = hashtable.keys();
		while (iter.next() != null)
			count++;
		return count;
	}
	
	public void print() throws IOException
	{
		// Print all the data in the hashtable
		FastIterator iter = hashtable.keys();
		String key;
		while ((key = (String)iter.next())!=null)
		{
			System.out.println(key + " : " + hashtable.get(key));
		}

	}	
	
	public void save() throws IOException
	{
		// save with commit
		recman.commit();
		recman.close();
	}

	public HTree countWords(Vector<String> words) throws IOException
	{
		// Create hashtable for those words with frequency
		// Add an "freq" entry for the key "word" into hashtable
		String prev = null;
		for (String word : words) {
			
			prev = (String) hashtable.get(word);
			
			if (prev != null) {
				int value = Integer.valueOf(prev);
				hashtable.put(word, Integer.toString(value+1));
				updateTopFive(word, value+1);
			}
			else {
				hashtable.put(word, "1");
				updateTopFive(word, 1);
			}
		}
		return hashtable;
	}
	
	public void updateTopFive(String word, int freq)
	{
		// if word already in top five
		// update and sort
		for (int i = 0; i < 5; i++) {
			if (word.compareTo(TopFive[i]) == 0) {
				TopFiveVal[i] = freq;
				// swap to sort the array
				for (int j = i; j > 0; j--) {
					if (TopFiveVal[j] > TopFiveVal[j-1]) {
						String tmpTopFive = TopFive[j-1];
						TopFive[j-1] = TopFive[j];
						TopFive[j] = tmpTopFive;
						int tmpTopFiveVal = TopFiveVal[j-1];
						TopFiveVal[j-1] = TopFiveVal[j];
						TopFiveVal[j] = tmpTopFiveVal;
					}
				}
				return;
			}
		}
		// if word not in top five
		// 
		for (int i = 0; i < 5; i++) {
			if (freq > TopFiveVal[i]) {
				TopFive[i] = word;
				TopFiveVal[i] = freq;
				break;
			}
		}
	}
}
