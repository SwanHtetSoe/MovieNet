package cgm.system.MovieNet.controller;

import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.repository.MovieRepository;
import cgm.system.MovieNet.service.FileStorageService;
import cgm.system.MovieNet.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MovieService movieService;

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        return "redirect:/movie-details";
    }

    @GetMapping("/downloadFile/{movieId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long movieId,@RequestParam String fileUrl, @RequestParam String quality) {
        Path path = Paths.get(fileUrl);
        Resource resource;

        try {
            resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + fileUrl);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file: " + fileUrl, e);
        }

        // Append the quality suffix to the filename for the download
        String originalFileName = resource.getFilename();
        String qualityFileName = originalFileName.replace(".mp4", "_" + quality + ".mp4");

        String contentType = "application/octet-stream";
        Movie movie = movieService.findById(movieId);
        movie.setDownloaded(true);
        movieService.save(movie);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + qualityFileName + "\"")
                .body(resource);
    }
}
