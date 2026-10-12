package com.doctorappointment.service;

import com.doctorappointment.entity.Doctor;
import com.doctorappointment.entity.Notification;
import com.doctorappointment.entity.Patient;
import com.doctorappointment.entity.User;
import com.doctorappointment.repository.DoctorRepository;
import com.doctorappointment.repository.NotificationRepository;
import com.doctorappointment.repository.PatientRepository;
import com.doctorappointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Notification createNotification(User user, String message, String type) {
        if (user == null) return null;
        Notification notification = new Notification(user, message, type != null ? type : "APPOINTMENT");
        return notificationRepository.save(notification);
    }

    public Long resolveUserId(Long idOrEntityId) {
        if (idOrEntityId == null) return null;
        if (userRepository.existsById(idOrEntityId)) {
            return idOrEntityId;
        }
        Optional<Patient> p = patientRepository.findById(idOrEntityId);
        if (p.isPresent()) {
            Optional<User> u = userRepository.findByEmail(p.get().getEmail());
            if (u.isPresent()) return u.get().getId();
        }
        Optional<Doctor> d = doctorRepository.findById(idOrEntityId);
        if (d.isPresent()) {
            Optional<User> u = userRepository.findByEmail(d.get().getEmail());
            if (u.isPresent()) return u.get().getId();
        }
        return idOrEntityId;
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        Long resolved = resolveUserId(userId);
        if (resolved == null) return Collections.emptyList();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(resolved);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        Long resolved = resolveUserId(userId);
        if (resolved == null) return 0;
        return notificationRepository.countByUserIdAndReadFalse(resolved);
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Long resolved = resolveUserId(userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (resolved != null && !notification.getUser().getId().equals(resolved)) {
            throw new SecurityException("Unauthorized access to notification");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        Long resolved = resolveUserId(userId);
        if (resolved == null) return;
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(resolved);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
