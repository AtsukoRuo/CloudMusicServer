package cn.atsukoruo.societyservice.Controller;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class PostController {


    @GetMapping("feed")
    public String feed(Authentication authentication) {
        return "OK";
    }

}
