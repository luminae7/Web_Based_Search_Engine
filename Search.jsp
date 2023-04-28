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
function similar(x){
    document.getElementById("hid1").value = x.id;
    document.forms[0].submit();
    out.print("HIHI");
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
<input type="hidden" id="hid1"  name="hid1">
</form>

<%
String query = "";
if(request.getParameter("hid1")!=null)
{
    String PageID = request.getParameter("hid1");
	Database PageIDtoTopFiveWordID = new Database("PageIDtoTopFiveWordID", "1");
    Database WordIDtoWord = new Database("WordIDtoWord", "1");

    String[] wordIDs = PageIDtoTopFiveWordID.get(PageID).split(";");
    for (int i = 0; i < 5; i++) {
        String[] wordID_freq = wordIDs[i].split(" ");
        String word = WordIDtoWord.get(wordID_freq[0]);
        query += word + " ";
    }
}
else if (request.getParameter("query") != null) {
    query = request.getParameter("query");
}
SearchEngine se = new SearchEngine(query);
se.search();
%>

<h3>
You are searching for:
<%=se.getStopStemQuery()%>

<BR><BR><BR>

<form method="post" action="Search.html"> 
<input type="submit" value="   Back to Search Engine   ">
&nbsp &nbsp
<input type="submit" id="delete" value="   Delete Database   ">
</form>
</h3>

<div>
<%
Vector<String> results = se.print();
int i = 0;
int total = results.size();
%>
<table align=center border=1>
<%
for (String result : results) {
%>
    <tr>
        <td>
        <BR>
<%
    out.print(result);
    String PageID = se.getPageID(i);
%>
<BR>
<input type="submit" name="similar" value="   Similar Pages   " id="<%=PageID%>" onClick="similar(this,<%=total%>)">
<BR>
<BR>
<%
    i++;
}
%>
        </td>
    </tr>
</table>
</div>

</body>
</html>
