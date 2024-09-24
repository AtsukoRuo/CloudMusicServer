package cn.atsukoruo.musicservice.controller;

import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.musicservice.entity.Sheet;
import cn.atsukoruo.musicservice.entity.Song;
import cn.atsukoruo.musicservice.service.SheetService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class SheetController {
    private final SheetService sheetService;
    public SheetController(SheetService sheetService) {
        this.sheetService = sheetService;
    }

    @PostMapping("/sheet")
    public Response<Object> createSheet(String title,
                                        MultipartFile image) throws IOException {
        int user =  getUserFromAuth();
        sheetService.createSheet(title, image, user);
        return Response.success();
    }


    @DeleteMapping("/sheet/{sheetId}")
    public Response<Object> deleteSheet(@PathVariable("sheetId") Integer sheetId) {
        int user = getUserFromAuth();
        sheetService.deleteSheet(user, sheetId);
        return Response.success();
    }

    @GetMapping("/sheet/{sheetId}")
    public Response<Object> getSongsFromSheet(@PathVariable("sheetId") Integer sheetId) {
        List<Song> songs = sheetService.getSongsFromSheet(sheetId);
        return Response.success(songs);
    }

    @GetMapping("/sheet/me")
    public Response<Object> getMySheet() {
        int user = getUserFromAuth();
        List<Sheet> sheets =  sheetService.getMySheet(user);
        return Response.success(sheets);
    }


    @PostMapping("/sheet/{sheetId}/{songId}")
    public Response<Object> addSongToSheet(
            @PathVariable("sheetId") Integer sheetId,
            @PathVariable("songId") Integer songId
    ) {
        sheetService.addSongToSheet(sheetId, songId);
        return Response.success();
    }

    @DeleteMapping("/sheet/{sheetId}/{songId}")
    public Response<Object> deleteSongInSheet(
            @PathVariable("sheetId") Integer sheetId,
            @PathVariable("songId") Integer songId
    ) {
        sheetService.deleteSongInSheet(sheetId, songId);
        return Response.success();
    }


    @GetMapping("/sheet/collection")
    public Response<Object> getSheetCollection() {
        int user = getUserFromAuth();
        List<Sheet> sheets =  sheetService.getSheetCollection(user);
        return Response.success(sheets);
    }

    @PostMapping("/sheet/collection/{sheetId}")
    public Response<Object> addSheetToCollection(
            @PathVariable("sheetId") Integer sheetId
    ) {
        int user = getUserFromAuth();
        sheetService.addSheetToCollection(sheetId, user);
        return Response.success();
    }

    @DeleteMapping("/sheet/collection/{sheetId}")
    public Response<Object> deleteSheetCollection(
            @PathVariable("sheetId") Integer sheetId
    ) {
        int user = getUserFromAuth();
        sheetService.deleteSheetCollection(sheetId, user);
        return Response.success();
    }


    private int getUserFromAuth() {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) context.getAuthentication();
        return Integer.parseInt(user.getName());
    }
}
