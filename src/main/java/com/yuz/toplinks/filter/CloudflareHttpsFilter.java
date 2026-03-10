package com.yuz.toplinks.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Filter to detect Cloudflare HTTPS requests when running behind a proxy (like Nginx) 
 * that communicates via HTTP.
 * 
 * It checks the "CF-Visitor" header which is added by Cloudflare.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CloudflareHttpsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String cfVisitor = httpRequest.getHeader("CF-Visitor");
            
            // Cloudflare sends CF-Visitor: {"scheme":"https"} when the original request was HTTPS
            if (cfVisitor != null && cfVisitor.contains("\"scheme\":\"https\"")) {
                chain.doFilter(new HttpsRequestWrapper(httpRequest), response);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }

    private static class HttpsRequestWrapper extends HttpServletRequestWrapper {
        public HttpsRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getScheme() {
            return "https";
        }

        @Override
        public boolean isSecure() {
            return true;
        }
        
        @Override
        public int getServerPort() {
            return 443;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append("https://");
            url.append(getServerName());
            url.append(getRequestURI());
            return url;
        }
    }
}