package cn.atsukoruo.AuthorizationService.Service;

import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import cn.atsukoruo.common.entity.UserInfo;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean isInfluencer(int userId) {
        Boolean result =  userMapper.isInfluencer(userId);
        return result != null && result;
    }


    public List<UserInfo> getUsers(List<Integer> users) {
        return userMapper.selectAllUsers(users);
    }
}