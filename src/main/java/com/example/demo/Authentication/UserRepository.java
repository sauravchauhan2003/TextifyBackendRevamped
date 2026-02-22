package com.example.demo.Authentication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface UserRepository extends JpaRepository<UserModel,Integer> {
    Optional<UserModel> findByid(Integer id);
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsername(String username);
    @Query(value = """
        SELECT *
        FROM user_model
        WHERE id <> :id
        AND public_key IS NOT NULL
        """, nativeQuery = true)
    List<UserModel> findOtherUsersWithPublicKey(@Param("id") Integer id);

}
