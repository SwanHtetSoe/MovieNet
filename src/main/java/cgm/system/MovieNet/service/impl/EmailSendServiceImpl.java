package cgm.system.MovieNet.service.impl;

import cgm.system.MovieNet.entity.PasswordReset;
import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.repository.PasswordResetRepository;
import cgm.system.MovieNet.service.EmailSendService;

import cgm.system.MovieNet.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailSendServiceImpl implements EmailSendService {

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    PasswordResetRepository repository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Override
    public void sendMail(User user, String email) {
        String token = UUID.randomUUID().toString();
       /* PasswordReset reset = new PasswordReset();
        reset.setEmail(email);
        reset.setToken(token);
        reset.setUser(user);
        repository.save(reset);*/
        passwordResetService.createPasswordResetTokenForUser(user, token);
        SimpleMailMessage message = getMessage(user,email, token);
        mailSender.send(message);
    }

    private SimpleMailMessage getMessage(User user,String email, String token){
        SimpleMailMessage message = new SimpleMailMessage();
        String subject="Password Reset";
        /*String Url = "http://localhost:9090/reset_password?token=" + token;
        String body= "This is password Reset Link \n" +
                "Click Below Link to reset your password \n" +
                Url;*/

        // Create the password reset URL
        String resetUrl = "http://localhost:9090/reset_password?token=" + token;

        // Create the email message content
        String body = "Hello " + user.getName() + ",\n\n" +
                "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                resetUrl + "\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Thank you";

        message.setFrom("swanhtetsoe36@gmail.com");
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        return message;
    }

}
