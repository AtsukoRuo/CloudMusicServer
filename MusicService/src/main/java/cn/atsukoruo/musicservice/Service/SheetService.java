package cn.atsukoruo.musicservice.Service;

import cn.atsukoruo.musicservice.Entity.Sheet;
import cn.atsukoruo.musicservice.Entity.Song;
import cn.atsukoruo.musicservice.Repository.SheetMapper;
import cn.atsukoruo.musicservice.Repository.SongMapper;
import com.aliyun.oss.OSS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SheetService {
    private final SheetMapper sheetMapper;
    private final SongMapper songMapper;
    private final OSS ossClient;
    private final static String IMAGE_BUCKET_NAME = "atsukoruo-oss-image";

    public SheetService(SheetMapper sheetMapper,
                        SongMapper songMapper,
                        OSS ossClient) {
        this.sheetMapper = sheetMapper;
        this.ossClient = ossClient;
        this.songMapper =  songMapper;
    }

    @Transactional
    public void createSheet(String title, MultipartFile image, int user) throws IOException {
        String filename = generateRandomImageFilename(Objects.requireNonNull(image.getOriginalFilename()));
        ossClient.putObject(IMAGE_BUCKET_NAME, filename, image.getInputStream());
        Sheet sheet = Sheet.builder()
                .imgUrl(filename)
                .title(title)
                .userId(user)
                .createTime(new Timestamp(System.currentTimeMillis()))
                .build();
        sheetMapper.createSheet(sheet);
        sheetMapper.insertUserSheetRelation(user, sheet.getId());
    }


    @Transactional
    public void deleteSheet(int user, int sheetId) {
        sheetMapper.deleteSheet(sheetId);
        sheetMapper.deleteUserSheetRelation(user, sheetId);
        sheetMapper.deleteAllSongSheetRelationBySheetId(sheetId);
        sheetMapper.deleteAllCollectionSheetRelationBySheetId(sheetId);
    }



    public void addSongToSheet(Integer sheetId, Integer songId) {
        sheetMapper.insertSongSheetRelation(sheetId, songId);
    }

    public void deleteSongInSheet(Integer sheetId, Integer songId) {
        sheetMapper.deleteSongSheetRelation(sheetId, songId);
    }

    public List<Sheet> getMySheet(int user) {
        return sheetMapper.selectMySheet(user);
    }

    public List<Song> getSongsFromSheet(Integer sheetId) {
        List<Integer> list = sheetMapper.selectSongIdFromSheet(sheetId);
        return songMapper.selectSongsByIds(list);
    }

    public void addSheetToCollection(Integer sheetId, int user) {
        sheetMapper.insertCollectionSheetRelation(sheetId, user);
    }

    public void deleteSheetCollection(Integer sheetId, int user) {
        sheetMapper.deleteCollectionSheetRelation(sheetId, user);
    }

    public List<Sheet> getSheetCollection(int user) {
        List<Integer> list = sheetMapper.selectSheetIdFromCollection(user);
        return sheetMapper.selectSheetsByIds(list);
    }

    private String generateRandomImageFilename(String originFilename) {
        int index = originFilename.lastIndexOf('.');
        String ext = originFilename.substring(index);
        return UUID.randomUUID() + ext;
    }
}
