package com.yuz.toplinks.service;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuz.toplinks.entity.BaseEntity;
import com.yuz.toplinks.entity.SysUser;
import com.yuz.toplinks.mapper.SysUserMapper;

@Service
public class UserService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        SysUser user = findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + email);
        }
        return User.withUsername(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(Collections.emptyList())
                .build();
    }

    @Cacheable(value = "users", key = "#email")
    public SysUser findByEmail(String email) {
        return userMapper.selectOne(new QueryWrapper<SysUser>().eq("email", email));
    }

    @CacheEvict(value = "users", key = "#user.email")
    public SysUser register(String email, String password, String nickname) {
        if (findByEmail(email) != null) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        SysUser user = new SysUser();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : email);
        user.setStatus(BaseEntity.STATUS_ACTIVE);
        user.setCreateTime(new Date());
        userMapper.insert(user);
        return user;
    }

    @CacheEvict(value = "users", key = "#email")
    public SysUser findOrCreateOAuthUser(String googleId, String email, String name, String avatar) {
        // Try to find by googleId first, then by email
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("google_id", googleId));
        if (user == null) {
            user = findByEmail(email);
        }
        if (user == null) {
            user = new SysUser();
            user.setId(UUID.randomUUID().toString());
            user.setEmail(email);
            user.setGoogleId(googleId);
            user.setNickname(name);
            user.setAvatar(avatar);
            user.setStatus(BaseEntity.STATUS_ACTIVE);
            user.setCreateTime(new Date());
            userMapper.insert(user);
        } else {
            user.setGoogleId(googleId);
            user.setNickname(name);
            user.setAvatar(avatar);
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
        }
        return user;
    }
}
