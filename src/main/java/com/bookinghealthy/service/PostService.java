package com.bookinghealthy.service;

import com.bookinghealthy.model.Post;
import java.util.List;
import java.util.Optional;

public interface PostService {
    List<Post> findAll();
    Optional<Post> findById(Long id);
    Post save(Post post);
    void deleteById(Long id);
}