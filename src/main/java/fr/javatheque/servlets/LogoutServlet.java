package fr.javatheque.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Servlet handling user logout requests.
 * This servlet supports both regular web requests and Locust test requests.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(LogoutServlet.class.getName());

    /**
     * Handles HTTP GET request for user logout.
     * Supports both regular web requests and test requests via X-Test-Database header.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws ServletException if the request cannot be handled
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isTestRequest = isTestRequest(request);

        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String userId = (String) session.getAttribute("userID");
                session.removeAttribute("userID");
                session.invalidate();

                if (isTestRequest) {
                    logger.info("Test user logged out: " + userId);
                    response.setStatus(HttpServletResponse.SC_OK);
                    return;
                } else {
                    logger.info("User logged out: " + userId);
                }
            } else if (isTestRequest) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }

            handleRegularLogout(request, response);

        } catch (Exception e) {
            logger.severe("Error during logout: " + e.getMessage());
            handleError(request, response, isTestRequest);
        }
    }

    /**
     * Checks if the current request is a Locust test request.
     *
     * @param request the HTTP request
     * @return true if it's a test request, false otherwise
     */
    private boolean isTestRequest(HttpServletRequest request) {
        String testHeader = request.getHeader("X-Test-Database");
        return testHeader != null && Boolean.parseBoolean(testHeader);
    }

    /**
     * Handles logout for regular web requests.
     * Forwards the user to the welcome page after successful logout.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    private void handleRegularLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/views/welcome.jsp").forward(request, response);
    }

    /**
     * Handles errors based on request type.
     * For test requests, sends an error status.
     * For regular requests, forwards to an error page.
     *
     * @param request       the HTTP request
     * @param response     the HTTP response
     * @param isTestRequest indicates whether this is a test request
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    private void handleError(HttpServletRequest request, HttpServletResponse response, boolean isTestRequest)
            throws IOException, ServletException {
        if (isTestRequest) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Logout failed");
        } else {
            request.setAttribute("error", "An error occurred during logout");
            request.getRequestDispatcher("/views/error.jsp").forward(request, response);
        }
    }
}