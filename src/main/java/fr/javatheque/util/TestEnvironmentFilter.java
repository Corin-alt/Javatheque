package fr.javatheque.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.logging.Logger;

@WebFilter("/*")
public class TestEnvironmentFilter implements Filter {
    private static final Logger logger = Logger.getLogger(TestEnvironmentFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String isTestHeader = httpRequest.getHeader("X-Test-Database");

        logger.info("Request URI: " + httpRequest.getRequestURI());
        logger.info("X-Test-Database header: " + isTestHeader);

        boolean isTestMode = Boolean.parseBoolean(isTestHeader);
        DatabaseUtils.setTestEnvironment(isTestMode);
        logger.info("Set test environment to: " + isTestMode);

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.severe("Error in filter chain: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseUtils.clearEnvironment();
            logger.info("Cleared test environment");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("TestEnvironmentFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("TestEnvironmentFilter destroyed");
    }
}