package com.example.testvideostream.service;

import com.example.testvideostream.model.Videos;

import java.util.Optional;

public interface VideosService {
    Iterable<Videos> selectAll();
    Optional<Videos> getVideo(String title);
    String getVoiceKo(String title);
    String getVoiceEn(String title);
    String getVoiceThai(String title);
    String getBg(String title);

}
