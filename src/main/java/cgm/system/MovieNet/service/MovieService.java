package cgm.system.MovieNet.service;

import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.entity.UserMovieDownload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MovieService {

    public Page<Movie> findPaginated(int page, int size);
    public Movie getMovieById(Long movieId);

    public List<Movie> searchMovies(String title, Long genreId, Double rating);
    public Page<Movie> setPage(List<Movie> movies, Pageable pageable);

    Page<Movie> searchByTitle(String title, Pageable pageable);

    Page<Movie> searchByGenre(Long genreId, Pageable pageable);

    public void deleteMovieById(Long movieId);

    public void addMovie(String title, Integer releaseDate, Double imdbRating, String plot,
                         String director, MultipartFile poster, MultipartFile video360,
                         MultipartFile video720, MultipartFile video1080, List<Long> genreIds);

    public  String savePosterFile(MultipartFile file);
    public Page<Movie> searchByRating(Double rating, Pageable pageable);
    public List<Movie> getFavoriteMoviesByUserId(Long userId);
    public Page<Movie> getFavoriteMoviesByUserIdForPage(Long userId, Pageable pageable);
    public List<Movie> findDownloadedMoviesByUser(String username);
    public void save(Movie movie);
    public Movie findById(Long movieId);
    public Page<Movie> getDownloadedMoviesByUserNameForPage(String username, Pageable pageable);
    public List<UserMovieDownload> getDownloadedMovies(User user);


}
