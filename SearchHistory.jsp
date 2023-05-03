<%@ page import="project.*"%>
<%@ page import="java.io.File"%>

<html>
<head>
<title> History </title>
<script>
function reload() {
    out.print("");
    window.location.reload();
}
</script>
<script src="http://code.jquery.com/jquery-1.10.1.min.js"></script>
<script>
$(document).ready(function(e) {
	$("#clearHistory").click(function(e) {
        e.preventDefault();
        if (confirm('Are you sure you want to Delete Search History?')) {
        // Save it!
			$.ajax({
                type: "POST",
                url: "SearchHistoryDelete.jsp",
                success: function(msg){
                    alert(msg)
                },
			});
        } else {
        // Do nothing!
        }
	}); 
});
</script>
<style type="text/css">
input {
    padding: 8px;
}
div {
    transform: translate(0%, -64px);
}
h4 {
    transform: translate(30%, 40px);
}
h5 {
    transform: translate(65%, -80px);
}
h6 {
    transform: translate(55%, -20px);
}
</style>
</head> 

<body>

<h4>
<form method="post" action="Search.html"> 
<input type="submit" value="   Back to Search Engine   ">
</form>
</h4>
<h6>
<form method="post" action="SearchHistory.jsp">
<input type="submit" id="clearHistory" value="   Clear All History   ">
</form>
<form method="post" action="SearchHistory.jsp">
<input type="submit" name="clear" value="   Clear All Ticks   " onClick="reload()">
</form>
</h6>

<%
Database History = new Database("SearchHistory", "1");
int HistoryIndex = History.size();
%>

<form action="Search.jsp">
<h5>
<input type="submit" value="     Search     ">
</h5>
<div>
<table align=center border=1 style="background-color:#e8f4f8">
<%
for (int i = HistoryIndex - 1; i >= 0; i--) {
    String word = History.get(String.valueOf(i));
%>
    <tr>
        <td>
        <BR>
    <input type="checkbox" name="history" value="<%=word%>">
<%
    out.print(word);
%>
        </td>
    </tr>
<%
}
%>
</form>
</div>

</body>
</html>
