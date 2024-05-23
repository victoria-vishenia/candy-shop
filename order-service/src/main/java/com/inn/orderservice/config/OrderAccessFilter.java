package com.inn.orderservice.config;

import com.inn.orderservice.service.OrderService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

//@Component
public class OrderAccessFilter extends OncePerRequestFilter implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain
        filterChain) throws  ServletException, IOException {

            Authentication authentication = (Authentication) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
            if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                response.sendRedirect("http://localhost:8082/login/oauth2/code/keycloak");

            }

            String username = authentication.getName();
            boolean isAdmin = checkIfUserIsAdmin(authentication);

            if(request.getRequestURI().contains("/order/all")){
                if (isAdmin) {
                    filterChain.doFilter(request, response);
                } else {
                    setForbiddenResponse(response);
                }
                return;
            }

            if(request.getRequestURI().contains("/order/add")){
                filterChain.doFilter(request, response);
                return;
            }

            if (request.getRequestURI().contains("/order/find")) {
                if (isAdmin) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String orderNumber = request.getRequestURI().substring("/order/find/".length());
                String userId = getUserId(orderNumber);
                if (!username.equals(userId)) {
                    setForbiddenResponse(response);
                    return;
                }
                filterChain.doFilter(request, response);
                return;
            }

            if (request.getRequestURI().contains("/order/delete")) {
                if (isAdmin) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String orderNumber = request.getRequestURI().substring("/order/delete/".length());
                String userId = getUserId(orderNumber);
                if (!username.equals(userId)) {
                  setForbiddenResponse(response);
                    return;
                }
                filterChain.doFilter(request, response);
            }

            if (request.getRequestURI().contains("/order/update-order-items")) {
                if (isAdmin) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String orderNumber = request.getRequestURI().substring("/order/update-order-items/".length());
                String userId = getUserId(orderNumber);
                if (!username.equals(userId)) {
                    setForbiddenResponse(response);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        }

        private void setForbiddenResponse(HttpServletResponse response) throws IOException {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.print("You don't have enough rights for access");
        }
    private boolean checkIfUserIsAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
    private String getUserId(String orderNumber){
        OrderService orderService = applicationContext.getBean(OrderService.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return  orderService.getByOrderNumber(orderNumber, authentication).getUserId();
    }
}