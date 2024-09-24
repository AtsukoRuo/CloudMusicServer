package cn.atsukoruo.musicservice.repository;

import cn.atsukoruo.musicservice.entity.Sheet;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface SheetMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO sheet(user_id, img_url, create_time, title)" +
            " VALUES(#{userId}, #{imgUrl}, #{createTime}, #{title})")
    void createSheet(Sheet sheet);


    @Insert("INSERT INTO user_sheet(user_id, sheet_id)" +
            " VALUES(#{user}, #{id})")
    void insertUserSheetRelation(int user, int id);

    @Delete("DELETE FROM user_sheet WHERE `user_id`=#{user} AND `sheet_id`=#{sheetId}")
    void deleteUserSheetRelation(int user, int sheetId);

    @Delete("DELETE FROM sheet_song WHERE sheet_id=#{sheetId}")
    void deleteAllSongSheetRelationBySheetId(int sheetId);

    @Delete("DELETE FROM sheet_song WHERE sheet_id=#{sheetId} AND song_id=#{songId}")
    void deleteSongSheetRelation(int sheetId, int songId);

    @Delete("DELETE FROM sheet_collection WHERE sheet_id=#{sheetId}")
    void deleteAllCollectionSheetRelationBySheetId(int sheetId);

    @Delete("DELETE FROM sheet WHERE id=#{sheetId}")
    void deleteSheet(int sheetId);

    @Insert("INSERT INTO sheet_song(sheet_id, song_id) VALUES(#{sheetId}, #{songId})")
    void insertSongSheetRelation(Integer sheetId, Integer songId);

    @Select("SELECT * FROM sheet WHERE user_id=#{user}")
    List<Sheet> selectMySheet(int user);


    @Select("SELECT song_id FROM sheet_song WHERE sheet_id=#{sheetId}")
    List<Integer> selectSongIdFromSheet(Integer sheetId);

    @Select("SELECT sheet_id FROM sheet_collection WHERE user_id=#{user}")
    List<Integer> selectSheetIdFromCollection(int user);

    @SelectProvider(type = SheetProvider.class, method = "selectSheetsByIds")
    List<Sheet> selectSheetsByIds(List<Integer> list);

    @Insert("INSERT INTO sheet_collection(sheet_id, user_id) VALUES(#{sheetId}, #{user})")
    void insertCollectionSheetRelation(Integer sheetId, int user);

    @Delete("DELETE FROM sheet_collection WHERE user_id=#{user} AND sheet_id=#{sheetId}")
    void deleteCollectionSheetRelation(Integer sheetId, int user);
}
