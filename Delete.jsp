<%@ page import="java.io.File"%>

<%
File directory = new File("database");
File[] files = directory.listFiles();
for (File file : files)
	if(file.getName().compareTo("SearchHistory.db") != 0 && 
		file.getName().compareTo("SearchHistory.lg") != 0)
		if(file.delete())
			out.println("Sucessfully deleted file: "+file.getName());
		else
			out.println("Error in deleting file");
%>