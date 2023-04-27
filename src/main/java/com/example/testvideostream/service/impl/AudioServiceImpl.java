package com.example.testvideostream.service.impl;

import com.example.testvideostream.service.AudioService;
import com.example.testvideostream.service.VideoService;
import com.example.testvideostream.utils.MediaUtils;
import lombok.AllArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

import static com.example.testvideostream.utils.FileUtils.checkFilePath;

@Service
@AllArgsConstructor
public class AudioServiceImpl implements AudioService {
    @Autowired
    private VideoService videoService;
    @Autowired
    private Environment environment;
    private MediaUtils mediaUtils;

    @Override
    public void separateAudio(String title, String mediaType) throws IOException {
        try {
            // 해당 파일을 s3 버킷을 참조해서 다운로드
            mediaUtils.init(videoService, environment);
            String audioLocalPath = mediaUtils.getMediaDownloadPathOnLocal(title, mediaType);
            System.out.println("audioLocalPath: " + audioLocalPath);

            // 1. MultipartFile -> File 변환
            File audioFile = new File(audioLocalPath);

            // 2. net.bramp 라이브러리를 사용하여 ffmpeg 명령어 생성
            String localSrc = environment.getProperty("app.local.src");
            FFmpeg ffmpeg = new FFmpeg(localSrc + "ffmpeg"); // ffmpeg 실행 파일 경로
            FFprobe ffprobe = new FFprobe(localSrc + "ffprobe"); // ffprobe 실행 파일 경로

            // FFmpeg 라이브러리에 포함된 FFprobe 라이브러리를 사용하여 오디오 파일의 정보를 분석하고 결과를 저장함
            FFmpegProbeResult probeResult = ffprobe.probe(audioFile.getAbsolutePath());

            System.out.println("probeResult: " + probeResult);
            List<FFmpegStream> streams = probeResult.getStreams();
            for (FFmpegStream stream: streams) {
                if (stream.codec_type == FFmpegStream.CodecType.AUDIO) {
                    int channels = stream.channels;
                    String channelLayout = stream.channel_layout;
                    boolean hasFLChannel = channelLayout.contains("FL");
                    System.out.println("Number of audio channels: " + channels);
                    System.out.println("Channel layout: " + channelLayout);
                    System.out.println("Has FL channel: " + hasFLChannel);
                    break;
                }
            }

            String filePath = audioFile.getParentFile().getAbsolutePath(); // 임시 디렉토리 생성
            String outputFileNameBGM = String.format("%s_bgm.mp3", title);
            String outputFileNameVoice = String.format("%s_voice.mp3", title);
            String outputFilePathBGM = String.format("%s/%s", filePath, outputFileNameBGM);
            String outputFilePathVoice = String.format("%s/%s", filePath, outputFileNameVoice);
            System.out.println("outputFilePathVoice: " + outputFilePathVoice);

            File mediaDir = checkFilePath(filePath);

            // 3. FFmpeg 라이브러리로 음성 분리
            FFmpegBuilder builderVoice = new FFmpegBuilder()
                    .setInput(audioFile.getAbsolutePath()) // 입력 파일 설정
                    .addOutput(outputFilePathVoice)
                    .setAudioCodec("aac") // 오디오 코덱 설정
                    .setVideoCodec("copy") // 비디오 코덱 설정
                    .setFormat("mp4") // 포맷 설정
                    .disableSubtitle() // 자막 설정
                    .addExtraArgs("-threads", "1") // 코어 수를 1개로 제한
                    .addExtraArgs("-map_channel", "0.0.0")
                    .done(); // FFmpeg 명령어 빌더 생성 완료

            // 4. FFmpeg 라이브러리로 음성 분리
            FFmpegBuilder builderBGM = new FFmpegBuilder()
                    .setInput(audioFile.getAbsolutePath()) // 입력 파일 설정
                    .addOutput(outputFilePathBGM)
                    .setAudioCodec("aac") // 오디오 코덱 설정
                    .setVideoCodec("copy") // 비디오 코덱 설정
                    .setFormat("mp4") // 포맷 설정
                    .disableSubtitle() // 자막 설정
                    .addExtraArgs("-threads", "1") // 코어 수를 1개로 제한
                    .addExtraArgs("-map_channel", "0.0.1")
                    .done(); // FFmpeg 명령어 빌더 생성 완료

            // FFmpeg 명령어 실행을 위한 라이브러리를 이용함

            builderVoice.overrideOutputFiles(true); // 덮어쓰기 허용
            builderBGM.overrideOutputFiles(true); // 덮어쓰기 허용

            // 파일 경로가 없을 경우 해당 파일 경로 생성
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe); // FFmpegExecutor 생성
            executor.createJob(builderVoice).run(); // FFmpeg 명령어 실행
            executor.createJob(builderBGM).run(); // FFmpeg 명령어 실행

            // 분리된 오디오 파일을 저장한다.
            File extractedFileVoice = new File(outputFilePathVoice); // 분리된 오디오 파일 경로 설정
            File extractedFileBGM = new File(outputFilePathBGM); // 분리된 오디오 파일 경로 설정
            System.out.println("extractedFileVoice: " + extractedFileVoice);
            System.out.println("extractedFileBGM: " + extractedFileBGM);

//            Files.copy(extractedFileVoice.toPath(), new File(filePath + "/extracted_voice.mp3").toPath()); // 분리된 오디오 파일을 저장
//            Files.copy(extractedFileBGM.toPath(), new File(filePath + "/extracted_bgm.mp3").toPath()); // 분리된 오디오 파일을 저장}

            System.out.println("파일이 정상적으로 분리되었습니다.");
        } catch (IOException e) {
            System.out.println(e);
            throw new IOException(e);
        }
    }
}
