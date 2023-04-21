package com.example.testvideostream.controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.testvideostream.exception.NotFoundException;
import com.example.testvideostream.model.Videos;
import com.example.testvideostream.service.VideosService;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import java.io.*;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    @Autowired
    private VideosService service;

    @Autowired
    private Environment environment;

    private AmazonS3 s3;

    @PostConstruct
    public void init() {
        String endPoint = environment.getProperty("app.endPoint.url");
        String regionName = environment.getProperty("app.regionName");
        String accessKey = environment.getProperty("app.ncp.accessKey");
        String secretKey = environment.getProperty("app.ncp.secretKey");

        this.s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, regionName))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();
    }

    @GetMapping("/createStream/{title}/language/{language}")
    public ResponseEntity<String> speechSynthesisOnLanguage (
            @PathVariable @NotBlank String title,
            @PathVariable @NotBlank String language
    ) throws Exception {
        // 해당 타이틀의 정보를 database에서 가져오기
        Optional<Videos> videoInfo = service.getVideo(title);

        // background music 다운로드 및 경로 저장
        String bgLocalPath = getMediaDownloadPathOnLocal(title, "bg");
        String languageLocalPath = getMediaDownloadPathOnLocal(title, language);

        // ffmpeg, ffprobe 선언
        String localSrc = environment.getProperty("app.local.src");
        FFmpeg ffmpeg = new FFmpeg(localSrc + "ffmpeg");
        FFprobe ffprobe = new FFprobe(localSrc + "ffprobe");

        // 두 audio 파일의 재생 길이 확인
        FFmpegProbeResult probeResultBg = ffprobe.probe(bgLocalPath);
        FFmpegProbeResult probeResultLanguage = ffprobe.probe(languageLocalPath);

        double durationBg = probeResultBg.getFormat().duration;
        double durationLanguage = probeResultLanguage.getFormat().duration;

        // 두 audio 파일 중 재생 길이가 더 긴 것을 변수로 저장
        long maxDuration = Math.round(Math.max(durationBg, durationLanguage));

        // 새 audio 파일이 저장될 위치
        String outputDir = "./src/main/resources/temp/" + "audio_" + UUID.randomUUID().toString();
        String outputFileName = String.format("%s_%s.m3u8", title, language);
        String outputFilePath = String.format("%s/%s", outputDir, outputFileName);

        // 파일 경로가 없을 경우 해당 파일 경로 생성
        File mediaDir = new File(outputDir);
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .addInput(bgLocalPath)
                .addInput(languageLocalPath)
                .setComplexFilter(String.format(
                                "[0:a]atrim=0:%d,asetpts=PTS-STARTPTS[a0];" +
                                        "[1:a]atrim=0:%d,asetpts=PTS-STARTPTS[a1];" +
                                        "[a0][a1]amix=inputs=2:duration=longest",
                                maxDuration, maxDuration
                        ))
                .addOutput(outputFilePath)
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", outputDir + "/" + title + "_" + language + "_%d.ts")
                .addExtraArgs("-c:a", "aac")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        // s3 버킷 설정
        String bucketName = environment.getProperty("app.ncp.bucketName");
        String objectPath = String.format("media/%s/", title);

        // .m3u8 파일 로컬에 저장
        String localFilePath = String.format("%s/%s", outputDir, outputFileName);

        // .ts 파일들을 S3에 업로드
        String[] tsFiles = new File(outputDir).list((dir, name) -> name.endsWith(".ts"));
        String newS3Path = String.format("%sstreamingFile/%s/", objectPath, language);
        for (String tsFile : tsFiles) {
            File file = new File(String.format("%s/%s", outputDir, tsFile));
            String s3KeyName = String.format("%s%s", newS3Path, tsFile);
            s3.putObject(bucketName, s3KeyName, file);
        }

        // .m3u8 파일 S3 업로드
        String s3KeyName = String.format("%s%s", newS3Path, outputFileName);
        File m3u8File = new File(localFilePath);
        s3.putObject(bucketName, s3KeyName, m3u8File);

        // mediaDir 경로의 모든 파일 삭제
        FileUtils.deleteDirectory(mediaDir);

        // s3에서 로컬로 다운로드한 bg, languageVoice 파일 경로 삭제
        File tempFileBg = new File(bgLocalPath);
        File tempFileLanguage = new File(languageLocalPath);
        FileUtils.deleteDirectory(tempFileBg.getParentFile());
        FileUtils.deleteDirectory(tempFileLanguage.getParentFile());

        System.out.println(newS3Path + "에 정상적으로 업로드 되었습니다. ");

        // .m3u8 URL 반환
        String s3ObjectUrl = s3.getUrl(bucketName, s3KeyName).toString();

        return ResponseEntity.ok(s3ObjectUrl);
    }

    private String getMediaDownloadPathOnLocal (String title, String mediaType) {
        // 해당 타이틀의 정보를 database에서 가져오기
        Optional<Videos> videoInfo = service.getVideo(title);

        // S3 버킷 정보 설정
        String bucketName = environment.getProperty("app.ncp.bucketName");
        String objectPath = String.format("media/%s/", title);
        String objectName;

        // 변수 초기화
        String getMediaName = null;
        String getMediaFormat;

        switch (mediaType) {
            case "video":
                getMediaName = videoInfo.get().getVideo();
                break;
            case "bg":
                getMediaName = videoInfo.get().getBg();
                break;
            case "ko":
                getMediaName = videoInfo.get().getVoiceKr();
                break;
            case "en":
                getMediaName = videoInfo.get().getVoiceEn();
                break;
            case "thai":
                getMediaName = videoInfo.get().getVoiceThai();
                break;
        }

        try {
            // db table의 entity 내 column 값이 비어있는지 확인
            if (getMediaName == null || getMediaName.isEmpty()) {
                throw new NullPointerException("getMediaName is null or empty.");
            }

            // 확장자 식별
            int lastDotIndex = getMediaName.lastIndexOf('.');
            if (lastDotIndex >= 0) {
                getMediaFormat = getMediaName.substring(lastDotIndex);
                System.out.println("getMediaName: " + getMediaName);
                System.out.println("getMediaFormat: " + getMediaFormat);
            } else {
                throw new NotFoundException("media format error: 확장자를 식별할 수 없습니다.");
            }

            // S3 버킷에 media 타입 전달
            objectName = String.format("%s%s", objectPath, getMediaName);
            System.out.println("objectName: " + objectName);

            S3Object s3Object = s3.getObject(bucketName, objectName);
            InputStream inputStream = s3Object.getObjectContent();
            System.out.println("inputStream: " + inputStream);

            // 임시 파일 생성
            File inputFile = File.createTempFile(title, getMediaFormat);
            inputFile.deleteOnExit();

            // 파일을 저장할 경로 생성
            String outputDir = "./src/main/resources/temp/" + "audio_" + UUID.randomUUID().toString();
            String outputFilePath = String.format("%s/%s", outputDir, inputFile.getName());
            File outputFile = new File(outputFilePath);

            // 파일 경로가 없을 경우 해당 파일 경로 생성
            File mediaDir = new File(outputDir);
            if (!mediaDir.exists()) {
                mediaDir.mkdirs();
            }

            // 임시 파일을 지정된 경로에 복사
            FileUtils.copyInputStreamToFile(inputStream, outputFile);

            System.out.println("outputFilePath: " + outputFilePath);
            return outputFilePath;

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    @GetMapping("/createStream/{title}/video")
    public ResponseEntity<String> getHLSUrl(@PathVariable @NotBlank String title) throws Exception {
        // 해당 타이틀의 정보를 database에서 가져오기
        Optional<Videos> videoInfo = service.getVideo(title);
        String db_video = videoInfo.get().getVideo();

        // S3 버킷에서 파일 다운로드
        String bucketName = environment.getProperty("app.ncp.bucketName");
        String objectPath = String.format("media/%s/", title);
        String objectName = String.format("%s%s", objectPath, db_video);
        System.out.println("objectName: " + objectName);

        S3Object s3Object = s3.getObject(bucketName, objectName);
        InputStream inputStream = s3Object.getObjectContent();
        System.out.println("inputStream: " + inputStream);

        // 임시 파일 생성
        File inputFile = File.createTempFile(title, ".mp4");
        inputFile.deleteOnExit();

        // InputStream을 임시 파일에 복사
        FileUtils.copyInputStreamToFile(inputStream, inputFile);

        // .m3u8 파일 생성
        String localSrc = environment.getProperty("app.local.src");
        FFmpeg ffmpeg = new FFmpeg(localSrc + "ffmpeg");
        FFprobe ffprobe = new FFprobe(localSrc + "ffprobe");

        String outputDir = "./src/main/resources/temp/" + UUID.randomUUID().toString();
        String outputFileName = String.format("%s.m3u8", title);
        String outputFilePath = String.format("%s/%s", outputDir, outputFileName);

        File mediaDir = new File(outputDir);
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFile.getAbsolutePath())
                .addOutput(outputFilePath)
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", outputDir + "/" + title + "_%d.ts")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        executor.createJob(builder).run();

        // .m3u8 파일 로컬에 저장
        String localFilePath = String.format("%s/%s", outputDir, outputFileName);

        // .ts 파일들을 S3에 업로드
        String[] tsFiles = new File(outputDir).list((dir, name) -> name.endsWith(".ts"));
        String newS3Path = objectPath + "streamingFile/video/";
        for (String tsFile : tsFiles) {
            File file = new File(String.format("%s/%s", outputDir, tsFile));
            String s3KeyName = String.format("%s%s", newS3Path, tsFile);
            s3.putObject(bucketName, s3KeyName, file);
        }

        // .m3u8 파일 S3 업로드
        String s3KeyName = String.format("%s%s", newS3Path, outputFileName);
        File m3u8File = new File(localFilePath);
        s3.putObject(bucketName, s3KeyName, m3u8File);

        // mediaDir 경로의 모든 파일 삭제
        FileUtils.deleteDirectory(mediaDir);

        // .m3u8 URL 반환
        String s3ObjectUrl = s3.getUrl(bucketName, s3KeyName).toString();

        System.out.println(newS3Path + "에 정상적으로 업로드 되었습니다. ");

        return ResponseEntity.ok(s3ObjectUrl);
    }


    @GetMapping("/download/{title}")
    public ResponseEntity<InputStreamResource> downloadVideo(
            @PathVariable @NotBlank String title
    ) {
        String bucketName = environment.getProperty("app.ncp.bucketName");
        String objectPath = String.format("media/%s/", title);
        String objectName = String.format("%s%s.mp4", objectPath, title);
        String downloadFilePath = "src/main/resources/temp"; // 다운로드 경로

        // create a temporary file to store downloaded object
        File tempFile;
        try {
            tempFile = File.createTempFile("video",".mp4");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        try {
            S3Object s3Object = s3.getObject(bucketName, objectName);
            S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            byte[] bytesArray = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = s3ObjectInputStream.read(bytesArray)) != -1) {
                outputStream.write(bytesArray, 0, bytesRead);
            }

            outputStream.close();
            s3ObjectInputStream.close();
            System.out.format("Object %s has been downloaded. \n", objectName);

            // Move the downloaded file to desired download path
            File downloadFile = new File(downloadFilePath + "/" + title + ".mp4");
            tempFile.renameTo(downloadFile);
            String currentDirectory = System.getProperty("user.dir");
            System.out.println("현재 작업 경로: " + currentDirectory);
            System.out.println("downleadFile: " + downloadFile);

            // create an input stream resource from temporary file
            InputStreamResource resource = new InputStreamResource(new FileInputStream(downloadFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + title + ".mp4");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(tempFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (AmazonS3Exception | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            // delete temporary file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                System.out.println("임시 파일을 삭제했다.");
            }
        }
    }


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
}
