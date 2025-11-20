package com.bookinghealthy.repository;

import com.bookinghealthy.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Lấy tất cả bài viết, bài mới nhất lên đầu
    List<Post> findAllByOrderByCreatedAtDesc();

    // (Tùy chọn) Tìm bài viết theo tiêu đề (cho ô search sau này)
    List<Post> findByTitleContainingIgnoreCase(String title);

    // === THÊM HÀM NÀY ===
    // Lấy bài viết theo danh mục (Mới nhất lên đầu)
    List<Post> findByCategoryOrderByCreatedAtDesc(String category);
}