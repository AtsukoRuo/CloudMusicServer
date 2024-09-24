package cn.atsukoruo.musicservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration.class)
@MapperScan
public class MusicServiceApplication {
    // 上传歌曲
    // 歌曲搜索

    // TODO 创建歌单
    // TODO 删除歌单
    // TODO 添加歌曲
    // TODO 删除歌曲
    // TODO 查看歌单内容
    // TODO 获取推荐歌单
    // TODO 获取推荐歌曲
    // TODO 收藏歌单
    public static void main(String[] args) {
        SpringApplication.run(MusicServiceApplication.class, args);
    }
}
