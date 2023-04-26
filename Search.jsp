<%@ page import="project.*"%>

<html>
<head> <title> Results </title> </head> 
<body>

<%
String url = "http://www.cse.ust.hk";
int num;
if(request.getParameter("num") != null)
	num = Integer.valueOf(request.getParameter("num"));
else
	num = 30;
Spider spider = new Spider(url);
spider.crawl(num);
%>

<%=spider.print()%>

<%
	spider.saveDatabase();
%>

</body>
</html>
