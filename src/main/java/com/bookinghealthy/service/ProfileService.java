package com.bookinghealthy.service;

import com.bookinghealthy.dto.ChangePasswordDTO;
import com.bookinghealthy.dto.UpdateProfileDTO;
import com.bookinghealthy.model.User;
import com.bookinghealthy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateProfile(String username, UpdateProfileDTO dto) {
        User user = getProfile(username);
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        return userRepository.save(user);
    }

    public boolean changePassword(String username, ChangePasswordDTO dto) {
        User user = getProfile(username);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        return true;
    }

    public void updateAvatar(String username, String avatarPath) {
        User user = getProfile(username);
        user.setAvatar(avatarPath);
        userRepository.save(user);
    }
}
