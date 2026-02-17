package com.example.demo.Authentication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface UserRepository extends JpaRepository<UserModel,Integer> {
    Optional<UserModel> findByid(Integer id);
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsername(String username);
}
