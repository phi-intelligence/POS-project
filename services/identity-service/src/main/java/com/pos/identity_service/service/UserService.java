package com.pos.identity_service.service;

import com.pos.identity_service.dto.UserCreateRequest;
import com.pos.identity_service.dto.UserDto;
import com.pos.identity_service.dto.UserStatusUpdateRequest;
import com.pos.identity_service.dto.UserUpdateRequest;
import com.pos.identity_service.model.Role;
import com.pos.identity_service.model.User;
import com.pos.identity_service.repository.RoleRepository;
import com.pos.identity_service.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        PasswordPolicy.validateOrThrow(request.getUsername(), request.getPassword());

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setAdminUnitId(request.getAdminUnitId());
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUser(Long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<UserDto> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id).map(user -> {
            if (request.getRoleId() != null) {
                Role role = roleRepository.findById(request.getRoleId())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found"));
                user.setRole(role);
            }
            if (request.getAdminUnitId() != null) {
                user.setAdminUnitId(request.getAdminUnitId());
            }
            User saved = userRepository.save(user);
            return toDto(saved);
        });
    }

    @Transactional
    public Optional<UserDto> updateStatus(Long id, UserStatusUpdateRequest request) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(request.getStatus());
            User saved = userRepository.save(user);
            return toDto(saved);
        });
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAdminUnitId(user.getAdminUnitId());
        dto.setStatus(user.getStatus());
        if (user.getRole() != null) {
            dto.setRoleName(user.getRole().getName());
        }
        return dto;
    }
}

