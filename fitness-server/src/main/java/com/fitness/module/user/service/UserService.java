package com.fitness.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ErrorCode;
import com.fitness.module.user.dto.LoginRequest;
import com.fitness.module.user.dto.PasswordRequest;
import com.fitness.module.user.dto.RegisterRequest;
import com.fitness.module.user.dto.TokenResponse;
import com.fitness.module.user.entity.User;
import com.fitness.module.user.mapper.UserMapper;
import com.fitness.security.JwtUtil;
import com.fitness.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public TokenResponse register(RegisterRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId());
        return new TokenResponse(token, 604800L);
    }

    public TokenResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        return new TokenResponse(token, 604800L);
    }

    public TokenResponse refreshToken(Long userId) {
        String token = jwtUtil.generateToken(userId);
        return new TokenResponse(token, 604800L);
    }

    public void changePassword(Long userId, PasswordRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    public void logout(String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenExpired(token)) {
            String tokenId = jwtUtil.getTokenId(token);
            long ttl = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
            blacklistService.add(tokenId, ttl);
        }
    }
}
