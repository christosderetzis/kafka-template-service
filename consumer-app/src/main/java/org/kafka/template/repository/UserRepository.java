package org.kafka.template.repository;

import org.kafka.template.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    UserEntity findByUserId(Integer userId);
}