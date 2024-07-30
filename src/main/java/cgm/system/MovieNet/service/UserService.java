package cgm.system.MovieNet.service;

import cgm.system.MovieNet.entity.User;
import org.springframework.web.multipart.MultipartFile;


public interface UserService {

    public void saveUser(User user);

    public void updatePassword(User user, String newPassword) ;

    public User getUserByUserName(String name) ;

    public User getCurrentUser();

    public void updateUserProfile(String name,String email);

    public void changePassword(String oldPassword, String newPassword, String confirmNewPassword);

    public boolean validateOldPassword(String userName, String oldPassword);

    public String saveProfileImage(MultipartFile file);

    public void addFavoriteMovie(String username, Long movieId);

    public void removeFavoriteMovie(String username, Long movieId);



}
