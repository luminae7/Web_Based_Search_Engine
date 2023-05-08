# COMP4321 Project - Web-Based Search Engine

Given a starting URL and number of pages to be indexed, the Spider will crawl the site into the local database using a breadth-first search. You can then search by submitting the query on the web interface.

Download the files on Github and unzip it

Add environment var:

**CATALINA_HOME** = "your tomcat path (e.g. /Users/ABC/apache-tomcat-9.0.74)"
**JAVA_HOME** = "your jdk path (e.g. /Users/ABC/jdk-20.0.1.jdk/Contents/Home)"

Go to $CATALINA_HOME/webapps and create a folder "Search"

Put the files under $CATALINA_HOME/webapps/Search

Then build the program by:
> javac -cp combined.jar WEB-INF/classes/project/*.java

Then start it by:
> $CATALINA_HOME/bin/startup.sh

Open in your browser: ***http://localhost:8080/Search/Search.html*** to test the program

Shut it down by:
> $CATALINA_HOME/bin/shutdown.sh

The inputs link and number of pages to fetch are optional. The initial values of the inputs are **https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm** and **300** (if you enter nothing in the Input Number of Pages to Fetch).

With -num 0, you can print the current data (fetched pages) stored in database. Or you can press the button "Database".

If you want to first clear the database saved previously, press the button "Delete Database"

## Extra Features:

1. Get Similar Pages: equivalent to search pages with the top 5 most frequent keywords from that page
2. List of Stemmed Keywords (Dictionary): you can browse through and select keywords to search
3. Query Search History: you can browse through and select queries to search
4. Delete Database and Delete History Button: you can delete database and past search history by one click
5. Sensitive Words: if you add words in sensitivewords.txt, and if your query submitted contains any of the sensitive word, you will get nothing, but a reminder of words that violates the regulation

## Others:

The documentation can be found in the doc folder. **(Project Document.pdf)**

1. If the title cannot be extracted, “No Title” would be shown, dealing with the certification error.
2. If the last modified time cannot be extracted, the time would be the current time.
3. As most of the content-length cannot be extracted (null), the number of characters of the HTML string would be used also.
4. If the HTML length cannot be extracted also (null), the number of words extracted from the page can be considered.
5. If the page is visited this round (one execution of the program), it will not be visited again, no matter what the new last modified time is, preventing the cyclic link problem.
