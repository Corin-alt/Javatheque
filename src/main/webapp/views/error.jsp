<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Error</title>
    <style>
        .error-message {
            font-weight: bold;
            font-size: 18px;
            color: #eb3b5a;
            margin-bottom: 10px;
            text-align: center;
            padding: 20px;
        }
    </style>
</head>
<body>
<c:if test="${not empty errorMessageBean.errorMessage}">
    <div class="error-message">
            ${errorMessageBean.errorMessage}
    </div>
</c:if>
</body>
</html>