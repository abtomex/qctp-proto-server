package ru.abtomex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.abtomex.dto.QctpMessage;
import ru.abtomex.service.QrCodeService;
import ru.abtomex.session.SessionRepo;
import ru.abtomex.session.UploadSession;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequiredArgsConstructor
public class QrController {

    private final QrCodeService service;
    private final SessionRepo sessionRepo;
    private final ObjectMapper objectMapper;

    @Value("app.qctp.serviceName")
    private String serviceName;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestParam("file") MultipartFile file, Model model) throws Exception {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "File not found or empty file");
            return index();
        }

        byte[] originalBytes = file.getBytes();
        String originalName = file.getOriginalFilename();

        // 1. Сжимаем файл в 7z с максимальной степенью сжатия
        byte[] compressedBytes = service.compressTo7z(originalBytes, sanitize(originalName));

        // 2. Кодируем сжатый архив в Base64
        String base64 = service.encodeToBase64(compressedBytes);

        // 3. Разбиваем Base64 на чанки и генерируем QR-коды
        List<String> chunks = service.splitBase64(base64);

        List<byte[]> qrPngList = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            final String chunk = chunks.get(i);
            final QctpMessage message = new QctpMessage(
                    sanitize(originalName) + ".7z",
                    chunks.size(),
                    i + 1,
                    chunk
            );
            qrPngList.add(service.generateQrCodePng(objectMapper.writeValueAsString(message)));
        }

        // 4. Создаем сессию: в fileSize сохраняем размер сжатого архива,
        UploadSession session = service.createSession(
                originalName,
                compressedBytes.length,
                chunks,
                qrPngList
        );

        return "redirect:/view/" + session.getSessionId() + "?index=0";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable String id, @RequestParam(defaultValue = "0") int index, Model model) {
        UploadSession s = sessionRepo.get(id);
        if (s == null) {
            model.addAttribute("error", "session was not found or expired");
            return "index";
        }
        if (index < 0) index = 0;
        if (index >= s.getTotal()) index = s.getTotal() - 1;

        model.addAttribute("sessionId", s.getSessionId());
        model.addAttribute("fileName", s.getFileName());
        model.addAttribute("fileSize", s.getFileSize());
        model.addAttribute("chunkSize", s.getChunkSize());
        model.addAttribute("total", s.getTotal());
        model.addAttribute("index", index);
        model.addAttribute("currentChunk", s.getChunks().get(index));

        return "view";
    }

    @GetMapping(value = "/qr/{id}/{index}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qrByIndex(
            @PathVariable String id,
            @PathVariable int index
    ) {
        final UploadSession session = sessionRepo.get(id);
        if (session == null || index < 0 || index >= session.getTotal()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(session.getQrPngCache().get(index));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadZip(@PathVariable String id) throws Exception {
        final UploadSession session = sessionRepo.get(id);
        if (session == null) return ResponseEntity.notFound().build();

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (
                final ZipOutputStream zip = new ZipOutputStream(bos)
        ) {
            String baseName = sanitize(session.getFileName());
            for (int i = 0; i < session.getTotal(); i++) {
                final String name = String.format("%s_part_%04d_of_%04d.png", baseName, i + 1, session.getTotal());
                zip.putNextEntry(new ZipEntry(name));
                zip.write(session.getQrPngCache().get(i));
                zip.closeEntry();
            }
        }
        final byte[] zipBytes = bos.toByteArray();
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qr_" + session.getSessionId() + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    private String sanitize(String name) {
        if (!StringUtils.hasLength(name)) return "file";
        return name.replaceAll("[^\\p{L}0-9._-]", "_");
    }
}