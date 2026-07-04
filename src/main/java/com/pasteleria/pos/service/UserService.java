package com.pasteleria.pos.service;

import com.pasteleria.pos.domain.entity.Company;
import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.dto.UserRequest;
import com.pasteleria.pos.dto.UserResponse;
import com.pasteleria.pos.exception.ApiException;
import com.pasteleria.pos.mapper.DtoMapper;
import com.pasteleria.pos.repository.UserRepository;
import com.pasteleria.pos.security.SecurityUtils;
import com.pasteleria.pos.security.UserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyService companyService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CompanyService companyService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyService = companyService;
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAllWithCompany().stream()
                .map(DtoMapper::toUserResponse)
                .toList();
    }

    public List<UserResponse> listSellers() {
        return userRepository.findByRoleInWithCompany(List.of(UserRole.SELLER, UserRole.ADMIN))
                .stream()
                .map(DtoMapper::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        validateRoleAssignment(request.role());
        if (userRepository.findByUsername(normalizeUsername(request.username())).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El usuario ya está registrado");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(request.name());
        user.setUsername(request.username().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(request.active());
        user.setBirthDate(request.birthDate());
        user.setCompany(resolveCompany(request.companyId(), request.role()));
        return DtoMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        validateRoleAssignment(request.role());
        if (!user.getUsername().equalsIgnoreCase(request.username())
                && userRepository.findByUsername(normalizeUsername(request.username())).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El usuario ya está registrado");
        }
        user.setName(request.name());
        user.setUsername(request.username().toLowerCase());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setRole(request.role());
        user.setActive(request.active());
        user.setBirthDate(request.birthDate());
        user.setCompany(resolveCompany(request.companyId(), request.role()));
        return DtoMapper.toUserResponse(userRepository.save(user));
    }

    public User getUserEntity(UUID id) {
        return userRepository.findByIdWithCompany(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void validateRoleAssignment(UserRole role) {
        UserPrincipal current = SecurityUtils.currentUser();
        if (current.getRole() == UserRole.ADMIN && role == UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puede asignar rol SUPER_ADMIN");
        }
    }

    private Company resolveCompany(UUID companyId, UserRole role) {
        if (companyId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El usuario debe pertenecer a una empresa sin importar el rol");
        }
        Company company = companyService.getCompanyEntity(companyId);
        if (!company.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La empresa seleccionada no está activa");
        }
        return company;
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
