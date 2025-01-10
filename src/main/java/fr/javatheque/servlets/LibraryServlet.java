package fr.javatheque.servlets;

import fr.javatheque.database.model.Film;
import fr.javatheque.database.model.Library;
import fr.javatheque.database.model.User;
import fr.javatheque.database.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.google.gson.Gson;

/**
 * Servlet that handles requests related to the user's library.
 */
@WebServlet(name = "LibraryServlet", urlPatterns = {"/library"})
public class LibraryServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(LibraryServlet.class.getName());
    private final Gson gson = new Gson();

    /**
     * Handles the HTTP GET request for the user's library.
     *
     * @param request  the HttpServletRequest object
     * @param response the HttpServletResponse object
     * @throws ServletException if the request could not be handled
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean isTestRequest = isTestRequest(request);

        try {
            String userID = (String) request.getSession().getAttribute("userID");
            if (userID == null) {
                handleAuthenticationError(request, response, isTestRequest);
                return;
            }

            UserRepository ur = new UserRepository();
            Optional<User> target = ur.getUserById(userID);

            if (target.isEmpty()) {
                handleAuthenticationError(request, response, isTestRequest);
                return;
            }

            User user = target.get();
            String searchQuery = request.getParameter("search");
            Library library = user.getLibrary();

            List<Film> films = searchQuery != null && searchQuery.equalsIgnoreCase("all")
                    ? library.getFilms()
                    : searchFilmsInLibrary(library, searchQuery);

            handleSuccessfulRequest(request, response, films, isTestRequest);

        } catch (Exception e) {
            logger.severe("Error accessing library: " + e.getMessage());
            handleError(request, response, isTestRequest, e);
        }
    }

    private boolean isTestRequest(HttpServletRequest request) {
        String testHeader = request.getHeader("X-Test-Database");
        return testHeader != null && Boolean.parseBoolean(testHeader);
    }

    /**
     * Searches for films in the user's library based on the provided search query.
     *
     * @param library     the user's library
     * @param searchQuery the search query
     * @return the list of films matching the search query
     */
    private List<Film> searchFilmsInLibrary(Library library, String searchQuery) {
        List<Film> allFilms = library.getFilms();
        if (searchQuery == null || searchQuery.isEmpty()) {
            return allFilms;
        }
        return allFilms.stream()
                .filter(film -> film.getTitle().toLowerCase().contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void handleSuccessfulRequest(HttpServletRequest request, HttpServletResponse response,
                                         List<Film> films, boolean isTestRequest) throws ServletException, IOException {

        if (isTestRequest) {
            // Pour les tests Locust, renvoyer le JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(films));
            return;
        }

        // Pour les requêtes web normales
        request.setAttribute("films", films);
        request.getRequestDispatcher("/views/library.jsp").forward(request, response);
    }

    private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response,
                                           boolean isTestRequest) throws IOException, ServletException {

        if (isTestRequest) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login");
    }

    private void handleError(HttpServletRequest request, HttpServletResponse response,
                             boolean isTestRequest, Exception e) throws IOException, ServletException {

        if (isTestRequest) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error accessing library: " + e.getMessage());
            return;
        }

        // Pour les requêtes web normales
        request.setAttribute("error", "An error occurred while accessing your library");
        request.getRequestDispatcher("/views/error.jsp").forward(request, response);
    }
}