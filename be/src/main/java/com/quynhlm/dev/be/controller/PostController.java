package com.quynhlm.dev.be.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quynhlm.dev.be.core.ResponseObject;
import com.quynhlm.dev.be.model.dto.requestDTO.PostRequestDTO;
import com.quynhlm.dev.be.model.dto.requestDTO.ShareRequestDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.PostMediaDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.PostResponseDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.PostStatisticalDTO;
import com.quynhlm.dev.be.service.PostService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequestMapping("/api/posts")
@RestController
public class PostController {
    @Autowired
    private PostService postService;

    @GetMapping("/statistical")
    public Page<PostMediaDTO> postStatistical(Pageable pageable) {
        return postService.postStatistical(pageable);
    }

    @GetMapping("/user-create/{user_id}")
    public Page<PostResponseDTO> foundPostByUserId(@PathVariable Integer user_id, Pageable pageable) {
        return postService.foundPostByUserId(user_id, pageable);
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject<?>> insertPost(
            @RequestPart("post") String postJson,
            @RequestPart(value = "files") List<MultipartFile> files,
            HttpServletRequest request) {

        ObjectMapper objectMapper = new ObjectMapper();
        PostRequestDTO post = null;
        try {
            post = objectMapper.readValue(postJson, PostRequestDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        PostResponseDTO postResponse = postService.insertPost(post, files);
        ResponseObject<PostResponseDTO> result = new ResponseObject<>();
        result.setMessage("Create a new post successfully");
        result.setStatus(true);
        result.setData(postResponse);
        return new ResponseEntity<ResponseObject<?>>(result, HttpStatus.OK);
    }

    @PostMapping("/share")
    public ResponseEntity<ResponseObject<?>> sharePost(@RequestBody ShareRequestDTO shareRequestDTO) {
        ResponseObject<PostResponseDTO> result = new ResponseObject<>();
        PostResponseDTO response = postService.sharePost(shareRequestDTO);
        result.setMessage("Create a new share successfully");
        result.setStatus(true);
        result.setData(response);
        return new ResponseEntity<ResponseObject<?>>(result, HttpStatus.OK);
    }

    @GetMapping("/search/{userId}")
    public ResponseEntity<Page<PostMediaDTO>> searchPostWithPostId(@PathVariable Integer userId,
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size) {
        Page<PostMediaDTO> posts = postService.searchPostWithContent(userId, keyword, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/hashtag/{userId}")
    public ResponseEntity<Page<PostMediaDTO>> searchPostWithHashtag(@PathVariable Integer userId,
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size) {
        Page<PostMediaDTO> posts = postService.searchPostWithHashtag(keyword, userId, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/friend_posts/{userId}")
    public Page<PostResponseDTO> getAllPostsAndSharedPosts(@PathVariable Integer userId,
            Pageable pageable) {
        return postService.getAllPostsAndSharedPosts(userId, pageable);
    }

    @GetMapping("/all_posts")
    public Page<PostResponseDTO> getAllPosts(
            Pageable pageable) {
        return postService.getAllPosts(pageable);
    }

    @GetMapping("/{post_id}/{user_id}")
    public ResponseEntity<ResponseObject<PostResponseDTO>> getAnPost(@PathVariable Integer post_id,
            @PathVariable Integer user_id) {
        ResponseObject<PostResponseDTO> result = new ResponseObject<>();
        result.setData(postService.getAnPostWithPostId(post_id, user_id));
        result.setMessage("Get an post by " + post_id + " successfully");
        result.setStatus(true);
        return new ResponseEntity<ResponseObject<PostResponseDTO>>(result, HttpStatus.OK);
    }

    @DeleteMapping("/{post_id}")
    public ResponseEntity<ResponseObject<?>> deleteStory(@PathVariable Integer post_id) {
        postService.deletePost(post_id);
        ResponseObject<Void> result = new ResponseObject<>();
        result.setMessage("Delete post successfully");
        result.setStatus(true);
        return new ResponseEntity<ResponseObject<?>>(result, HttpStatus.OK);
    }

    @PutMapping(path = "/restore/{post_id}")
    public ResponseEntity<ResponseObject<?>> restorePost(@PathVariable Integer post_id) throws Exception {
        postService.restorePost(post_id);
        ResponseObject<Void> result = new ResponseObject<>();
        result.setMessage("Restore post successfully");
        result.setStatus(true);
        return new ResponseEntity<ResponseObject<?>>(result, HttpStatus.OK);
    }

    @PutMapping(path = "/{post_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject<?>> updatePost(@PathVariable Integer post_id,
            @RequestPart("post") String postJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        PostRequestDTO post = null;
        try {
            post = objectMapper.readValue(postJson, PostRequestDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        postService.updatePost(post_id, post, files);
        ResponseObject<Void> result = new ResponseObject<>();
        result.setMessage("Update post successfully");
        result.setStatus(true);
        return new ResponseEntity<ResponseObject<?>>(result, HttpStatus.OK);
    }

    @GetMapping("/videos/{user_id}")
    public Page<PostResponseDTO> getAllPostTypeVideo(@PathVariable Integer user_id, Pageable pageable) {
        return postService.getAllPostTypeVideo(user_id, pageable);
    }

    @GetMapping("/hash_tag")
    public List<String> getHashTag() {
        return postService.getAllHashTag();
    }

    @GetMapping("/statistical_post/{year}")
    public List<PostStatisticalDTO> getPostCreateCount(@PathVariable int year) {
        return postService.getPostCreateCount(year);
    }
}