package com.example.demo.Messaging;

import com.example.demo.Authentication.UserModel;
import com.example.demo.Authentication.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message")
public class MessageController {
    @Autowired
    private UserRepository repository;
    @GetMapping("/explore")
    public ResponseEntity<?> getExplorePage(Authentication authentication){
        System.out.println("🔥 Controller reached");
        UserModel user = (UserModel) authentication.getPrincipal();
        int id= user.getId();
        return ResponseEntity.ok(repository.findOtherUsersWithPublicKey(id));
    }
}
