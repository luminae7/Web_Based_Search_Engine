<%@ page import="project.*"%>
<%@ page import="java.util.Vector"%>

<html>
<head>
<title> Results </title>
<script src="http://code.jquery.com/jquery-1.10.1.min.js"></script>
<script>
$(document).ready(function(e) {
	$("#delete").click(function(e) {
        e.preventDefault();
        if (confirm('Are you sure you want to Delete Files?')) {
        // Save it!
			$.ajax({
                type: "POST",
                url: "Delete.jsp",
                success: function(msg){
                    alert(msg)
                },
			});
        } else {
        // Do nothing!
        }
	}); 
});
function similar(x) {
    document.getElementById("hid1").value = x.id;
    document.forms[0].submit();
    out.print("");
    window.location.reload();
}
</script>
<style type="text/css">
input {
    padding: 8px;
}
div {
    transform: translate(0%, 50px);
}
h3 {
    transform: translate(30%, 40px);
}
</style>
</head> 

<body>

<form action="Search.jsp">
<input type="hidden" id="hid1" name="similar">
</form>

<%
String query = "";
String dictionary[] = request.getParameterValues("word");
String history[] = request.getParameterValues("history");
// from similar pages
if (request.getParameter("similar") != null)
{
    String PageID = request.getParameter("similar");
	Database PageIDtoTopFiveWordID = new Database("PageIDtoTopFiveWordID", "1");
    Database WordIDtoWord = new Database("WordIDtoWord", "1");

    String[] wordIDs = PageIDtoTopFiveWordID.get(PageID).split(";");
    for (int i = 0; i < 5; i++) {
        String[] wordID_freq = wordIDs[i].split(" ");
        String word = WordIDtoWord.get(wordID_freq[0]);
        query += word + " ";
    }
}
// from dictionary
else if (dictionary != null && dictionary.length != 0) {
    for (String word : dictionary)
        query += word + " "; 
}
// from searchHistory
else if (history != null && history.length != 0) {
    for (String word : history)
        query += word.replaceAll("@", "\"") + " "; 
}
// from search
else if (request.getParameter("query") != null) {
    query = request.getParameter("query");
}
SearchEngine se = new SearchEngine(query);
se.phrase();
se.stopStem();
%>

<h3>
You are searching for:
<%=se.getStopStemQuery()%>
<BR><BR>
&nbsp &nbsp &nbsp &nbsp
Phrases:
<%=se.getPhrases()%>

<%
Database History = new Database("SearchHistory", "1");
int HistoryIndex = History.size();
History.add(String.valueOf(HistoryIndex), query.replaceAll("\"", "@"));
HistoryIndex++;
History.save();
%>

<%
Vector<String> sensitiveWords = se.getSensitiveWords();
if (!sensitiveWords.isEmpty()) {
%>

<BR><BR>
&nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp

<%
    out.print("These words violate our regulations: ");
    for (String word : sensitiveWords) {
        out.print(word + " ");
    }
}
%>

<BR><BR><BR>

<form method="post" action="SearchHistory.jsp"> 
<input type="submit" value="   Search History   ">
</form>

<form method="post" action="Search.html"> 
<input type="submit" value="   Back to Search Engine   ">
&nbsp &nbsp
<input type="submit" id="delete" value="   Delete Database   ">
</form>
</h3>

<div>
<%
if (sensitiveWords.isEmpty()) {
    se.search();
    Vector<Double> scores = se.printScores();
    Vector<String> results = se.printResults();
    int i = 0;
    int total = results.size();
    %>
    <table align=center border=1 style="background-color:#e8f4f8">
    <%
    for (String result : results) {

        if (scores.elementAt(i) != 0.0) {
    %>
        <tr>
        <td valign="top" style="padding: 20px;">
    <%
            int score = (int) (scores.elementAt(i) / scores.elementAt(0) * 100);
            out.print("<b>" + score + "</b>");
    %>
            </td>
            <td>
            <BR>
    <%
            out.print(result);
            String PageID = se.getPageID(i);
    %>
    <BR>
    <input type="submit" name="similar_button" value="   Similar Pages   " id="<%=PageID%>" onClick="similar(this)">
    <BR>
    <BR>
            </td>
        </tr>
    <%
            i++;
        }
    }
}
%>
</table>
</div>

</body>
</html>
