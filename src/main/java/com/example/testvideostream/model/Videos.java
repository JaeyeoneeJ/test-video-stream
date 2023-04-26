package com.example.testvideostream.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@Entity
public class Videos {
    @Id
    @GeneratedValue
    private Integer id;
    private String title;
    private String Video;
    private String voiceKo;
    private String voiceEn;
    private String voiceThai;
    private String bgm;
}
