<%@ page import="project.*"%>
<%@ page import="java.util.Vector"%>
<%@ page import="java.util.Collections"%>

<html>
<head>
<title> Words </title>
<script>
function reload() {
    out.print("");
    window.location.reload();
}
</script>
<style type="text/css">
input {
    padding: 8px;
}
div {
    transform: translate(0%, 0px);
}
h4 {
    transform: translate(30%, 40px);
}
h5 {
    transform: translate(65%, -17px);
}
h6 {
    transform: translate(55%, 44px);
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
<form method="post" action="Words.jsp">
<input type="submit" name="clear" value="   Clear All Ticks   " onClick="reload()">
</form>
</h6>

<%
String url = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
Spider spider = new Spider(url);
%>


<%
Vector<String> words = spider.printWords();
Collections.sort(words);
%>
<form action="Search.jsp">
<h5>
<input type="submit" value="     Search     ">
</h5>
<div>
<table align=center border=1 style="background-color:#e8f4f8">
<%
int num = 0;
for (String word : words) {
    if (num % 4 == 0) {
%>
    <tr>
<%
    }
%>
        <td>
        <BR>
    <input type="checkbox" name="word" value="<%=word%>">
<%
    out.print(word);
%>
        </td>
<%
    if (num % 4 == 3) {
%>
    </tr>
<%
    }
    num++;
}
%>
</form>
</div>

</body>
</html>
