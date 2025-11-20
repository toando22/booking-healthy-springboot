package com.bookinghealthy.controller.user;

import com.bookinghealthy.model.Post;
import com.bookinghealthy.repository.PostRepository;
import com.bookinghealthy.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class NewsController {

    @Autowired
    private PostService postService;
    @Autowired
    PostRepository postRepository;

    // 1. HIỂN THỊ DANH SÁCH TIN TỨC
    @GetMapping("/news")
    public String showNewsList(Model model) {
        model.addAttribute("posts", postService.findAll());
        model.addAttribute("activePage", "news"); // Để highlight menu nếu cần
        return "user/news"; // -> Trỏ tới file templates/user/news.html
    }
    // 2. TRANG Y HỌC THƯỜNG THỨC (Chỉ hiện bài KNOWLEDGE)
    @GetMapping("/knowledge")
    public String showKnowledgeList(Model model) {
        // Lọc bài viết có category là 'KNOWLEDGE'
        model.addAttribute("posts", postRepository.findByCategoryOrderByCreatedAtDesc("KNOWLEDGE"));
        model.addAttribute("pageTitle", "Y học thường thức"); // Tiêu đề trang
        model.addAttribute("activePage", "news");
        return "user/news"; // Dùng chung giao diện news.html
    }
    // 2. HIỂN THỊ CHI TIẾT BÀI VIẾT
    @GetMapping("/news/{id}")
    public String showNewsDetails(@PathVariable("id") Long id, Model model) {
        Optional<Post> post = postService.findById(id);
        if (post.isPresent()) {
            model.addAttribute("post", post.get());
            model.addAttribute("activePage", "news");
            return "user/news-details"; // -> Trỏ tới file templates/user/news-details.html
        } else {
            return "redirect:/news";
        }
    }
}