package com.yuz.toplinks.service;

import com.yuz.toplinks.entity.SysUser;
import com.yuz.toplinks.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCacheEvictKeyUsesEmailParam() throws Exception {
        Method method = UserService.class.getMethod("register", String.class, String.class, String.class);
        CacheEvict annotation = method.getAnnotation(CacheEvict.class);
        assertNotNull(annotation, "register method should have @CacheEvict");
        assertEquals("#email", annotation.key(), "@CacheEvict key should reference #email parameter");
    }

    @Test
    void registerCreatesNewUser() {
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        SysUser user = userService.register("test@example.com", "pass123", "Test");

        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encoded", user.getPassword());
        assertEquals("Test", user.getNickname());
        verify(userMapper).insert(any(SysUser.class));
    }

    @Test
    void registerThrowsWhenEmailExists() {
        when(userMapper.selectOne(any())).thenReturn(new SysUser());

        assertThrows(IllegalArgumentException.class,
                () -> userService.register("existing@example.com", "pass123", null));
    }
}
