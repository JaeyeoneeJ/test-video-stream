package com.example.testvideostream.ui.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VideosRequest {
    Integer id;

    @NotBlank
    String title;
    @NotBlank
    String video;
    @NotBlank
    String voiceKo;
    @NotBlank
    String voiceEn;
    @NotBlank
    String voiceThai;
    @NotBlank
    String bgm;
}
