<%--
  Created by IntelliJ IDEA.
  User: jdosornio
  Date: 4/05/17
  Time: 06:47 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>$Title$</title>
      <script src="js/jquery-3.2.1.min.js"></script>
  </head>
  <body>
  $END$
  <script>
          jQuery.post( "http://148.231.90.7:8080/dataplugin/rs/p/exec",
              { name: "QueretaroSummaryPlugin", args: ""} );
              //QueretaroSummaryPlugin
  </script>
  </body>
</html>
