package com.bookinghealthy.service;

import com.bookinghealthy.model.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // <-- THÊM DÒNG NÀY
}