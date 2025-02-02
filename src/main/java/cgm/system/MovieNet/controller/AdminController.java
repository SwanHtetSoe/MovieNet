package cgm.system.MovieNet.controller;

import cgm.system.MovieNet.entity.Genre;
import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.entity.Review;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.repository.GenreRepository;
import cgm.system.MovieNet.repository.MovieRepository;
import cgm.system.MovieNet.repository.ReviewRepository;
import cgm.system.MovieNet.repository.UserRepository;
import cgm.system.MovieNet.service.GenreService;
import cgm.system.MovieNet.service.MovieService;
import cgm.system.MovieNet.service.ReviewService;
import cgm.system.MovieNet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {
/*
    @GetMapping("/home")
    public String adminHome() {
        return "/user/userHome";
    }*/


    @Autowired
    private MovieService movieService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private GenreService genreService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private final Path fileStorageLocation;

    public static Long admin_userId;

    public AdminController(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Autowired
    private GenreRepository genreRepository;
    @GetMapping("/home")
    public String userHome(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model){
        Page<Movie> moviePage = movieService.findPaginated(page, size);
        model.addAttribute("adm_moviePage", moviePage);
        List<Genre> genres = genreRepository.findAll();
        model.addAttribute("adm_genres", genres);
        /*Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByName(authentication.getName());
        model.addAttribute("user", user);*/
        if(this.admin_userId == null){
            this.admin_userId  =userRepository.findByName(SecurityContextHolder.getContext().getAuthentication().getName()).getId();
        }
        model.addAttribute("user", userRepository.findById(this.admin_userId )
                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + admin_userId )));

        return "/admin/adminHome";
    }



    @GetMapping("/search")
    public String searchMovie(@RequestParam(value = "title", required = false) String title,
                              @RequestParam(value = "genreId", required = false) Long genreId,
                              @RequestParam(value = "rating", required = false) Double rating,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "20") int size,
                              Model model) {

        PageRequest pageRequest = PageRequest.of(page, size);
        List<Genre> genres = genreRepository.findAll();
        model.addAttribute("adm_genres", genres);

        Page<Movie> moviePage;
        List<Movie> movies;

        if ((title == null || title.isEmpty()) && genreId == null && rating == null) {
            moviePage = movieService.findPaginated(page, size);
        } else if (genreId == null && rating == null) {
            moviePage = movieService.searchByTitle(title, pageRequest);
        } else if (title == null || title.isEmpty() && rating == null) {
            moviePage = movieService.searchByGenre(genreId, pageRequest);
        } else if (title == null || title.isEmpty() && genreId == null) {
            moviePage = movieService.searchByRating(rating, pageRequest);
        } else {
            movies = movieService.searchMovies(title, genreId, rating);
            moviePage = movieService.setPage(movies, pageRequest);
            model.addAttribute("adm_movies_genres", movies);
        }

        model.addAttribute("adm_ttl", title); // Add title to model for Thymeleaf
        model.addAttribute("adm_gId", genreId);
        model.addAttribute("adm_rating", rating);

        model.addAttribute("adm_moviePage", moviePage);
        model.addAttribute("user", userRepository.findById(this.admin_userId )
                .orElseThrow(() -> new IllegalArgumentException("Invalid user id: " + admin_userId )));

        return "/admin/adminHome"; // Thymeleaf template name
    }


    @GetMapping("/movie/{id}")
        public String showMovieDetails(@PathVariable("id") Long id, Model model,
                                       @RequestParam(value = "genreId", required = false) Long genreId,
                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size) {
            Movie movie = movieRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid movie id: " + id));
            List<Review> reviews = reviewRepository.findByMovie(movie);
            model.addAttribute("adm_movie", movie);
            model.addAttribute("adm_reviews", reviews);
            List<Genre> genres = genreRepository.findAll();
            model.addAttribute("adm_genres", genres);
            model.addAttribute("adm_gId", genreId);
            /* model.addAttribute("newReview", new Review());*/
        model.addAttribute("page", page);
        model.addAttribute("size", size);

            return "admin-movie-details"; // Thymeleaf template name
        }



        @PostMapping("/updatePoster/{id}")
        public String updatePoster(@PathVariable("id") Long movieId, @RequestParam("posterFile") MultipartFile posterFile, Model model, RedirectAttributes redirectAttributes) {
            Movie movie = movieService.getMovieById(movieId);
            if (!posterFile.isEmpty()) {
                // Validate file type
                String contentType = posterFile.getContentType();
                if (!isImageFile(contentType)) {
                    redirectAttributes.addFlashAttribute("errorPoster", "Invalid file type. Only JPG and PNG are allowed.");
                    /*model.addAttribute("error", "Invalid file type. Only JPG and PNG are allowed.");*/
                    return "redirect:/admin/movie/" + movieId;
                }

                // Validate file size (limit to 2MB for example)
                if (posterFile.getSize() > 10 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("errorPoster", "File size too large. The maximum allowed size is 2MB.");
                   /* model.addAttribute("error", "File size too large. The maximum allowed size is 2MB.");*/
                    return "redirect:/admin/movie/" + movieId;
                }
                String posterUrl = movieService.savePosterFile(posterFile);
                movie.setPosterUrl(posterUrl);
                movieRepository.save(movie);
                redirectAttributes.addFlashAttribute("successPoster", "Poster updated successfully!");
            }
            return "redirect:/admin/movie/" + movieId;
        }
    private boolean isImageFile(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/png");
    }
    private boolean isVideoFile(String contentType) {
        return contentType.startsWith("video/");
    }

    @PostMapping("/updateMovie/{id}")
    public String updateMovie(@ModelAttribute Movie movie, @PathVariable Long id,Model model,RedirectAttributes redirectAttributes,@RequestParam("genreId") List<Long> genreIds) {
        List<Genre> genres = genreService.findGenresByIds(genreIds);
        Movie existingMovie = movieRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid movie ID:" + id));


        existingMovie.setTitle(movie.getTitle());
        existingMovie.setReleaseDate(movie.getReleaseDate());
        existingMovie.setImdbRating(movie.getImdbRating());
        existingMovie.setDirector(movie.getDirector());
        existingMovie.setPlot(movie.getPlot());
        existingMovie.setGenres(genres);

        movieRepository.save(existingMovie);
        redirectAttributes.addFlashAttribute("successMovie", "Movie updated successfully!");
        model.addAttribute("adm_gId", genreIds);

        return "redirect:/admin/movie/" + id; // Redirect to the updated movie's details page
    }



    @PostMapping("/deleteMovie/{id}")
    public String deleteMovie(@PathVariable("id") Long movieId,RedirectAttributes redirectAttributes) {
        movieService.deleteMovieById(movieId);
        redirectAttributes.addFlashAttribute("success", "Movie deleted successfully!");
        return "redirect:/admin/home";
    }

    @GetMapping("/addMovie")
    public String showAddMovieForm(Model model) {
        List<Genre> genres = genreService.findAllGenres();
        model.addAttribute("genres", genres);
        return "/admin/admin-add-movie"; // make sure this matches your actual HTML file name
    }


    @PostMapping("/addNewMovie")
    public String addMovie(@RequestParam("title") String title,
                           @RequestParam("releaseDate") Integer releaseYear,
                           @RequestParam("imdb_rating") Double imdbRating,
                           @RequestParam("plot") String plot,
                           @RequestParam("director") String director,
                           @RequestParam("poster") MultipartFile poster,
                           @RequestParam("video360") MultipartFile video360,
                           @RequestParam("video720") MultipartFile video720,
                           @RequestParam("video1080") MultipartFile video1080,
                           @RequestParam("genreIds") List<Long> genreIds,
                           RedirectAttributes redirectAttributes) {

        // Validate required fields
        if (title.isEmpty() || plot.isEmpty() || director.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Title, plot, and director are required fields.");
            return "redirect:/admin/addMovie";
        }

        // Validate release year
        if (releaseYear < 1888 || releaseYear > 2100) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid release year.");
            return "redirect:/admin/addMovie";
        }

        // Validate IMDB rating
        if (imdbRating < 0 || imdbRating > 10) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid IMDB rating (0-10).");
            return "redirect:/admin/addMovie";
        }

        // Validate poster file
        if (poster.isEmpty() || !isImageFile(poster.getContentType())) {
            redirectAttributes.addFlashAttribute("error", "Please upload a valid poster file (JPG or PNG).");
            return "redirect:/admin/addMovie";
        }

        // Validate video files
        if (video360.isEmpty() || !isVideoFile(video360.getContentType())) {
            redirectAttributes.addFlashAttribute("error", "Please upload a valid video file for 360p.");
            return "redirect:/admin/addMovie";
        }

        if (video720.isEmpty() || !isVideoFile(video720.getContentType())) {
            redirectAttributes.addFlashAttribute("error", "Please upload a valid video file for 720p.");
            return "redirect:/admin/addMovie";
        }

        if (video1080.isEmpty() || !isVideoFile(video1080.getContentType())) {
            redirectAttributes.addFlashAttribute("error", "Please upload a valid video file for 1080p.");
            return "redirect:/admin/addMovie";
        }

        // Add movie through service
        movieService.addMovie(title, releaseYear, imdbRating, plot, director, poster, video360, video720, video1080, genreIds);

        // Redirect with success message
        redirectAttributes.addFlashAttribute("success", "Movie added successfully!");
        return "redirect:/admin/home";
    }

    @GetMapping("/profile/{id}")
    public String userProfile(@PathVariable("id") Long userId,Model model) {
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", currentUser);
        return "/admin/adminProfile";
    }


    @PostMapping("/profile/{id}")
    public String updateUserProfile(@PathVariable("id") Long userId,
                                    @RequestParam("username") String userName,
                                    @RequestParam("email") String email,
                                    RedirectAttributes redirectAttributes
    ) {

        if (userName == null || userName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Username is required");
            return "redirect:/admin/profile/" + userId;
        }

        if (email == null || email.trim().isEmpty() || !email.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            redirectAttributes.addFlashAttribute("error", "A valid email is required");
            return "redirect:/admin/profile/" + userId;
        }

        userService.updateUserProfile(userId,userName,email);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/admin/profile/" + userId;
    }

    @PostMapping("/profile/changePassword/{id}")
    public String changePassword(@PathVariable("id") Long userId,
                                 @RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 RedirectAttributes redirectAttributes) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorPassword", "Old password is required");
            return "redirect:/admin/profile/" + userId;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorPassword", "New password is required");
            return "redirect:/admin/profile/" + userId;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "New password and confirm new password do not match");
            return "redirect:/admin/profile/" + userId;
        }


        if (!userService.validateOldPassword(userId, oldPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Old password is incorrect");
            return "redirect:/admin/profile/" + userId;
        }
        userService.changePassword(userId,oldPassword, newPassword, confirmNewPassword);
        redirectAttributes.addFlashAttribute("successPassword", "Password changed successfully");
        return "redirect:/admin/profile/" + userId;
    }

    @PostMapping("/changeAvatar/{id}")
    public String updateAvatar(@PathVariable("id") Long movieId, @RequestParam("imageUrl") MultipartFile posterFile, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (!posterFile.isEmpty()) {
            // Validate file type
            String contentType = posterFile.getContentType();
            if (!isImageFile(contentType)) {
                redirectAttributes.addFlashAttribute("errorImage", "Invalid file type. Only JPG and PNG are allowed.");
                /*model.addAttribute("error", "Invalid file type. Only JPG and PNG are allowed.");*/
                return "redirect:/admin/profile/" + movieId;
            }

            // Validate file size (limit to 2MB for example)
            if (posterFile.getSize() > 10 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorImage", "File size too large. The maximum allowed size is 10MB.");
                /* model.addAttribute("error", "File size too large. The maximum allowed size is 2MB.");*/
                return "redirect:/admin/profile/" + movieId;
            }
            String posterUrl = userService.saveProfileImage(posterFile);
            user.setImageUrl(posterUrl);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successImage", "Poster updated successfully!");
        }
        return "redirect:/admin/profile/" + movieId;
    }





}
