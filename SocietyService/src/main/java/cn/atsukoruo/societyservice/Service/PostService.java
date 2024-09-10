package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.societyservice.Repository.PostMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private final PostMapper postMapper;
    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public void copyToInbox(int outbox, int inbox) {
        List<Integer> ids = postMapper.getPostIdsOfUser(outbox);
        if (ids.size() != 0) {
            postMapper.insertToInbox(outbox, inbox, ids);
        }
    }

    public void deletePostFromInbox(int userId, int removedUserId) {
        postMapper.deletePostInInbox(userId, removedUserId);
    }
}
