<%@ page import="project.*"%>
<%@ page import="java.util.Vector"%>
<%@ page import="java.util.Collections"%>
<%@ page import="jdbm.helper.FastIterator"%>

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

<%-- <%
Database db = new Database("PageIDtoTriWordID", "1");
Database worddb = new Database("WordIDtoWord", "1");
FastIterator iter = db.getKeys();
String Word;
while ((Word = (String)iter.next()) != null) {
    String[] freqs = db.get(Word).split(";");
    for (String freq : freqs) {
        String[] words = freq.split(" ");
        out.println(worddb.get(words[0]) + " " + worddb.get(words[1]) +
        " " + worddb.get(words[2]) + " " + words[3] + "<BR>");
    }
    out.print("Done<BR><BR>");
}
%> --%>

<%-- <%
Database a = new Database("a", "1");
FastIterator iter = a.getKeys();
String Word;
while ((Word = (String)iter.next()) != null) {
    out.print(a.get(Word) + "<BR>");
}
%> --%>

</body>
</html>
