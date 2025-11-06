package com.bookinghealthy.repository;

import com.bookinghealthy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Thêm import này
import org.springframework.data.repository.query.Param; // Thêm import này
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * === SỬA LẠI HÀM NÀY ===
     * Thêm @Query để ép Spring JOIN FETCH roles (giải quyết LazyInitializationException)
     * khi BookingController hoặc CustomUserDetailsService gọi hàm này.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * === SỬA LẠI HÀM NÀY ===
     * Thêm @Query để ép Spring JOIN FETCH roles (giải quyết LazyInitializationException)
     * khi BookingController hoặc CustomUserDetailsService gọi hàm này.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    // (Giữ nguyên các hàm cũ)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}