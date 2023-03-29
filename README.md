# COMP4321 Project - Web-Based Search Engine

Given a starting URL and number of pages to be indexed, the Spider will crawl the site into the local database using a breadth-first search.

First build the program by:
> javac -cp combined.jar project/Index.java project/Spider.java project/Tester.java

Then execute it by:
> java -cp combined.jar:. project.Tester -links [the staring URL] -num [the required number of pages]

You can also specify the output destination by:
> java -cp combined.jar:. project.Tester -links [the staring URL] -num [the required number of pages] > result.txt

The arguments links and num are optional. The intial value of the arguments are http://cse.ust.hk and 30.

If you want to first clear the database saved previously:
> rm \*.db \*.lg

## Others:

The specification of the JDBM Database Scheme of the Indexer can be found in the doc folder. **JDBM Database Scheme.pdf**

1. If the title cannot be extracted, “No Title” would be shown, dealing with the certification error.
2. If the last modified time cannot be extracted, the time would be the current time.
3. As most of the content-length cannot be extracted (null), the number of characters of the HTML string would be used also.
4. If the HTML length cannot be extracted also (null), the number of words extracted from the page can be considered.
5. If the page is visited this round (one execution of the program), it will not be visited again, no matter what the new last modified time is, for preventing the cyclic link problem.
6. Only pages fetched this round (this execution) will be shown by the program. Pages stored in the database but not updated this round will not be shown.
