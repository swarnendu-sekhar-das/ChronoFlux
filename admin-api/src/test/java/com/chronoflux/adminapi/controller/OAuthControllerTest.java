package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.service.TokenService;
import com.chronoflux.adminapi.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserService userService;

    private OAuthController oAuthController;

    @BeforeEach
    void setUp() {
        oAuthController = new OAuthController(tokenService, userService);
    }

    @Test
    void callback_HttpLocalhost_SetsSecureFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("admin@chronoflux.com");
        when(tokenService.verifyToken("dummy-token")).thenReturn(payload);

        oAuthController.callback(Map.of("idToken", "dummy-token"), request, response);

        Cookie cookie = response.getCookie("AUTH_TOKEN");
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
    }

    @Test
    void callback_HttpsOrForwardedProto_SetsSecureTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("admin@chronoflux.com");
        when(tokenService.verifyToken("dummy-token")).thenReturn(payload);

        oAuthController.callback(Map.of("idToken", "dummy-token"), request, response);

        Cookie cookie = response.getCookie("AUTH_TOKEN");
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
    }

    @Test
    void signout_ClearsCookieWithMatchingSecureFlag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuthController.signout(request, response);

        Cookie cookie = response.getCookie("AUTH_TOKEN");
        assertNotNull(cookie);
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.getSecure());
    }
}
