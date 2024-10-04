package cn.atsukoruo.productservice.controller;


import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.productservice.exception.OversellException;
import cn.atsukoruo.productservice.service.ProductService;
import cn.atsukoruo.productservice.utils.Idempotent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/product/add")
    public Response<Object> addProduct(
            @RequestParam("name") String name,
            @RequestParam("price") String price,
            @RequestParam("introduction") String introduction,
            @RequestParam("initAmount") Integer initAmount
    ) {
        productService.addProduct(name, price, introduction, initAmount);
        return Response.success();
    }

    @PostMapping("/product/order")
    // @Idempotent
    public Response<Object> orderProduct(
        @RequestParam("productId") Integer productId,
        @RequestParam("amount") Integer amount,
        @RequestParam(value = "token", required = false) Integer token
    ) {
        Object data = null;
        try {
            int user = getUserFromAuth();
            data = productService.orderProduct(productId, user, amount);
        } catch (OversellException exception) {
            return Response.fail(ErrorCodeConfig.OVERSELL_ERROR,exception.toString());
        }
        if (data == null) {
            return Response.fail();
        }
        return Response.success(data);
    }

    private int getUserFromAuth() {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) context.getAuthentication();
        return Integer.parseInt(user.getName());
    }
}
