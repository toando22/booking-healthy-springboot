package com.bookinghealthy.config;

import com.bookinghealthy.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // --- 1. CÁC TRANG CÔNG KHAI (AI CŨNG XEM ĐƯỢC) ---
                        .requestMatchers(
                                "/", "/home",             // Trang chủ
                                "/login", "/register",       // Đăng nhập, Đăng ký
                                "/doctors", "/doctors/**",   // Trang Bác sĩ
                                "/services", "/services/**", // Trang Dịch vụ
                                "/departments", "/department-details/**", // Trang Khoa (Mới)
                                "/contact", "/about",       // Trang Liên hệ, Giới thiệu
                                // === MỞ CỬA CHO CÁC TRANG CHI TIẾT ===
                              //  "/doctors", "/doctors/**",               // Cho phép /doctors VÀ /doctors/1, /doctors/search...
                                "/services", "/service-details/**",      // Cho phép /services VÀ /service-details/1...
                                "/departments", "/department-details/**",// Cho phép /departments VÀ /department-details/1...
                                "/api/doctors"            // API cho AJAX (Mới)
                        ).permitAll()

                        // --- 2. TÀI NGUYÊN TĨNH (CSS, JS, ẢNH) ---
                        .requestMatchers(
                                "/assets/**",
                                "/assets-admin/**",
                                "/uploads/**"
                        ).permitAll()

                        // --- 3. PHÂN QUYỀN ADMIN (BẮT BUỘC ROLE_ADMIN) ---
                        // Sửa: Dùng "/admin/**" để bao gồm TẤT CẢ các trang con
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // --- 4. PHÂN QUYỀN BÁC SĨ (BẮT BUỘC ROLE_DOCTOR) ---
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")

                        // --- 5. CÁC TRANG CẦN ĐĂNG NHẬP (USER, DOCTOR, ADMIN đều được) ---
                        .requestMatchers(
                                "/appointment",
                                "/profile",
                                "/change-password"
                        ).authenticated()

                        // 6. Mọi request CÒN LẠI đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // === NÂNG CẤP: PHÂN LUỒNG ĐĂNG NHẬP ===
                        .successHandler((request, response, authentication) -> {
                            // Nếu là ADMIN, về /admin/dashboard
                            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                                response.sendRedirect("/admin/dashboard");
                            }
                            // Nếu là DOCTOR, về /doctor/dashboard
                            else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
                                response.sendRedirect("/doctor/dashboard");
                            }
                            // Nếu là USER (Bệnh nhân), về trang chủ
                            else {
                                response.sendRedirect("/");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}