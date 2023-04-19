package com.example.testvideostream.ui.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideosResponse {
    Integer id;
    String title;
    String video;
    String voiceKr;
    String voiceEn;
    String voiceThai;
    String bg;
}
