package cgm.system.MovieNet.controller;

import cgm.system.MovieNet.entity.Genre;
import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.entity.Review;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.repository.GenreRepository;
import cgm.system.MovieNet.repository.MovieRepository;
import cgm.system.MovieNet.repository.ReviewRepository;
import cgm.system.MovieNet.repository.UserRepository;
import cgm.system.MovieNet.service.MovieService;
import cgm.system.MovieNet.service.ReviewService;
import cgm.system.MovieNet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/home")
    public String userHome(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model){
        Page<Movie> moviePage = movieService.findPaginated(page, size);
        model.addAttribute("moviePage", moviePage);
        List<Genre> genres = genreRepository.findAll();
        model.addAttribute("genres", genres);

        return "/user/userHome";
    }


    @GetMapping("/movie/{id}")
    public String showMovieDetails(@PathVariable("id") Long id,@RequestParam(value = "page", defaultValue = "0") int page, @RequestParam(value = "size", defaultValue = "20") int size, Model model, Principal principal) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid movie id: " + id));
        List<Review> reviews = reviewRepository.findByMovie(movie);
        model.addAttribute("movie", movie);
        model.addAttribute("reviews", reviews);
        model.addAttribute("newReview", new Review());
        model.addAttribute("page", page);
        model.addAttribute("size", size);

        User user = userRepository.findByName(principal.getName());
        boolean isFavorite = user.getFavoriteMovies().contains(movie);
        model.addAttribute("isFavorite", isFavorite);

        return "movie-details"; // Thymeleaf template name
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
        model.addAttribute("genres", genres);

        Page<Movie> moviePage;
        List<Movie> movies;

        if ((title == null || title.isEmpty()) && genreId == null && rating == null) {
            moviePage = movieService.findPaginated(page, size);
        } else if (genreId == null && rating == null) {
            moviePage = movieService.searchByTitle(title, pageRequest);
        } else if ((title == null || title.isEmpty()) && rating == null) {
            moviePage = movieService.searchByGenre(genreId, pageRequest);
        } else if ((title == null || title.isEmpty()) && genreId == null) {
            moviePage = movieService.searchByRating(rating, pageRequest);
        } else {
            movies = movieService.searchMovies(title, genreId, rating);
            moviePage = movieService.setPage(movies, pageRequest);
            model.addAttribute("movies_genres", movies);
        }

        model.addAttribute("ttl", title);
        model.addAttribute("gId", genreId);
        model.addAttribute("rating", rating);

        model.addAttribute("moviePage", moviePage);

        return "/user/userHome"; // Thymeleaf template name
    }




    @PostMapping("/reviews/{id}")
    public String addReview(@ModelAttribute("newReview") Review newReview,@PathVariable("id") Long id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Movie movieB = movieService.getMovieById(id);
        newReview.setMovie(movieB);

        if (movieB == null || newReview == null || newReview.getMovie() == null ) {
            // Handle the case where movie is not properly set
            // Redirect or return an error message
            return "redirect:/user/home"; // Example redirection
        }
        newReview.setReviewDate(LocalDate.now());

        // Save the review with username
        reviewService.saveReview(newReview,username);
        return "redirect:/user/movie/" + id;
    }

    @GetMapping("/profile")
    public String userProfile(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("user", currentUser);
        return "/user/userProfile";
    }

    @PostMapping("/profile")
    public String updateUserProfile(@RequestParam("username") String userName,
                                    @RequestParam("email") String email,
                                    RedirectAttributes redirectAttributes
                                   ) {

        if (userName == null || userName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Username is required");
            return "redirect:/user/profile";
        }

        if (email == null || email.trim().isEmpty() || !email.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            redirectAttributes.addFlashAttribute("error", "A valid email is required");
            return "redirect:/user/profile";
        }

        userService.updateUserProfile(userName,email);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/user/profile";
    }

    @PostMapping("/profile/changePassword")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 RedirectAttributes redirectAttributes) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorPassword", "Old password is required");
            return "redirect:/user/profile";
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorPassword", "New password is required");
            return "redirect:/user/profile";
        }

        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "New password and confirm new password do not match");
            return "redirect:/user/profile";
        }

        String username = userService.getCurrentUser().getName();
        if (!userService.validateOldPassword(username, oldPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Old password is incorrect");
            return "redirect:/user/profile";
        }
        userService.changePassword(oldPassword, newPassword, confirmNewPassword);
        redirectAttributes.addFlashAttribute("successPassword", "Password changed successfully");
        return "redirect:/user/profile";
    }

    @PostMapping("/changeAvatar/{id}")
    public String updatePoster(@PathVariable("id") Long movieId, @RequestParam("imageUrl") MultipartFile posterFile, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (!posterFile.isEmpty()) {
            // Validate file type
            String contentType = posterFile.getContentType();
            if (!isImageFile(contentType)) {
                redirectAttributes.addFlashAttribute("errorImage", "Invalid file type. Only JPG and PNG are allowed.");
                /*model.addAttribute("error", "Invalid file type. Only JPG and PNG are allowed.");*/
                return "redirect:/user/profile";
            }

            // Validate file size (limit to 2MB for example)
            if (posterFile.getSize() > 10 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorImage", "File size too large. The maximum allowed size is 10MB.");
                /* model.addAttribute("error", "File size too large. The maximum allowed size is 2MB.");*/
                return "redirect:/user/profile";
            }
            String posterUrl = userService.saveProfileImage(posterFile);
            user.setImageUrl(posterUrl);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successImage", "Poster updated successfully!");
        }
        return "redirect:/user/profile";
    }

    private boolean isImageFile(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/png");
    }

    @PostMapping("/favorites/add/{movieId}")
    public String addFavorite(@PathVariable Long movieId, Principal principal) {
        String username = principal.getName();
        userService.addFavoriteMovie(username, movieId);
        return "redirect:/user/movie/" + movieId;
    }

    @PostMapping("/favorites/remove/{movieId}")
    public String removeFavorite(@PathVariable Long movieId, Principal principal) {
        String username = principal.getName();
        userService.removeFavoriteMovie(username, movieId);
        return "redirect:/user/movie/" + movieId;
    }



    @GetMapping("/favorites/{id}")
    public String getUserFavorites(@PathVariable("id") Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size,Model model,Principal principal) {
        String username = principal.getName();
        User user = userService.getUserByUserName(username);
        model.addAttribute("user", user);

        Page<Movie> favoriteMoviesPage = movieService.getFavoriteMoviesByUserIdForPage(userId, PageRequest.of(page, size));
        model.addAttribute("moviePage", favoriteMoviesPage);

        List<Movie> favoriteMovies = movieService.getFavoriteMoviesByUserId(userId);
        model.addAttribute("favoriteMovies", favoriteMovies);

        return "user/userFavorite";
    }

    @GetMapping("/downloaded/{id}")
    public String getDownloaded(@PathVariable("id") Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "3") int size,Model model,Principal principal) {
        String username = principal.getName();
        User user = userService.getUserByUserName(username);
        model.addAttribute("user", user);

        Page<Movie> downloadedMoviesPage = movieService.getDownloadedMoviesByUserNameForPage(username, PageRequest.of(page, size));
        model.addAttribute("moviePage", downloadedMoviesPage);

        List<Movie> downloadedMovies = movieService.findDownloadedMoviesByUser(username);
        model.addAttribute("downloadedMovies", downloadedMovies);


        return "user/userDownload";

    }

}
