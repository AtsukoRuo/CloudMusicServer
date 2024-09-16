package cn.atsukoruo.societyservice.Controller;


import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.societyservice.Entity.Post;
import cn.atsukoruo.societyservice.Service.LikeService;
import cn.atsukoruo.societyservice.Service.PostService;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
public class PostController {
    private final PostService postService;
    private final LikeService likeService;

    public PostController(PostService postService,
                          LikeService likeService) {
        this.postService = postService;
        this.likeService = likeService;
    }

    /**
     * 获取用户的 Feed 流，从 lastTimestamp 开始获取 size 个，
     */
    @GetMapping("feed")
    public Response<List<Post>> feed(long lastTimestamp, int size) {
        int user = getUserFromAuth();
        List<Post> post =  postService.feed(user, lastTimestamp, size);
        return Response.success(post);
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

    @GetMapping("post")
    public Response<Object> retrievePostFromUser(int user, long timestamp, int size) {
        List<Post> data = postService.retrievePost(user, timestamp, size);
        return Response.success(data);
    }

    @PostMapping("post/like")
    public Response<Object> postLike(Integer post) {
        int user = getUserFromAuth();
        likeService.asyncCommitLike(user, post);
        return null;
    }


    @PostMapping("post/comment")
    public Response<Object> postComment() {
        return null;
    }

    private int getUserFromAuth() {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) context.getAuthentication();
        return Integer.parseInt(user.getName());
    }
}
