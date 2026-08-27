package com.crm.controller;

import com.crm.common.enums.OwnerType;
import com.crm.dto.common.ApiResponse;
import com.crm.dto.resource.ResourceResponse;
import com.crm.security.SecurityUtils;
import com.crm.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（UC-035，v3.8）。
 * 上传 PDF / Word / zip，保存后发布事件由异步监听器处理。
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 上传文件（multipart/form-data）。
     *
     * @param file            文件（pdf / doc / docx / zip）
     * @param parentResourceId 父资源 ID（可选）
     * @param ownerType        所有权归属：USER（个人，默认）/ COMPANY（企业）
     * @param ownerId          所有者 ID（默认当前用户）
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResourceResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "parentResourceId", required = false) Long parentResourceId,
            @RequestParam(value = "ownerType", required = false) OwnerType ownerType,
            @RequestParam(value = "ownerId", required = false) Long ownerId) {
        return ApiResponse.ok(fileUploadService.upload(
                SecurityUtils.getCurrentUserId(), file, parentResourceId, ownerType, ownerId));
    }
}
