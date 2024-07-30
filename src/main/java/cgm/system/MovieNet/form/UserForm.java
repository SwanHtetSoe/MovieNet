package cgm.system.MovieNet.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserForm {
    private Long id;

    @NotNull(message = "Fill the name")
    private String name;

    @NotNull(message = "Fill the email")
    private String email;

    @NotNull(message = "Fill the password")
    private String password;

    private MultipartFile imageFile; // Field for the profile image upload
}
