package cn.atsukoruo.musicservice.Controller;

import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.musicservice.Entity.Song;
import cn.atsukoruo.musicservice.Service.SongService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
public class SongController {
    private final SongService songService;
    public SongController(SongService songService) {
        this.songService = songService;
    }

    @PostMapping("/song/upload")
    public Response<Object> uploadSong(String title,
        String singer,
        MultipartFile image,
        MultipartFile lrc,
        MultipartFile song
    ) throws IOException {
        songService.uploadSong(title, singer, image, lrc ,song);
        return Response.success();
    }

    @GetMapping("/song/search")
    public Response<Object> searchSong(@RequestParam("title") String title) throws IOException {
        List<Song> songs = songService.search(title);
        return Response.success(songs);
    }
}
