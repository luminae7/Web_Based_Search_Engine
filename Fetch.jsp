<%@ page import="project.*"%>
<%@ page import="java.util.Vector"%>
<%@ page import="java.io.File"%>

<html>
<head>
<title> Database </title>
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
</script>
<script type="text/javascript">
function alertName(){
alert("Done!");
} 
</script>
<style type="text/css">
input {
    padding: 8px;
}
div {
    transform: translate(0%, 50px);
}
h4 {
    transform: translate(30%, 40px);
}
</style>
</head> 

<body>

<h4>
<form method="post" action="Search.html"> 
<input type="submit" value="   Back to Search Engine   ">
&nbsp &nbsp
<input type="submit" id="delete" value="   Delete Database   ">
</form>
</h4>

<%
String url = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
if(request.getParameter("link").compareTo("") != 0)
	url = request.getParameter("link");
int num = 300;
if(request.getParameter("number").compareTo("") != 0)
	num = Integer.valueOf(request.getParameter("number"));
Spider spider = new Spider(url);
spider.crawl(num);
%>

<div>
<%
Vector<String> results = spider.print();
%>
<table align=center border=1 style="background-color:#e8f4f8">
<%
for (String result : results) {
%>
    <tr>
        <td>
        <BR>
<%
    out.print(result);
%>
        </td>
    </tr>
<%
}
%>
</div>

<%
spider.saveDatabase();
%>

</body>
</html>

<%
File titlewordfreqdb = new File("database/titlewordfreq.db");
titlewordfreqdb.delete();
File titlewordfreqlg = new File("database/titlewordfreq.lg");
titlewordfreqlg.delete();
File wordfreqdb = new File("database/wordfreq.db");
wordfreqdb.delete();
File wordfreqlg = new File("database/wordfreq.lg");
wordfreqlg.delete();
%>

<script type="text/javascript"> window.onload = alertName; </script>