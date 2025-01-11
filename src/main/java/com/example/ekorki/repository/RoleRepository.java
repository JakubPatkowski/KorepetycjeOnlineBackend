package com.example.ekorki.repository;

import com.example.ekorki.entity.RoleEntity;
import com.example.ekorki.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    void deleteByUserAndRole(UserEntity user, RoleEntity.Role role);
    boolean existsByUserIdAndRole(Long userId, RoleEntity.Role role);
    List<RoleEntity> findByUserId(Long userId);

    Optional<RoleEntity> findByUserIdAndRole(Long id, RoleEntity.Role role);
}
