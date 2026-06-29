package com.example.demo.Messaging;

import com.example.demo.Authentication.UserModel;
import com.example.demo.Authentication.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private ChatRoomRepository chatRoomRepo;

    @Autowired
    private ChatParticipantRepository participantRepo;

    @Autowired
    private UserRepository userRepo;

    public static class CreateGroupRequest {
        public String name;
        public String description;
        public List<String> memberEmails;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request, Authentication authentication) {
        UserModel user = (UserModel) authentication.getPrincipal();

        ChatRoom room = new ChatRoom();
        room.setGroup(true);
        room.setName(request.name);
        room.setDescription(request.description);
        room.setCreatedAt(Instant.now());
        chatRoomRepo.save(room);

        ChatParticipant owner = new ChatParticipant();
        owner.setChatRoom(room);
        owner.setUser(user);
        owner.setRole(ChatParticipant.Role.OWNER);
        participantRepo.save(owner);

        if (request.memberEmails != null) {
            for (String email : request.memberEmails) {
                userRepo.findByEmail(email).ifPresent(memberUser -> {
                    if (!memberUser.getEmail().equals(user.getEmail())) {
                        ChatParticipant p = new ChatParticipant();
                        p.setChatRoom(room);
                        p.setUser(memberUser);
                        p.setRole(ChatParticipant.Role.MEMBER);
                        participantRepo.save(p);
                    }
                });
            }
        }
        return ResponseEntity.ok(room);
    }

    public static class UpdateGroupRequest {
        public String name;
        public String description;
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody UpdateGroupRequest request, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.ADMIN, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        room.setName(request.name);
        room.setDescription(request.description);
        chatRoomRepo.save(room);
        return ResponseEntity.ok(room);
    }

    public static class RenameRequest {
        public String name;
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<?> renameGroup(@PathVariable Long id, @RequestBody RenameRequest request, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.ADMIN, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        room.setName(request.name);
        chatRoomRepo.save(room);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{id}/invite-link")
    public ResponseEntity<?> getInviteLink(@PathVariable Long id, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.ADMIN, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        if (room.getInviteLink() == null) {
            room.setInviteLink(UUID.randomUUID().toString());
            chatRoomRepo.save(room);
        }
        return ResponseEntity.ok("{\"inviteLink\":\"" + room.getInviteLink() + "\"}");
    }

    @PostMapping("/join/{inviteLink}")
    public ResponseEntity<?> joinViaLink(@PathVariable String inviteLink, Authentication auth) {
        UserModel user = (UserModel) auth.getPrincipal();
        ChatRoom room = chatRoomRepo.findByInviteLink(inviteLink).orElse(null);
        if (room == null) return ResponseEntity.status(404).body("Invalid link");

        if (participantRepo.findByChatRoomAndUser(room, user).isPresent()) {
            return ResponseEntity.badRequest().body("Already joined");
        }

        ChatParticipant p = new ChatParticipant();
        p.setChatRoom(room);
        p.setUser(user);
        p.setRole(ChatParticipant.Role.MEMBER);
        participantRepo.save(p);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable Long id, Authentication auth) {
        UserModel user = (UserModel) auth.getPrincipal();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        ChatParticipant p = participantRepo.findByChatRoomAndUser(room, user).orElseThrow();
        
        participantRepo.delete(p);
        if (participantRepo.findByChatRoom(room).isEmpty()) {
            chatRoomRepo.delete(room);
        }
        return ResponseEntity.ok("Left group");
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteGroup(@PathVariable Long id, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        participantRepo.deleteByChatRoom(room);
        chatRoomRepo.delete(room);
        return ResponseEntity.ok("Deleted group");
    }

    public static class MemberEmailsRequest {
        public List<String> memberEmails;
    }

    @PostMapping("/{id}/add-members")
    public ResponseEntity<?> addMembers(@PathVariable Long id, @RequestBody MemberEmailsRequest request, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.ADMIN, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        
        for (String email : request.memberEmails) {
            userRepo.findByEmail(email).ifPresent(memberUser -> {
                if (participantRepo.findByChatRoomAndUser(room, memberUser).isEmpty()) {
                    ChatParticipant p = new ChatParticipant();
                    p.setChatRoom(room);
                    p.setUser(memberUser);
                    p.setRole(ChatParticipant.Role.MEMBER);
                    participantRepo.save(p);
                }
            });
        }
        return ResponseEntity.ok("Members added");
    }

    @PostMapping("/{id}/remove-members")
    public ResponseEntity<?> removeMembers(@PathVariable Long id, @RequestBody MemberEmailsRequest request, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.ADMIN, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        
        for (String email : request.memberEmails) {
            userRepo.findByEmail(email).ifPresent(memberUser -> {
                participantRepo.findByChatRoomAndUser(room, memberUser).ifPresent(p -> {
                    if (p.getRole() != ChatParticipant.Role.OWNER) {
                        participantRepo.delete(p);
                    }
                });
            });
        }
        return ResponseEntity.ok("Members removed");
    }

    @PutMapping("/{id}/promote/{userId}")
    public ResponseEntity<?> promoteAdmin(@PathVariable Long id, @PathVariable Integer userId, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        UserModel target = userRepo.findById(userId).orElseThrow();
        ChatParticipant p = participantRepo.findByChatRoomAndUser(room, target).orElseThrow();
        p.setRole(ChatParticipant.Role.ADMIN);
        participantRepo.save(p);
        return ResponseEntity.ok("Promoted");
    }

    @PutMapping("/{id}/demote/{userId}")
    public ResponseEntity<?> demoteAdmin(@PathVariable Long id, @PathVariable Integer userId, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        UserModel target = userRepo.findById(userId).orElseThrow();
        ChatParticipant p = participantRepo.findByChatRoomAndUser(room, target).orElseThrow();
        p.setRole(ChatParticipant.Role.MEMBER);
        participantRepo.save(p);
        return ResponseEntity.ok("Demoted");
    }

    @PutMapping("/{id}/transfer-ownership/{userId}")
    public ResponseEntity<?> transferOwnership(@PathVariable Long id, @PathVariable Integer userId, Authentication auth) {
        if (!hasRole(id, auth, ChatParticipant.Role.OWNER)) return ResponseEntity.status(403).build();
        ChatRoom room = chatRoomRepo.findById(id).orElseThrow();
        
        UserModel newOwner = userRepo.findById(userId).orElseThrow();
        ChatParticipant newOwnerP = participantRepo.findByChatRoomAndUser(room, newOwner).orElseThrow();
        newOwnerP.setRole(ChatParticipant.Role.OWNER);
        participantRepo.save(newOwnerP);
        
        UserModel currentUser = (UserModel) auth.getPrincipal();
        ChatParticipant currentOwnerP = participantRepo.findByChatRoomAndUser(room, currentUser).orElseThrow();
        currentOwnerP.setRole(ChatParticipant.Role.ADMIN);
        participantRepo.save(currentOwnerP);
        
        return ResponseEntity.ok("Ownership transferred");
    }

    private boolean hasRole(Long roomId, Authentication auth, ChatParticipant.Role... allowedRoles) {
        UserModel user = (UserModel) auth.getPrincipal();
        ChatRoom room = chatRoomRepo.findById(roomId).orElse(null);
        if (room == null) return false;
        
        Optional<ChatParticipant> p = participantRepo.findByChatRoomAndUser(room, user);
        if (p.isEmpty()) return false;
        
        for (ChatParticipant.Role r : allowedRoles) {
            if (p.get().getRole() == r) return true;
        }
        return false;
    }
}
