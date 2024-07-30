package cgm.system.MovieNet.service.impl;

import cgm.system.MovieNet.entity.Genre;
import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.entity.UserMovieDownload;
import cgm.system.MovieNet.repository.MovieRepository;
import cgm.system.MovieNet.repository.ReviewRepository;
import cgm.system.MovieNet.repository.UserMovieDownloadRepository;
import cgm.system.MovieNet.repository.UserRepository;
import cgm.system.MovieNet.service.GenreService;
import cgm.system.MovieNet.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MovieServieImpl implements MovieService {
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private GenreService genreService;

    @Autowired
    private UserRepository userRepository;

    private final Path imageStorageLocation=Path.of("src\\main\\resources\\static\\img\\poster").toAbsolutePath();
    private final Path fileStorageLocation= Path.of("src\\main\\resources\\static\\file\\movie").toAbsolutePath();

    @Autowired
    private UserMovieDownloadRepository userMovieDownloadRepository;

    public List<UserMovieDownload> getDownloadedMovies(User user) {
        return userMovieDownloadRepository.findByUserAndDownloadedTrue(user);
    }

    @Override
    public Page<Movie> findPaginated(int page, int size) {
        return movieRepository.findAllActiveMovies(PageRequest.of(page, size));
    }


    @Override
    public Movie getMovieById(Long movieId) {

        return movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found")); // Handle not found case as per your application logic
    }

    @Override
    public Page<Movie> searchByRating(Double rating, Pageable pageable) {
        return movieRepository.findByImdbRatingAndDeletedFalse(rating, pageable);
    }

    public List<Movie> searchMovies(String title, Long genreId, Double rating) {
        return movieRepository.searchMovies(title, genreId, rating);
    }

    public Page<Movie> setPage(List<Movie> movies, Pageable pageable) {

        int start = (int) pageable.getOffset();
        int end = (start + pageable.getPageSize()) > movies.size() ? movies.size() : (start + pageable.getPageSize());
        Page<Movie> moviePage = new PageImpl<>(movies.subList(start, end), pageable, movies.size());

        return moviePage;
    }

    @Override
    public Page<Movie> searchByTitle(String title, Pageable pageable) {
        return movieRepository.findByTitleContainingIgnoreCaseAndDeletedFalse(title, pageable);
    }

    @Override
    public Page<Movie> searchByGenre(Long genreId, Pageable pageable) {
        return movieRepository.findByGenres_GenreIdAndDeletedFalse(genreId, pageable);
    }

    @Transactional
    public void deleteMovieById(Long movieId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        movie.setDeleted(true);
        movieRepository.save(movie);


    }

    @Override
    public void addMovie(String title, Integer releaseDate, Double imdbRating, String plot,
                         String director, MultipartFile poster, MultipartFile video360,
                         MultipartFile video720, MultipartFile video1080, List<Long> genreIds) {

        // Save poster and video files
        String posterUrl = savePosterFile(poster);
        String videoUrl360 = saveFile(video360);
        String videoUrl720 = saveFile(video720);
        String videoUrl1080 = saveFile(video1080);

        // Create new movie object
        Movie movie = new Movie(title, releaseDate, plot, imdbRating, director, posterUrl, videoUrl360, videoUrl720, videoUrl1080);

        // Add genres to the movie
        List<Genre> genres = genreService.findGenresByIds(genreIds);
        movie.setGenres(genres);

        // Save movie
        movieRepository.save(movie);
    }


    private String saveFile(MultipartFile file) {
        String fileName = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            System.out.println("Target location: " + targetLocation.toString());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File stored successfully at: " + targetLocation.toString());
            return targetLocation.toString();


        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public String savePosterFile(MultipartFile file) {
        String fileName = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png")) {
                throw new RuntimeException("Invalid file type. Only JPG and PNG are allowed.");
            }

            Path targetLocation = this.imageStorageLocation.resolve(fileName);
            System.out.println("Target location: " + targetLocation.toString());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File stored successfully at: " + targetLocation.toString());

            return "/img/poster/" + fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public List<Movie> getFavoriteMoviesByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFavoriteMovies();
    }

    @Override
    public Page<Movie> getFavoriteMoviesByUserIdForPage(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Movie> favoriteMovies = user.getFavoriteMovies();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), favoriteMovies.size());
        return new PageImpl<>(favoriteMovies.subList(start, end), pageable, favoriteMovies.size());
    }



    public void save(Movie movie) {
        movieRepository.save(movie);
    }

    public Movie findById(Long movieId) {
        return movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
    }

    public Page<Movie> getDownloadedMoviesByUserNameForPage(String username, Pageable pageable) {
        User user = userRepository.findByName(username);
        Page<UserMovieDownload> downloads = userMovieDownloadRepository.findByUserAndDownloadedTrue(user, pageable);
        return downloads.map(UserMovieDownload::getMovie);
    }

    public List<Movie> findDownloadedMoviesByUser(String username) {
        User user = userRepository.findByName(username);
        List<UserMovieDownload> downloads = userMovieDownloadRepository.findByUserAndDownloadedTrue(user);
        return downloads.stream()
                .map(UserMovieDownload::getMovie)
                .collect(Collectors.toList());
    }


}
