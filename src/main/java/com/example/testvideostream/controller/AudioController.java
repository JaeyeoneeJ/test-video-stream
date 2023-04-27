package com.example.testvideostream.controller;

import com.example.testvideostream.service.AudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.io.IOException;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    @Autowired
    private AudioService service;

    @Autowired
    private Environment environment;

    @GetMapping("/separate/{title}/{mediaType}")
    public String separateAudio(@PathVariable @NotBlank String title, @PathVariable @NotBlank String mediaType) {
        try {
            service.separateAudio(title, mediaType);
            return "음성과 배경음이 성공적으로 분리되었습니다.";
        } catch (IOException e) {
            System.out.println(e);
            return e.toString();
        }
    }
}
