package com.bookinghealthy.service.impl;

import com.bookinghealthy.model.Post;
import com.bookinghealthy.repository.PostRepository;
import com.bookinghealthy.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;

    @Override
    public List<Post> findAll() {
        // Lấy danh sách bài viết (mới nhất lên đầu)
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    @Override
    public Post save(Post post) {
        return postRepository.save(post);
    }

    @Override
    public void deleteById(Long id) {
        postRepository.deleteById(id);
    }
}