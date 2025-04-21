<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>.NETFlix - Login</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #273c75;
            margin: 0;
            padding: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
            flex-direction: column;
        }

        label {
            display: block;
            margin-bottom: 8px;
            font-weight: bold;
        }

        input {
            width: 100%;
            padding: 8px;
            margin-bottom: 16px;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-sizing: border-box;
        }

        button {
            background-color: #e056fd;
            color: #fff;
            padding: 10px 15px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }

        button:hover {
            background-color: #be2edd;
        }

        form {
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
            padding: 20px;
            width: 300px;
            margin-top: 20px;
        }

        a {
            text-decoration: none;
            color: #fff;
        }

        .error-message {
            font-weight: bold;
            font-size: 18px;
            color: #eb3b5a;
            margin-bottom: 10px;
            text-align: center;
            width: 100%;
        }
    </style>
</head>
<body>

<h1><a href="${pageContext.request.contextPath}/">.NETFlix</a></h1>

<c:if test="${not empty errorMessageBean.errorMessage}">
    <div class="error-message">
            ${errorMessageBean.errorMessage}
    </div>
</c:if>

<form id="login_user" action="login" method="POST">
    <label for="email">Email: </label>
    <input type="email" id="email" name="email" required>

    <label for="password">Password: </label>
    <input type="password" id="password" name="password" required>

    <button type="submit">Connect</button>
</form>

</body>
</html>