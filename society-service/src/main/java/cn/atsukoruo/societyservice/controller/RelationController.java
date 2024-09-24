package cn.atsukoruo.societyservice.controller;
import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.societyservice.exception.BlacklistError;
import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.societyservice.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
public class RelationController {
    private final RelationService relationService;

    @Autowired
    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }


    @PostMapping("/follow")
    public Response<Object> follow(@RequestParam("favoriteUserId") Integer favoriteUserId) {
        try {
            int followerId = getUserFromAuth();
            relationService.follow(followerId, favoriteUserId);
            return Response.success();
        } catch (BlacklistError e) {
            log.info(e.toString());
            return Response.fail(ErrorCodeConfig.BLACKLIST_ERROR, "由于拉黑，操作被禁止");
        }
    }

    /**
     * 取消关注功能
     */
    @PostMapping("/unfollow")
    public Response<Object> unfollow(@RequestParam("unfollowedUser") Integer unfollowedUser) {
        int userId = getUserFromAuth();
        relationService.unfollow(userId, unfollowedUser);
        return Response.success();
    }

    @PostMapping("/blacklist")
    public Response<Object> blacklist(@RequestParam("blacklistedUser") Integer blacklistedUser) throws Exception {
        int userId = getUserFromAuth();
        relationService.blacklist(userId, blacklistedUser);
        return Response.success();
    }

    @PostMapping("/unblacklist")
    public Response<Object> unblacklist(@RequestParam("blacklistedUser") Integer blacklistedUser) {
        int userId = getUserFromAuth();
        relationService.unblacklist(userId, blacklistedUser);
        return Response.success();
    }


    @GetMapping("/followed/num")
    public Response<Object> getFollowedUserNum(Integer user) {
        int i = 10;
        int result = relationService.getFollowedUserNum(user);
        return Response.success(result);
    }

    @GetMapping("/following/num")
    public Response<Object> getFollowingUserNum(Integer user) {
        int result = relationService.getFollowingUserNum(user);
        return Response.success(result);
    }

    @GetMapping("/followed/user")
    public Response<Object> getFollowedUser(Integer user, Integer from, Integer size) {
        List<Integer> users = relationService.getFollowedUser(user, from, size);
        return Response.success(users);
    }

    @GetMapping("/following/user")
    public Response<Object> getFollowingUser(Integer user, Integer from, Integer size) {
        List<Integer> users = relationService.getFollowingUser(user, from, size);
        return Response.success(users);
    }

    private int getUserFromAuth() {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) context.getAuthentication();
        return Integer.parseInt(user.getName());
    }
}
