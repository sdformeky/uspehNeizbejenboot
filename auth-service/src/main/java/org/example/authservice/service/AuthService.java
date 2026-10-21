package org.example.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.UserDto;
import org.example.authservice.exception.ConflictException;
import org.example.authservice.exception.NotFoundException;
import org.example.authservice.exception.UnauthorizedException;
import org.example.authservice.model.Role;
import org.example.authservice.model.RoleName;
import org.example.authservice.model.User;
import org.example.authservice.repository.RoleRepository;
import org.example.authservice.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    // BCrypt: в базе лежит только хэш, сам пароль не восстановить
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserDto register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email " + email + " уже зарегистрирован");
        }
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new NotFoundException("Роль STUDENT не найдена"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.getRoles().add(studentRole);
        userRepository.save(user);
        log.info("Зарегистрирован пользователь {} (id {})", email, user.getId());
        return UserDto.from(user);
    }

    public UserDto login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Неверный email или пароль"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Неудачный вход: {}", email);
            throw new UnauthorizedException("Неверный email или пароль");
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Пользователь заблокирован");
        }
        log.info("Вход пользователя {} (id {})", email, user.getId());
        return UserDto.from(user);
    }

    public List<UserDto> findAll() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserDto::from)
                .toList();
    }

    public UserDto findById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }
}
