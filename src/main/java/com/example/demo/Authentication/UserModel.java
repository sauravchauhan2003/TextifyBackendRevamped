package com.example.demo.Authentication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    // Null or dummy for OAuth users
    private String password;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(unique = true)
    private String public_key;

}