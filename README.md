# COMP4321 Project - Web-Based Search Engine

Given a starting URL and number of pages to be indexed, the Spider will crawl the site into the local database using a breadth-first search.

First Add environment var:

**CATALINA HOME** = "your tomcat path (e.g. C:\apache-tomcat-9.0.70)"

**JAVA HOME** = "your jdk path (e.g. C:\Program Files\Java\jdk-19)"

Go to %CATALINA_HOME%\webapps and create a folder "Search"

Put the files under %CATALINA_HOME%\webapps\Search

Then build the program by:
> javac -cp combined.jar WEB-INF/classes/project/*.java

Then start it by:
> %CATALINA_HOME%\bin\startup.bat

Open in your browser: ***http://localhost:8080/Search/Search.html*** to test your program

Shut it down by:
> %CATALINA_HOME%\bin\shutdown.bat

The inputs link and number of pages to fetch are optional. The initial values of the inputs are **https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm** and **300** (if you enter nothing in the Input Number of Pages to Fetch).

With -num 0, you can print the current data (fetched pages) stored in database. Or you can press the button "Database".

If you want to first clear the database saved previously, press the button "Delete Database"

The button "Similar Pages" is equivalent to search pages with the top 5 most frequent keywords from that page.

## Others:

The specification of the JDBM Database Scheme of the Indexer can be found in the doc folder. **(JDBM Database Scheme.pdf)**

1. If the title cannot be extracted, “No Title” would be shown, dealing with the certification error.
2. If the last modified time cannot be extracted, the time would be the current time.
3. As most of the content-length cannot be extracted (null), the number of characters of the HTML string would be used also.
4. If the HTML length cannot be extracted also (null), the number of words extracted from the page can be considered.
5. If the page is visited this round (one execution of the program), it will not be visited again, no matter what the new last modified time is, preventing the cyclic link problem.
