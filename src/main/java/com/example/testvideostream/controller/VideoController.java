package com.example.testvideostream.controller;

import com.example.testvideostream.model.Videos;
import com.example.testvideostream.service.VideosService;
//import com.example.testvideostream.ui.request.VideosRequest;
//import net.bramp.ffmpeg.FFmpegExecutor;
//import net.bramp.ffmpeg.FFprobe;
//import net.bramp.ffmpeg.builder.FFmpegBuilder;
//import net.bramp.ffmpeg.FFmpeg;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.Optional;
//import javax.servlet.http.HttpServletRequest;
//import javax.validation.constraints.Size;
//import java.io.*;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    @Autowired
    private VideosService service;

    @GetMapping("")
    public Iterable<Videos> getVideoList() {
        return service.selectAll();
    }

    @GetMapping("/{title}")
    public Optional<Videos> getVideo(@PathVariable @NotBlank String title) {
        return service.getVideo(title);
    }

    @GetMapping("/{title}/ko")
    public String getVoiceKo(@PathVariable @NotBlank String title) {
        return service.getVoiceKo(title);
    }

    @GetMapping("/{title}/en")
    public String getVoiceEn(@PathVariable @NotBlank String title) {
        return service.getVoiceEn(title);
    }

    @GetMapping("/{title}/thai")
    public String getVoiceThai(@PathVariable @NotBlank String title) {
        return service.getVoiceThai(title);
    }

    @GetMapping("/{title}/bg")
    public String getBg(@PathVariable @NotBlank String title) {
        return service.getBg(title);
    }


//    @GetMapping(value = "")
//    public ResponseEntity<InputStreamResource> getVideoStream(
//            HttpServletRequest request
//    ) throws IOException {
//        HttpHeaders headers = new HttpHeaders();
//
//        // 사용자 에이전트에 따라 적절한 미디어 타입 선택
//        String userAgent = request.getHeader("User-Agent");
//        String mimeType;
//
//        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("ipod")) {
//            mimeType = "application/x-mpegURL";
//        } else if (userAgent.contains("Android")) {
//            mimeType = "video/mp4";
//        } else if (userAgent.contains("Chrome")) {
//            mimeType = "video/webm";
//        } else {
//            mimeType = "video/mp4";
//        }
//
//        headers.setContentType(MediaType.parseMediaType(mimeType));
//
//        FFmpeg ffmpeg = new FFmpeg("/opt/homebrew/bin/ffmpeg");
//        FFprobe ffprobe = new FFprobe("/opt/homebrew/bin/ffprobe");
//
//
//        // FFmpegBuilder 객체 생성
//        FFmpegBuilder builder = new FFmpegBuilder()
//                // https://www.sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4
//                .setInput("/Users/twin/Downloads/input.mp4")
//                .overrideOutputFiles(true)
//                .addOutput("/Users/twin/Downloads/output.m3u8")
//                .setFormat("hls")
//                .setAudioCodec("aac")
//                .setVideoCodec("libx264")
//                .setPreset("medium")
//                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
//                // .addExtraArgs("-movflags", "frag_keyframe+empty_moov")
//                // .addExtraArgs("-bsf:v", "h264_mp4toannexb")
//                 .addExtraArgs("-hls_time", "10")
//                 .addExtraArgs("-hls_list_size", "0")
//                 .addExtraArgs("-hls_segment_filename", "segment_%03d.ts")
//                .done();
//
//        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
//        executor.createJob(builder).run();
//
//        File videoFile = new File("/Users/twin/Downloads/output.m3u8");
//        InputStream inputStream = new FileInputStream(videoFile);
//
////        byte[] videoBytes = new byte[(int) videoFile.length()];
////        FileInputStream fileInputStream = new FileInputStream(videoFile);
////        fileInputStream.read(videoBytes);
////        fileInputStream.close();
//
////        InputStreamResource resource = new InputStreamResource(new FileInputStream(videoFile));
//
////        return ResponseEntity.ok()
////                .headers(headers)
////                .contentLength(videoFile.length())
//////                .contentType(MediaType.parseMediaType(mimeType))
////                .contentType(MediaType.parseMediaType("video/webm"))
////                .body(new ByteArrayResource(videoBytes));
//        return ResponseEntity.status(HttpStatus.OK)
//                .headers(headers)
//                .body(new InputStreamResource(inputStream));
//    }
}
