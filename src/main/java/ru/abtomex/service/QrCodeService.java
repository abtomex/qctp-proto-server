package ru.abtomex.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.abtomex.session.SessionRepo;
import ru.abtomex.session.UploadSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.google.zxing.EncodeHintType.*;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    @Value("${app.qctp.chunk-size-in-bytes}")
    public int chunkSizeBytes;

    @Value("${app.qctp.qrCodeSizePx}")
    public int qrCodeSize;

    private final SessionRepo sessionRepo;

    public String encodeToBase64(byte[] fileBytes) {
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    public List<String> splitBase64(String base64) {

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < base64.length(); i += chunkSizeBytes) {
            int end = Math.min(base64.length(), i + chunkSizeBytes);
            chunks.add(base64.substring(i, end));
        }

        return chunks;
    }

    public byte[] generateQrCodePng(String content) throws WriterException, IOException {
        return generateQrCodePng(content, qrCodeSize);
    }
    public byte[] generateQrCodePng(String content, int size) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    public UploadSession createSession(String originalFilename, int length, List<String> chunks, List<byte[]> qrPngList) {
        return sessionRepo.create(originalFilename, length, chunkSizeBytes, chunks, qrPngList);
    }
}
