package com.example.testvideostream.repository;

import com.example.testvideostream.model.Videos;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends CrudRepository<Videos, Integer> {
    @Query("SELECT * FROM videos WHERE title LIKE '%:title%'")
    Optional<Videos> findByTitle(@Param("title") String title);

    @Query("SELECT voiceKo FROM videos WHERE title LIKE '%:title%'")
    Videos findVoiceKoByTitle(@Param("title") String title);
    @Query("SELECT voiceEn FROM videos WHERE title LIKE '%:title%'")
    Videos findVoiceEnByTitle(@Param("title") String title);
    @Query("SELECT voiceThai FROM videos WHERE title LIKE '%:title%'")
    Videos findVoiceThaiByTitle(@Param("title") String title);
    @Query("SELECT bgm FROM videos WHERE title LIKE %:title%")
    Videos findBgmByTitle(@Param("title") String title);
}
