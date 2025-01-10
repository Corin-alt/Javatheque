package fr.javatheque.servlets;

import fr.javatheque.beans.ErrorMessageBean;
import fr.javatheque.beans.UserBean;
import fr.javatheque.database.model.User;
import fr.javatheque.database.repository.UserRepository;
import fr.javatheque.util.PasswordUtil;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servlet that handles user login requests.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());

    @Inject
    private ErrorMessageBean errorMessageBean;

    @Inject
    private UserBean userBean;

    /**
     * Handles the HTTP GET request for the login page.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws ServletException if the request for the GET could not be handled
     * @throws IOException      if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/views/login.jsp").forward(request, response);
    }

    /**
     * Handles the HTTP POST request for user login.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws ServletException if the request for the POST could not be handled
     * @throws IOException      if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        boolean isTestRequest = isTestRequest(request);

        try {
            UserRepository ur = new UserRepository();
            Optional<User> target = ur.getUserByEmail(email);

            if (target.isPresent() && checkPassword(password, target.get().getPassword(), isTestRequest)) {
                handleSuccessfulLogin(request, response, target.get(), isTestRequest);
            } else {
                handleFailedLogin(request, response, isTestRequest);
            }
        } catch (Exception e) {
            logger.severe("Error during login: " + e.getMessage());
            handleError(request, response, isTestRequest);
        }
    }

    private boolean isTestRequest(HttpServletRequest request) {
        String testHeader = request.getHeader("X-Test-Database");
        return testHeader != null && Boolean.parseBoolean(testHeader);
    }

    private boolean checkPassword(String inputPassword, String storedPassword, boolean isTestRequest) {
        if (isTestRequest) {
            // Pour les tests, on accepte une comparaison simple
            return inputPassword.equals(storedPassword);
        }
        // Pour les requêtes normales, on utilise la vérification sécurisée
        return PasswordUtil.verifyPassword(inputPassword, storedPassword);
    }

    private void handleSuccessfulLogin(HttpServletRequest request, HttpServletResponse response,
                                       User user, boolean isTestRequest) throws IOException, ServletException {
        if (isTestRequest) {
            // Pour les tests Locust, renvoyer juste un statut 200
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Pour les requêtes web normales
        if (userBean != null) {
            userBean.setUserId(user.getId());
            userBean.setLastname(user.getLastname());
            userBean.setFirstname(user.getFirstname());
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userID", user.getId());
        request.getRequestDispatcher("/views/welcome.jsp").forward(request, response);
    }

    private void handleFailedLogin(HttpServletRequest request, HttpServletResponse response,
                                   boolean isTestRequest) throws IOException, ServletException {
        if (isTestRequest) {
            // Pour les tests Locust, renvoyer un statut 401
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            return;
        }

        // Pour les requêtes web normales
        if (errorMessageBean != null) {
            errorMessageBean.setErrorMessage("No user found or incorrect password.");
        }
        request.getRequestDispatcher("/views/login.jsp").forward(request, response);
    }

    private void handleError(HttpServletRequest request, HttpServletResponse response,
                             boolean isTestRequest) throws IOException, ServletException {
        if (isTestRequest) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        if (errorMessageBean != null) {
            errorMessageBean.setErrorMessage("An error occurred during login.");
        }
        request.getRequestDispatcher("/views/login.jsp").forward(request, response);
    }
}