<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Success</title>
    <style>
        .success-message {
            font-weight: bold;
            font-size: 18px;
            color: #4cd137;
            margin-bottom: 10px;
            text-align: center;
            padding: 20px;
        }
    </style>
</head>
<body>
<c:if test="${not empty successMessageBean.successMessage}">
    <div class="success-message">
            ${successMessageBean.successMessage}
    </div>
</c:if>

<c:if test="${not empty userBean.fullName}">
    <div class="welcome-message">
        Welcome, ${userBean.fullName}!
    </div>
</c:if>
</body>
</html>