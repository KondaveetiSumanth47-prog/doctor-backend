package com.doctorappointment.controller;

import com.doctorappointment.entity.Notification;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.UserRepository;
import com.doctorappointment.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    private Long resolveCallerUserId(Long fallbackId, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            Optional<User> u = userRepository.findByEmail(authentication.getName());
            if (u.isPresent()) {
                return u.get().getId();
            }
        }
        return fallbackId != null ? notificationService.resolveUserId(fallbackId) : null;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(
            @PathVariable Long userId,
            Authentication authentication) {
        Long resolvedId = resolveCallerUserId(userId, authentication);
        if (resolvedId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(notificationService.getUserNotifications(resolvedId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long userId,
            Authentication authentication) {
        Long resolvedId = resolveCallerUserId(userId, authentication);
        long count = resolvedId != null ? notificationService.getUnreadCount(resolvedId) : 0;
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        Long resolvedId = resolveCallerUserId(userId, authentication);
        return ResponseEntity.ok(notificationService.markAsRead(id, resolvedId));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable Long userId,
            Authentication authentication) {
        Long resolvedId = resolveCallerUserId(userId, authentication);
        if (resolvedId != null) {
            notificationService.markAllAsRead(resolvedId);
        }
        return ResponseEntity.noContent().build();
    }
}
