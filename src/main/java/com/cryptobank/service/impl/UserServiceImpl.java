package com.cryptobank.service.impl;

import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.domain.enums.UserRole;
import com.cryptobank.repository.UserRepository;
import com.cryptobank.security.SecurityUtils;
import com.cryptobank.service.UserService;
import com.cryptobank.web.dto.request.RegisterRequest;
import com.cryptobank.web.exception.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityUtils securityUtils;

    public UserServiceImpl(UserRepository users, PasswordEncoder encoder, SecurityUtils securityUtils) {
        this.users = users;
        this.encoder = encoder;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    public UserEntity register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("An account already exists for that email");
        }
        if (req.dateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw ApiException.badRequest("You must be at least 18 years old to open an account");
        }
        UserEntity u = new UserEntity();
        u.setFullName(req.fullName());
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setDateOfBirth(req.dateOfBirth());
        u.setPanNumber(req.panNumber().toUpperCase());
        u.setAddress(req.address());
        u.setRole(UserRole.ROLE_CUSTOMER);
        return users.save(u);
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity currentUser() {
        return securityUtils.getCurrentUser();
    }

    @Override
    @Transactional
    public void changePassword(UserEntity user, String currentPassword, String newPassword) {
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.forbidden("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        users.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserEntity> allUsers() {
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity byId(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity byEmail(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
