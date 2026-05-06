package com.lks.config;

import com.lks.bean.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionAuthenticationFilterTest {

    private final SessionAuthenticationFilter filter = new SessionAuthenticationFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leavesSecurityContextEmptyWhenSessionHasNoLoggedInUser() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void authenticatesSessionUserWithUserRole() throws ServletException, IOException {
        User user = userWithRole("USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("loggedInUser", user);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertSame(user, authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void authenticatesSessionUserWithAdminRole() throws ServletException, IOException {
        User admin = userWithRole("ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("loggedInUser", admin);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertSame(admin, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    private User userWithRole(String role) {
        User user = new User();
        user.setUsername("test-user");
        user.setRole(role);
        return user;
    }
}
