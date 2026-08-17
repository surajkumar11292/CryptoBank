package com.cryptobank.service;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.RegisterRequest;

import java.util.List;

public interface UserService {
    UserEntity register(RegisterRequest req);
    UserEntity currentUser();
    void changePassword(UserEntity user, String currentPassword, String newPassword);
    List<UserEntity> allUsers();
    UserEntity byId(Long id);
    UserEntity byEmail(String email);
}
