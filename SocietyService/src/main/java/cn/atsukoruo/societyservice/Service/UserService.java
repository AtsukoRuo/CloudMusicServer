package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.societyservice.Repository.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper influencerMapper;

    public UserService(UserMapper influencerMapper) {
        this.influencerMapper = influencerMapper;
    }

    public Boolean isInfluencer(int userId) {
        return influencerMapper.isInfluencer(userId);
    }
}
