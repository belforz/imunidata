package com.example.imunidata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

// made by Claude

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int httpStatus = SC_OK;

        public StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            super.setStatus(sc);
            this.httpStatus = sc;
        }

        @Override
        public void sendError(int sc) throws IOException {
            super.sendError(sc);
            this.httpStatus = sc;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            super.sendError(sc, msg);
            this.httpStatus = sc;
        }

        public int getStatus() {
            return this.httpStatus;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper(response);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = responseWrapper.getStatus();
            String msg = String.format("%s %s -> %d (%d ms)", request.getMethod(), request.getRequestURI(), status, duration);
            if (status >= 500) {
                logger.error(msg);
            } else if (status >= 400) {
                logger.warn(msg);
            } else {
                logger.info(msg);
            }
        }
    }
}

