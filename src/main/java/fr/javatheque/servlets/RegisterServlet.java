package fr.javatheque.servlets;

import fr.javatheque.beans.ErrorMessageBean;
import fr.javatheque.beans.UserBean;
import fr.javatheque.database.model.User;
import fr.javatheque.database.repository.UserRepository;
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
 * Servlet that handles user registration requests.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(RegisterServlet.class.getName());

    @Inject
    private ErrorMessageBean errorMessageBean;

    @Inject
    private UserBean userBean;

    /**
     * Handles the HTTP GET request for the registration page.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws ServletException if the request for the GET could not be handled
     * @throws IOException      if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/views/register.jsp").forward(request, response);
    }

    /**
     * Handles the HTTP POST request for user registration.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws ServletException if the request for the POST could not be handled
     * @throws IOException      if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isTestRequest = isTestRequest(request);

        try {
            String lastname = request.getParameter("lastname");
            String firstname = request.getParameter("firstname");
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            if (!validateInput(lastname, firstname, email, password)) {
                handleError(request, response, "Missing or invalid input data", isTestRequest);
                return;
            }

            UserRepository ur = new UserRepository();
            Optional<User> target = ur.getUserByEmail(email);

            if (target.isPresent()) {
                handleError(request, response, "This email is already used.", isTestRequest);
                return;
            }

            User user = ur.createUser(new User(lastname, firstname, email, password, isTestRequest));

            handleSuccessfulRegistration(request, response, user, isTestRequest);

        } catch (Exception e) {
            logger.severe("Error during registration: " + e.getMessage());
            handleError(request, response, "Registration failed: " + e.getMessage(), isTestRequest);
        }
    }

    private boolean isTestRequest(HttpServletRequest request) {
        String testHeader = request.getHeader("X-Test-Database");
        return testHeader != null && Boolean.parseBoolean(testHeader);
    }

    private boolean validateInput(String lastname, String firstname, String email, String password) {
        return lastname != null && !lastname.trim().isEmpty() &&
                firstname != null && !firstname.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }

    private void handleSuccessfulRegistration(HttpServletRequest request, HttpServletResponse response,
                                              User user, boolean isTestRequest)
            throws ServletException, IOException {

        if (isTestRequest) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (userBean != null) {
            userBean.setUserId(user.getId());
            userBean.setLastname(user.getLastname());
            userBean.setFirstname(user.getFirstname());
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userID", user.getId());

        request.getRequestDispatcher("/views/welcome.jsp").forward(request, response);
    }

    private void handleError(HttpServletRequest request, HttpServletResponse response,
                             String message, boolean isTestRequest)
            throws ServletException, IOException {

        if (isTestRequest) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return;
        }

        if (errorMessageBean != null) {
            errorMessageBean.setErrorMessage(message);
        }
        request.getRequestDispatcher("/views/register.jsp").forward(request, response);
    }
}