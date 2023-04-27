package com.example.testvideostream.controller;

import com.example.testvideostream.model.Videos;
import com.example.testvideostream.service.VideoService;

import com.example.testvideostream.utils.MediaUtils;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.io.*;
import java.util.Optional;
import java.util.UUID;

import static com.example.testvideostream.utils.FileUtils.checkFilePath;


@RestController
@RequestMapping("/api/video")
public class VideoController {
    @Autowired
    private VideoService videoService;

    @Autowired
    private Environment environment;

    MediaUtils mediaUtils = new MediaUtils();

    @GetMapping("/createStream/{title}/language/{language}")
    public ResponseEntity<String> speechSynthesisOnLanguage (
            @PathVariable @NotBlank String title,
            @PathVariable @NotBlank String language
    ) throws Exception {
        // background music 다운로드 및 경로 저장
        mediaUtils.init(videoService, environment);
        String bgmLocalPath = mediaUtils.getMediaDownloadPathOnLocal(title, "bgm");
        String languageLocalPath = mediaUtils.getMediaDownloadPathOnLocal(title, language);

        // ffmpeg, ffprobe 선언
        String localSrc = environment.getProperty("app.local.src");
        FFmpeg ffmpeg = new FFmpeg(localSrc + "ffmpeg");
        FFprobe ffprobe = new FFprobe(localSrc + "ffprobe");

        // 두 audio 파일의 재생 길이 확인
        FFmpegProbeResult probeResultBgm = ffprobe.probe(bgmLocalPath);
        FFmpegProbeResult probeResultLanguage = ffprobe.probe(languageLocalPath);

        double durationBgm = probeResultBgm.getFormat().duration;
        double durationLanguage = probeResultLanguage.getFormat().duration;

        // 두 audio 파일 중 재생 길이가 더 긴 것을 변수로 저장
        long maxDuration = Math.round(Math.max(durationBgm, durationLanguage));

        // 새 audio 파일이 저장될 위치
        String outputDir = "./src/main/resources/temp/" + "media_" + UUID.randomUUID().toString();
        String outputFileName = String.format("%s_%s.m3u8", title, language);
        String outputFilePath = String.format("%s/%s", outputDir, outputFileName);

        // 파일 경로가 없을 경우 해당 파일 경로 생성
        File mediaDir = checkFilePath(outputDir);

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(bgmLocalPath)
                .addInput(languageLocalPath)
                .setComplexFilter(String.format(
                                "[0:a]atrim=0:%d,asetpts=PTS-STARTPTS[a0];" +
                                        "[1:a]atrim=0:%d,asetpts=PTS-STARTPTS[a1];" +
                                        "[a0][a1]amix=inputs=2:duration=longest",
                                maxDuration, maxDuration
                        ))
                .addOutput(outputFilePath)
                .setFormat("hls")
                .addExtraArgs("-threads", "1")  // 코어 수를 1개로 제한
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", outputDir + "/" + title + "_" + language + "_%d.ts")
                .addExtraArgs("-c:a", "aac")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        String s3ObjectUrl = mediaUtils.uploadMediaFileOnS3(title, language, outputDir, outputFileName, outputFilePath);

        try {
            // mediaDir 경로의 모든 파일 삭제
            FileUtils.deleteDirectory(mediaDir);

            // s3에서 로컬로 다운로드한 bgm, languageVoice 파일 경로 삭제
            File tempFileBgm = new File(bgmLocalPath);
            File tempFileLanguage = new File(languageLocalPath);
            FileUtils.deleteDirectory(tempFileBgm.getParentFile());
            FileUtils.deleteDirectory(tempFileLanguage.getParentFile());

            System.out.println("임시 파일이 정상적으로 삭제되었습니다.");
        } catch (Exception e) {
            System.out.println(e);
        }

        return ResponseEntity.ok(s3ObjectUrl);
    }

    @GetMapping("/createStream/{title}/video")
    public ResponseEntity<String> getHLSUrl(@PathVariable @NotBlank String title) throws Exception {
        // video 다운로드 및 경로 저장
        mediaUtils.init(videoService, environment);
        String inputFilePath = mediaUtils.getMediaDownloadPathOnLocal(title, "video");

//        File inputFile = new File(inputFilePath);

        // .m3u8 파일 생성
        String localSrc = environment.getProperty("app.local.src");
        FFmpeg ffmpeg = new FFmpeg(localSrc + "ffmpeg");
        FFprobe ffprobe = new FFprobe(localSrc + "ffprobe");

        String outputDir = "./src/main/resources/temp/" + UUID.randomUUID().toString();
        String outputFileName = String.format("%s.m3u8", title);
        String outputFilePath = String.format("%s/%s", outputDir, outputFileName);

        File mediaDir = checkFilePath(outputDir);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFilePath)
                .addOutput(outputFilePath)
                .setFormat("hls")
                .addExtraArgs("-threads", "1")  // 코어 수를 1개로 제한
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", outputDir + "/" + title + "_%d.ts")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        String s3ObjectUrl = mediaUtils.uploadMediaFileOnS3(title, "video", outputDir, outputFileName, outputFilePath);

        try {
            // mediaDir 경로의 모든 파일 삭제
            FileUtils.deleteDirectory(mediaDir);

            // s3에서 로컬로 다운로드한 파일 경로 삭제
            File tempFileBgm = new File(inputFilePath);
            FileUtils.deleteDirectory(tempFileBgm.getParentFile());

            System.out.println("임시 파일이 정상적으로 삭제되었습니다.");
        } catch (Exception e) {
            System.out.println(e);
        }

        return ResponseEntity.ok(s3ObjectUrl);
    }

    @GetMapping("")
    public Iterable<Videos> getVideoList() {
        return videoService.selectAll();
    }

    @GetMapping("/{title}")
    public Optional<Videos> getVideo(@PathVariable @NotBlank String title) {
        return videoService.getVideo(title);
    }

    @GetMapping("/{title}/ko")
    public String getVoiceKo(@PathVariable @NotBlank String title) {
        return videoService.getVoiceKo(title);
    }

    @GetMapping("/{title}/en")
    public String getVoiceEn(@PathVariable @NotBlank String title) {
        return videoService.getVoiceEn(title);
    }

    @GetMapping("/{title}/thai")
    public String getVoiceThai(@PathVariable @NotBlank String title) {
        return videoService.getVoiceThai(title);
    }

    @GetMapping("/{title}/bgm")
    public String getBgm(@PathVariable @NotBlank String title) {
        return videoService.getBgm(title);
    }
}
