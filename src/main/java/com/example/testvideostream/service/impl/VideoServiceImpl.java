package com.example.testvideostream.service.impl;

import com.example.testvideostream.exception.NotFoundException;
import com.example.testvideostream.model.Videos;
import com.example.testvideostream.repository.VideoRepository;
import com.example.testvideostream.service.VideoService;
import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class VideoServiceImpl implements VideoService {
    private VideoRepository repository;

    private Environment environment;

    @Override
    @Transactional(readOnly = true)
    public Iterable<Videos> selectAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Videos> getVideo(String title) {
        return repository.findByTitle(title);
    }

    @Override
    @Transactional(readOnly = true)
    public String getVoiceKo(String title) {
        if (repository.findByTitle(title) == null) {
            throw new NotFoundException("Title " + title + " not found in database");
        } else {
            Videos videos = repository.findVoiceKoByTitle(title);
            String BASE_URL = environment.getProperty("app.base.url");
            String url = BASE_URL +
                    "/" +
                    videos.getTitle() +
                    "/" +
                    videos.getVoiceKo();
            return url;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getVoiceEn(String title) {
        if (repository.findByTitle(title) == null) {
            throw new NotFoundException("Title " + title + " not found in database");
        } else {
            Videos videos = repository.findVoiceEnByTitle(title);
            String BASE_URL = environment.getProperty("app.base.url");
            String url = BASE_URL +
                    "/" +
                    videos.getTitle() +
                    "/" +
                    videos.getVoiceEn();
            return url;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getVoiceThai(String title) {
        if (repository.findByTitle(title) == null) {
            throw new NotFoundException("Title " + title + " not found in database");
        } else {
            Videos videos = repository.findVoiceThaiByTitle(title);
            String BASE_URL = environment.getProperty("app.base.url");
            String url = BASE_URL +
                    "/" +
                    videos.getTitle() +
                    "/" +
                    videos.getVoiceThai();
            return url;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getBgm(String title) {
        if (repository.findByTitle(title) == null) {
            throw new NotFoundException("Title " + title + " not found in database");
        } else {
            Videos videos = repository.findBgmByTitle(title);
            String BASE_URL = environment.getProperty("app.base.url");
            String url = BASE_URL +
                    "/" +
                    videos.getTitle() +
                    "/" +
                    videos.getBgm();
            return url;
        }
    }
}
