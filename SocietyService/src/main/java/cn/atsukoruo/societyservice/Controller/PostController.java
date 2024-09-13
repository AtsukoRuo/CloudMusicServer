package cn.atsukoruo.societyservice.Controller;


import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.societyservice.Service.PostService;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("feed")
    public String feed(Authentication authentication) {
        return "OK";
    }

    @PostMapping("post")
    public Response<Object> publishPost(int user, String content, MultipartFile image)
            throws IOException {
        postService.publishPost(user, content, image);
        return Response.success("ok");
    }

    @DeleteMapping("post")
    public Response<Object> deletePost(int user, int post) {
        postService.deletePostSoftlyInOutbox(user, post);
        return Response.success("ok");
    }

    @PostMapping("post/like")
    public Response<Object> postLike() {
        return null;
    }

    @PostMapping("post/comment")
    public Response<Object> postComment() {
        return null;
    }
}
