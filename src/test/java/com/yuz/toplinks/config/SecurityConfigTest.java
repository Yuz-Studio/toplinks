package com.yuz.toplinks.config;

import com.yuz.toplinks.service.CustomOAuth2UserService;
import com.yuz.toplinks.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private UserService userService;

    @Mock
    private CustomOAuth2UserService customOAuth2UserService;

    private SecurityConfig createConfigWithClientId(String clientId) throws Exception {
        SecurityConfig config = new SecurityConfig(userService, customOAuth2UserService);
        Field field = SecurityConfig.class.getDeclaredField("googleClientId");
        field.setAccessible(true);
        field.set(config, clientId);
        return config;
    }

    @Test
    void googleOAuthDisabledWhenClientIdIsDisabled() throws Exception {
        SecurityConfig config = createConfigWithClientId("disabled");
        assertFalse(config.isGoogleOAuthEnabled());
    }

    @Test
    void googleOAuthDisabledWhenClientIdIsBlank() throws Exception {
        SecurityConfig config = createConfigWithClientId("");
        assertFalse(config.isGoogleOAuthEnabled());
    }

    @Test
    void googleOAuthDisabledWhenClientIdIsNull() throws Exception {
        SecurityConfig config = createConfigWithClientId(null);
        assertFalse(config.isGoogleOAuthEnabled());
    }

    @Test
    void googleOAuthEnabledWhenClientIdIsValid() throws Exception {
        SecurityConfig config = createConfigWithClientId("123456.apps.googleusercontent.com");
        assertTrue(config.isGoogleOAuthEnabled());
    }
}
