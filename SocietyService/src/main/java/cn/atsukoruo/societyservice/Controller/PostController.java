package cn.atsukoruo.societyservice.Controller;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class PostController {

    public String feed(Authentication authentication) {
        return null;
    }

}
