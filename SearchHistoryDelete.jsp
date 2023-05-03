<%@ page import="java.io.File"%>

<%
File searchHistorydb = new File("database/SearchHistory.db");
if (searchHistorydb.delete())
	out.println("Sucessfully deleted file: "+searchHistorydb.getName());
File searchHistorylg = new File("database/SearchHistory.lg");
if (searchHistorylg.delete())
	out.println("Sucessfully deleted file: "+searchHistorylg.getName());;
%>