package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.Booking;
import com.bookinghealthy.model.User;
import com.bookinghealthy.repository.BookingRepository;
import com.bookinghealthy.service.ReviewService;
import com.bookinghealthy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/review")
public class UserReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService;

    private User getLoggedInUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/submit")
    public String submitReview(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("rating") Integer rating,
            @RequestParam("comment") String comment,
            Authentication authentication,
            RedirectAttributes ra) {

        try {
            User currentUser = getLoggedInUser(authentication);

            // Check bảo mật: Có đúng là user này sở hữu booking đó không
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null || !booking.getUser().getId().equals(currentUser.getId())) {
                ra.addFlashAttribute("errorMessage", "Lỗi: Bạn không có quyền đánh giá lịch hẹn này.");
                return "redirect:/profile";
            }

            reviewService.saveReview(bookingId, rating, comment);
            ra.addFlashAttribute("successMessage", "Cảm ơn bạn! Đánh giá của bạn đã được ghi nhận.");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/profile";
    }
}