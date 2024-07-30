package cgm.system.MovieNet.repository;

import cgm.system.MovieNet.entity.User;
import cgm.system.MovieNet.entity.UserMovieDownload;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface UserMovieDownloadRepository extends JpaRepository<UserMovieDownload, Long> {

    List<UserMovieDownload> findByUserAndDownloadedTrue(User user);
    Page<UserMovieDownload> findByUserAndDownloadedTrue(User user, Pageable pageable);
}

