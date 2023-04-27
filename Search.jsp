<%@ page import="project.*"%>

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
</script>
<style type="text/css">
div {
    transform: translate(35%, 0%);
}
</style>
<style type="text/css">
h4 {
    transform: translate(30%, 0%);
}
</style>
</head> 

<body>

<%
String url = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
int num;
if(request.getParameter("number").compareTo("") != 0)
	num = Integer.valueOf(request.getParameter("number"));
else
	num = 300;
Spider spider = new Spider(url);
spider.crawl(num);
%>

<div>
<%=spider.print()%>
</div>

<%
	spider.saveDatabase();
%>

<h4>
<form>
<input type="submit" id="delete" value="   Delete Database   ">
</form>
<form method="post" action="Search.html"> 
<input type="submit" value="   Back to Search Engine   ">
</form>
</h4>

</body>
</html>
