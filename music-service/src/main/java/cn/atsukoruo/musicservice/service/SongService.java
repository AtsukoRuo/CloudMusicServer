package cn.atsukoruo.musicservice.service;

import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.musicservice.entity.Song;
import cn.atsukoruo.musicservice.repository.SongMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.*;
import com.aliyun.oss.OSS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class SongService {

    private final SongMapper songMapper;
    private final ElasticsearchClient elasticsearchClient;

    private final static String SONG_BUCKET_NAME = "atsukoruo-oss-mp3";
    private final static String IMAGE_BUCKET_NAME = "atsukoruo-oss-image";
    private final static String LRC_BUCKET_NAME = "atsukoruo-oss-lrc";
    private final OSS ossClient;
    public SongService(SongMapper songMapper,
                       OSS ossClient,
                       ElasticsearchClient elasticsearchClient) {
        this.ossClient = ossClient;
        this.songMapper = songMapper;
        this.elasticsearchClient = elasticsearchClient;
    }

    public void uploadSong(String title, String singer, MultipartFile image, MultipartFile lrc, MultipartFile song) throws IOException {
        String filename = title + " - " + singer;
        String mp3Filename = filename + ".mp3";
        String lrcFilename = filename + ".lrc";
        String imageFilename = generateRandomImageFilename(Objects.requireNonNull(image.getOriginalFilename()));
        ossClient.putObject(SONG_BUCKET_NAME, mp3Filename, song.getInputStream());
        ossClient.putObject(LRC_BUCKET_NAME, lrcFilename, lrc.getInputStream());
        ossClient.putObject(IMAGE_BUCKET_NAME, imageFilename, image.getInputStream());

        Song object = Song.builder()
                .songUrl(mp3Filename)
                .imgUrl(imageFilename)
                .lrcUrl(lrcFilename)
                .title(title)
                .singer(singer)
                .build();
        songMapper.insertSong(object);
    }

    private String generateRandomImageFilename(String originFilename) {
        int index = originFilename.lastIndexOf('.');
        String ext = originFilename.substring(index);
        return UUID.randomUUID() + ext;
    }

    public List<Song> search(String title) throws IOException {
        Map<String, FieldSuggester> map = new HashMap<>();
        map.put("title-suggest", FieldSuggester.of(fs ->
                fs.completion(cs -> cs.skipDuplicates(true)
                        .fuzzy(SuggestFuzziness.of(sf->sf.fuzziness("1").transpositions(true)))
                        .field("title"))
        ));

        Suggester suggester = Suggester.of(s -> s
                .suggesters(map)
                .text(title)
        );

        var response = elasticsearchClient.search(s ->
            s.index("song")
                .source(SourceConfig.of(sc ->
                    sc.filter(f ->
                        f.includes(List.of("title", "id", "singer", "songUrl", "imgUrl", "lrcUrl")))))
                .suggest(suggester)
            , Song.class);

        Map<String, List<Suggestion<Song>>> suggestMap =  response.suggest();
        List<Song> songs = new ArrayList<>();

        if (suggestMap == null)
            return songs;

        List<Suggestion<Song>> titleSuggestionList = suggestMap.get("title-suggest");
        if (titleSuggestionList == null)
            return songs;

        for (Suggestion<Song> suggestion : titleSuggestionList) {
            if (suggestion.isCompletion()) {
                CompletionSuggest<Song> completionSuggest = suggestion.completion();
                List<CompletionSuggestOption<Song>> options = completionSuggest.options();
                for (CompletionSuggestOption<Song> option : options) {
                    Object obj = option.source();
                    Song song =  JsonUtils.objectMapper.convertValue(obj, Song.class);
                    songs.add(song);
                }
            }
        }
        return songs;
    }
}
