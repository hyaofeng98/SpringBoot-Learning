package com.hyao.advice.controller;

import com.hyao.advice.common.BizException;
import com.hyao.advice.common.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载（跳过包装）。
 */
@RestController
public class DownloadController {
    /**
     * 文件下载。
     *
     * <pre>
     *     请求示例：
     *     GET http://localhost:8081/download?filename=application.yaml
     * </pre>
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) {
        // 加载文件资源
        Resource resource = new ClassPathResource(filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(filename, StandardCharsets.UTF_8)
                                    .build()
                                    .toString())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
