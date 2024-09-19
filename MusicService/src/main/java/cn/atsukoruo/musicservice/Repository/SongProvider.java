package cn.atsukoruo.musicservice.Repository;
import org.apache.ibatis.jdbc.SQL;

import java.util.List;

public class SongProvider {
    public String selectSongsByIds(List<Integer> list) {
        SQL sql = new SQL();
        sql.SELECT("*").FROM("song");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" WHERE id IN (");
        for (var id : list) {
            stringBuilder.append(id + ",");
        }
        return sql + stringBuilder.substring(0, stringBuilder.length() - 1) + ")";
    }
}
