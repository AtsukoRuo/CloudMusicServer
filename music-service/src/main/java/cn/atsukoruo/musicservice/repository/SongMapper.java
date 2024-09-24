package cn.atsukoruo.musicservice.repository;

import cn.atsukoruo.musicservice.entity.Song;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface SongMapper {

    @Insert("INSERT INTO song(title, singer, song_url, img_url, lrc_url)" +
            " VALUES(#{title}, #{singer}, #{songUrl}, #{imgUrl}, #{lrcUrl})")
    void insertSong(Song song);


    @SelectProvider(type = SongProvider.class, method = "selectSongsByIds")
    List<Song> selectSongsByIds(List<Integer> list);
}
