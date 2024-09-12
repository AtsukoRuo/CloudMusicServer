package cn.atsukoruo.societyservice.Service;


import cn.atsukoruo.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class UserProxyService {
    private final RestClient.Builder restClientBuilder;

    public UserProxyService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }
    private final static String AUTHORIZATION_SERVICE = "authorization-service";
    public boolean isInfluencer(int user) {
        Response<?> response =  restClientBuilder.build().get().uri("http://{domain}/influencer?userId={id}",
                AUTHORIZATION_SERVICE, user).accept(MediaType.APPLICATION_JSON)
                .retrieve().body(Response.class);
        return response != null && (Boolean)response.getData();
    }
}
