package cgm.system.MovieNet.controller;

import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.entity.UserMovieDownload;
import cgm.system.MovieNet.repository.MovieRepository;
import cgm.system.MovieNet.repository.UserMovieDownloadRepository;
import cgm.system.MovieNet.service.FileStorageService;
import cgm.system.MovieNet.service.MovieService;
import cgm.system.MovieNet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

@Controller
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private UserMovieDownloadRepository userMovieDownloadRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/uploadFile")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        return "redirect:/movie-details";
    }

    @GetMapping("/downloadFile/{movieId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long movieId, @RequestParam String fileUrl, @RequestParam String quality, Principal principal) {
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

        String username = principal.getName();
        User user = userService.getUserByUserName(username);
        Movie movie =movieService.findById(movieId);

        boolean alreadyDownloaded = user.getDownloadedMovies().stream()
                .anyMatch(download -> download.getMovie().getMovieId().equals(movieId));

        if (!alreadyDownloaded) {
            UserMovieDownload userMovieDownload = new UserMovieDownload();
            userMovieDownload.setUser(user);
            userMovieDownload.setMovie(movie);
            userMovieDownload.setDownloaded(true);

            userMovieDownloadRepository.save(userMovieDownload);
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + qualityFileName + "\"")
                .body(resource);
    }
}
