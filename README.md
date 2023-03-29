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
