package org.example.handler;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class QrCodeHandler {

    @Resource
    private MinioHandler minioHandler;

    public String qrCode(JSONObject req) {
        try {
            String qrCodeText = req.toString();
            byte[] qrCodeImageData = generateQRCodeImage(qrCodeText);
            String name = RandomUtil.randomString(10);
            return minioHandler.upload(qrCodeImageData, name + ".png");
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] generateQRCodeImage(String text) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200, hints);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        } catch (WriterException | IOException e) {
            return new byte[0];
        }
    }
}