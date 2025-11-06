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
                        // --- CÁC TRANG CÔNG KHAI (AI CŨNG XEM ĐƯỢC) ---
                        .requestMatchers(
                                "/", "/home",             // Trang chủ
                                "/login", "/register",       // Đăng nhập, Đăng ký
                                "/doctors", "/doctors/**",   // Trang Bác sĩ
                                "/services", "/services/**", // Trang Dịch vụ
                                "/contact", "/about"       // Trang Liên hệ, Giới thiệu (ví dụ)
                        ).permitAll()

                        // --- TÀI NGUYÊN TĨNH (CSS, JS, ẢNH) ---
                        .requestMatchers(
                                "/assets/**",
                                "/assets-admin/**",
                                "/uploads/**"
                        ).permitAll()

                        // --- CÁC TRANG BẢO VỆ (PHẢI ĐĂNG NHẬP) ---

                        // 1. Phân quyền theo VAI TRÒ (ROLE)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")

                        // 2. Phân quyền theo XÁC THỰC (Đăng nhập là được)
                        // Trang Đặt lịch và Trang Hồ sơ cá nhân
                        .requestMatchers(
                                "/appointment",
                                "/profile",
                                "/change-password"
                        ).authenticated()

                        // 3. Mọi request CÒN LẠI đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // Chuyển về trang chủ SAU KHI đăng nhập thành công
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        // URL để kích hoạt đăng xuất
                        .logoutUrl("/logout")
                        // Chuyển về trang login SAU KHI đăng xuất
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable()); // (Tạm thời tắt CSRF để test)

        return http.build();
    }
}