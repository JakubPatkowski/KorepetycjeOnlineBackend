package com.example.demo.service.entity;

import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Transactional
    public void addRoleToUser(UserEntity user, RoleEntity.Role role) {
        if (!roleRepository.existsByUserIdAndRole(user.getId(), role)) {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setUser(user);
            roleEntity.setRole(role);
            roleRepository.save(roleEntity);
        }
    }

    @Transactional
    public void removeRoleFromUser(UserEntity user, RoleEntity.Role role) {
        RoleEntity roleEntity = roleRepository.findByUserIdAndRole(user.getId(), role)
                .orElseThrow(() -> new ApiException("Role not found"));
        roleRepository.delete(roleEntity);
    }

    public Set<RoleEntity.Role> getUserRoles(Long userId) {
        return roleRepository.findByUserId(userId).stream()
                .map(RoleEntity::getRole)
                .collect(Collectors.toSet());
    }
}
