package org.example.handler;

import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.util.StrUtil;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 功能描述: minio工具类
 */
@Slf4j
@Component
public class MinioHandler {

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String address;

    @Resource
    private MinioClient minioClient;

    /**
     * 功能描述: 查看存储bucket是否存在
     *
     * @param bucketName
     * @return java.lang.boolean
     */
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("exception info:", e);
            return false;
        }
    }


    /**
     * 功能描述: 文件上传
     *
     * @param file
     * @return com.zb.pojo.MinioFile
     */
    public String upload(MultipartFile file) throws IOException {
        String module = "rag";
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new RuntimeException();
        }
        byte[] bytes = file.getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);

        String currentName = getRandomName(originalFilename);
        try {

            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(StrUtil.isBlank(module) ? currentName : module + "/" + currentName)
                    .contentType(file.getContentType())
                    .stream(inputStream, file.getSize(), -1)
                    .build();

            //文件名称相同会覆盖
            minioClient.putObject(objectArgs);
            return address + "/" + bucketName + "/" + module + "/" + currentName;
        } catch (Exception e) {
            throw new RuntimeException("minio异常:" + e.getMessage());
        }
    }

    public String upload(byte[] bytes, String fileName) {
        String module = "rag";
        String contentType = "image/png";
        InputStream inputStream = new ByteArrayInputStream(bytes);

        String currentName = getRandomName(fileName);
        try {

            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(StrUtil.isBlank(module) ? currentName : module + "/" + currentName)
                    .contentType(contentType)
                    .stream(inputStream, bytes.length, -1)
                    .build();

            minioClient.putObject(objectArgs);
            return address + "/" + bucketName + "/" + module + "/" + currentName;
        } catch (Exception e) {
            throw new RuntimeException("minio异常:" + e.getMessage());
        }
    }

    /**
     * 功能描述: 删除文件
     *
     * @param bucketName
     * @param del
     * @return java.lang.boolean
     */
    public boolean del(String bucketName, String del) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean exists = bucketExists(bucketName);
        if (exists) {
            RemoveObjectArgs args = RemoveObjectArgs.builder().bucket(bucketName).object(del).build();
            minioClient.removeObject(args);
            return true;
        }
        return false;
    }


    /**
     * 功能描述: 下载
     *
     * @param fileUUIDName
     * @param module
     * @param fileName
     * @param resp
     * @return void
     */
    public void download(String fileUUIDName, String module, String fileName, HttpServletResponse resp) {
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucketName).object(StrUtil.isBlank(module) ? fileUUIDName : module + "/" + fileUUIDName).build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                resp.setCharacterEncoding("UTF-8");
                resp.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
                try (ServletOutputStream stream = resp.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            log.error("exception info:", e);
        }
    }

    /**
     * 功能描述: 获取随机uuid名
     *
     * @param fileName
     * @return java.lang.String
     */
    private static String getRandomName(String fileName) {
        String fileFormat = getFileFormat(fileName);
        return UUID.randomUUID().toString().replace("-", "") + fileFormat;
    }

    /**
     * 功能描述: 获取文件后缀
     *
     * @param fileName
     * @return java.lang.String
     */
    private static String getFileFormat(String fileName) {
        int index = fileName.lastIndexOf(".");
        //获取后缀名
        return fileName.substring(index);
    }

}
