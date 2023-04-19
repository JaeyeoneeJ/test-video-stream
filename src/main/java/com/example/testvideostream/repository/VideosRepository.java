package com.example.testvideostream.repository;

import com.example.testvideostream.model.Videos;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideosRepository extends CrudRepository<Videos, Integer> {
    @Query("SELECT * FROM videos WHERE title LIKE '%:title%'")
    Optional<Videos> findByTitle(@Param("title") String title);

    @Query("SELECT voiceKr FROM videos WHERE title LIKE '%:title%'")
    Videos findVoiceKrByTitle(@Param("title") String title);
    @Query("SELECT voiceEn FROM videos WHERE title LIKE '%:title%'")
    Videos findVoiceEnByTitle(@Param("title") String title);
    @Query("SELECT voiceThai FROM videos WHERE title LIKE '%:title%'")
    Videos findVoiceThaiByTitle(@Param("title") String title);
    @Query("SELECT bg FROM videos WHERE title LIKE %:title%")
    Videos findBgByTitle(@Param("title") String title);
}
