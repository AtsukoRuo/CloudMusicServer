package cn.atsukoruo.AuthorizationService.Service;

import cn.atsukoruo.AuthorizationService.Entity.User;
import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import cn.atsukoruo.common.entity.UserInfo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@DS("sharding")
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean isInfluencer(int userId) {
        Boolean result =  userMapper.isInfluencer(userId);
        return result != null && result;
    }

    public User getUserByVxOpenId(String vxOpenId) {
        User user =  userMapper.getUserByVxOpenId(vxOpenId);
        return user;
    }

    public void deleteVxOpenIdField(String vxOpenId) {
        userMapper.deleteOpenIdField(vxOpenId);
    }

    public List<UserInfo> getUsers(List<Integer> users) {
        return userMapper.selectAllUsers(users);
    }

    public void bindVxOpenId(Integer user, String vxOpenId) {
        userMapper.insertVxOpenIdToUser(user, vxOpenId);
    }

    public String getVxOpenIdFromUser(Integer user) {
        return userMapper.getVxOpenIdFromUser(user);
    }
}
