package com.fitness.module.user.controller;

import com.fitness.common.Result;
import com.fitness.module.user.dto.*;
import com.fitness.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(userService.register(req));
    }

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(userService.login(req));
    }

    @PostMapping("/refresh-token")
    public Result<TokenResponse> refreshToken(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.refreshToken(userId));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody PasswordRequest req) {
        userService.changePassword(userId, req);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute("userId") Long userId,
                               @RequestHeader("Authorization") String authHeader) {
        userService.logout(authHeader);
        return Result.success();
    }
}
