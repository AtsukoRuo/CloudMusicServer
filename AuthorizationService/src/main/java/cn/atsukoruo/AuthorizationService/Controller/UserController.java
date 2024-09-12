package cn.atsukoruo.AuthorizationService.Controller;


import cn.atsukoruo.AuthorizationService.Service.UserService;
import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.common.utils.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
final public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("influencer")
    public Response<Object> isInfluencer(Integer userId) {
        boolean result =  userService.isInfluencer(userId);
        return Response.success(result);
    }

    @GetMapping(value = "user", produces = MediaType.APPLICATION_JSON)
    public Response<Object> getUser(@RequestBody String body) {
        List<Integer> users = JsonUtils.parseObject(body, List.class);
        return Response.success(userService.getUsers(users));
    }
}
