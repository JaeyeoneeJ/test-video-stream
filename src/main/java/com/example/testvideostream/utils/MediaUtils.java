package com.example.testvideostream.utils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.example.testvideostream.exception.NotFoundException;
import com.example.testvideostream.model.Videos;
import com.example.testvideostream.service.VideoService;
import org.apache.commons.io.FileUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Component
public class MediaUtils {
    private static VideoService service;
    private static Environment environment;

    public static void init(VideoService videoService, Environment env) {
        service = videoService;
        environment = env;
    }

    public static String getMediaDownloadPathOnLocal(String title, String mediaType) {
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
            case "bgm":
                getMediaName = videoInfo.get().getBgm();
                break;
            case "ko":
                getMediaName = videoInfo.get().getVoiceKo();
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

            String endPoint = environment.getProperty("app.endPoint.url");
            String regionName = environment.getProperty("app.regionName");
            String accessKey = environment.getProperty("app.ncp.accessKey");
            String secretKey = environment.getProperty("app.ncp.secretKey");

            AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, regionName))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                    .build();

            S3Object s3Object = s3.getObject(bucketName, objectName);
            InputStream inputStream = s3Object.getObjectContent();
            System.out.println("inputStream: " + inputStream);

            // 임시 파일 생성
            File inputFile = File.createTempFile(title, getMediaFormat);
            inputFile.deleteOnExit();

            // 파일을 저장할 경로 생성
            String outputDir = "src/main/resources/temp/" + "media_" + UUID.randomUUID().toString();
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

            // 메모리 누수 방지를 위하여 inputStream을 명시적으로 닫음
            inputStream.close();

            return outputFilePath;

        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static String uploadMediaFileOnS3 (String title, String mediaType, String outputDir, String outputFileName, String outputFilePath) {
        try {
            String endPoint = environment.getProperty("app.endPoint.url");
            String regionName = environment.getProperty("app.regionName");
            String accessKey = environment.getProperty("app.ncp.accessKey");
            String secretKey = environment.getProperty("app.ncp.secretKey");

            AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, regionName))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                    .build();

            // s3 버킷 설정
            String bucketName = environment.getProperty("app.ncp.bucketName");
            String objectPath = String.format("media/%s/", title);

            // .ts 파일들을 S3에 업로드
            String[] tsFiles = new File(outputDir).list((dir, name) -> name.endsWith(".ts"));
            String newS3Path = String.format("%sstreamingFile/%s/", objectPath, mediaType);
            for (String tsFile : tsFiles) {
                File file = new File(String.format("%s/%s", outputDir, tsFile));
                String s3KeyName = String.format("%s%s", newS3Path, tsFile);
                s3.putObject(bucketName, s3KeyName, file);
            }

            // .m3u8 파일 S3 업로드
            String s3KeyName = String.format("%s%s", newS3Path, outputFileName);
            File m3u8File = new File(outputFilePath);
            s3.putObject(bucketName, s3KeyName, m3u8File);

            String result = newS3Path + "에 정상적으로 업로드 되었습니다. ";
            System.out.println(result);

            // .m3u8 URL 반환
            String s3ObjectUrl = s3.getUrl(bucketName, s3KeyName).toString();

            return s3ObjectUrl;
        } catch (Exception e) {
            System.out.println("uploadMediaFileOnS3, Exception error :" + e);
            return e.toString();
        }

    }
}
