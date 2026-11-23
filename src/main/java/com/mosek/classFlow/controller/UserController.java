package com.mosek.classFlow.controller;

import com.mosek.classFlow.business.AuthService;
import com.mosek.classFlow.dto.AuthDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<AuthDtos.UserResponse> createUser(@Valid @RequestBody AuthDtos.CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUserByAdmin(request));
    }

    @GetMapping
    public ResponseEntity<List<AuthDtos.UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }
}
