package com.example.testvideostream.service;

import java.io.IOException;

public interface AudioService {
    void separateAudio(String title, String mediaType) throws IOException;
}
