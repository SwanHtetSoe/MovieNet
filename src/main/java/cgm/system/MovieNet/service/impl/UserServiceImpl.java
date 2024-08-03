package cgm.system.MovieNet.service.impl;

import cgm.system.MovieNet.entity.Movie;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.form.MovieNotFoundException;
import cgm.system.MovieNet.repository.MovieRepository;
import cgm.system.MovieNet.repository.UserRepository;
import cgm.system.MovieNet.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MovieRepository movieRepository;

    private final Path imageStorageLocation=Path.of("src\\main\\resources\\static\\img\\profile").toAbsolutePath();

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        System.out.println(newPassword);
    }

    public User getUserByUserName(String name) {
        return userRepository.findByName(name);
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username = null;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        User user = userRepository.findByName(username);
        if (user == null) {
            System.out.println("User not found for username: " + username);
        }
        return user;
    }

    public void updateUserProfile(Long userId,String name,String email) {
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        currentUser.setEmail(email);
        currentUser.setName(name);

        userRepository.save(currentUser);
    }

    public void changePassword(Long userId,String oldPassword, String newPassword, String confirmNewPassword) {
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (passwordEncoder.matches(oldPassword, currentUser.getPassword()) && newPassword.equals(confirmNewPassword)) {
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(currentUser);
        }
    }

    public boolean validateOldPassword(Long userId, String oldPassword) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return passwordEncoder.matches(oldPassword, user.getPassword());
    }

    public String saveProfileImage(MultipartFile file) {
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

            return "/img/profile/" + fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public void addFavoriteMovie(String username, Long movieId) {
        User user = userRepository.findByName(username);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found"));
        user.getFavoriteMovies().add(movie);
        userRepository.save(user);
    }

    @Override
    public void removeFavoriteMovie(String username, Long movieId) {
        User user = userRepository.findByName(username);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found"));
        user.getFavoriteMovies().remove(movie);
        userRepository.save(user);
    }


}
