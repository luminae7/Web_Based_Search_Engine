package project;

public class main
{	
	public static void main (String[] args)
	{
		try
		{	
			// initial value of starting url and required number of pages
			String url = "http://www.cse.ust.hk";
			int num = 30;
			
			// argument parser
			for (int i = 0; i < args.length; i++) {
				if (args[i].equalsIgnoreCase ("-links"))
	                url = args[i+1];
				if (args[i].equalsIgnoreCase ("-num"))
					num = Integer.valueOf(args[i+1]);
			}
			
			// use spider to crawl
			Spider spider = new Spider(url);
			spider.crawl(num);
			
		}
		catch (Exception ex)
        {
			System.err.println(ex.toString());
		}
	}
}

	
