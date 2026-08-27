package com.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crm.common.enums.ResourceStatus;
import com.crm.entity.Resource;
import com.crm.repository.ResourceRepository;
import com.crm.repository.SysUserRepository;
import com.crm.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 文件上传与异步处理（UC-035 / UC-036，v3.8）集成测试。
 *
 * <p>验证：上传 PDF 保存至用户目录（user_no）并记录 file_key，异步 Mock 处理后状态为
 * PROCESSED；上传 zip 解压后对其中每个 PDF/Word 创建子资源并处理；不支持类型被拒绝。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private StorageService storageService;

    private String randomPhone() {
        return "131" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    /** 注册用户并返回 JWT Token */
    private String registerAndGetToken(String phone) throws Exception {
        mockMvc.perform(post("/api/sms/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        String resp = mockMvc.perform(post("/api/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"smsCode\":\"000000\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).path("data").path("token").asText();
    }

    /** 注册用户并返回 userId */
    private Long registerAndGetUserId(String phone) throws Exception {
        registerAndGetToken(phone);
        return sysUserRepository.findByPhone(phone).orElseThrow().getUserId();
    }

    /** 等待资源异步处理完成（Mock 处理很快，轮询 5s） */
    private Resource awaitProcessed(Long resourceId) throws InterruptedException {
        Resource r = null;
        for (int i = 0; i < 50; i++) {
            r = resourceRepository.findById(resourceId).orElse(null);
            if (r != null && r.getStatus() == ResourceStatus.PROCESSED) {
                return r;
            }
            Thread.sleep(100);
        }
        return r;
    }

    /** 构造包含 pdf/docx/txt 条目的 zip 字节 */
    private byte[] buildTestZip() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("docs/resume.pdf"));
            zos.write("%PDF-1.4 mock pdf".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("docs/cover.docx"));
            zos.write("mock docx".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("ignored".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    /** UC-035/036：上传 PDF → 记录 file_key（含 user_no 目录）→ 物理文件保存 → 异步处理完成 */
    @Test
    void uploadPdfFlowShouldSucceed() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);
        String userNo = sysUserRepository.findByPhone(phone).orElseThrow().getUserNo();

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "%PDF-1.4 mock content".getBytes(StandardCharsets.UTF_8));

        String resp = mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.resourceType").value("FILE"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andReturn().getResponse().getContentAsString();
        Long resourceId = objectMapper.readTree(resp).path("data").path("resourceId").asLong();
        String fileKey = objectMapper.readTree(resp).path("data").path("fileKey").asText();

        // file_key 以用户编号目录开头，不含绝对路径
        assertTrue(fileKey.startsWith(userNo + "/"), "file_key 应以 user_no 目录开头");
        assertTrue(fileKey.endsWith(".pdf"));
        assertTrue(!fileKey.contains(":"), "file_key 不应包含盘符/绝对路径");

        // 物理文件已保存
        assertTrue(storageService.exists(fileKey), "物理文件应已保存");

        // 等待异步 Mock 处理完成
        Resource processed = awaitProcessed(resourceId);
        assertNotNull(processed);
        assertEquals(ResourceStatus.PROCESSED, processed.getStatus());
    }

    /** UC-036：上传 zip → 解压后对其中每个 PDF/Word 创建子资源并处理（txt 忽略） */
    @Test
    void uploadZipFlowShouldCreateChildResources() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);

        MockMultipartFile file = new MockMultipartFile(
                "file", "docs.zip", "application/zip", buildTestZip());

        String resp = mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long zipResourceId = objectMapper.readTree(resp).path("data").path("resourceId").asLong();

        // 等待 zip 自身处理完成
        Resource zipRes = awaitProcessed(zipResourceId);
        assertNotNull(zipRes);
        assertEquals(ResourceStatus.PROCESSED, zipRes.getStatus());

        // zip 解压出的子资源：2 个（pdf + docx），txt 被忽略
        String zipDir = zipRes.getFileKey().substring(0, zipRes.getFileKey().lastIndexOf('.'));
        List<Resource> children = resourceRepository.findAll().stream()
                .filter(r -> r.getFileKey() != null && r.getFileKey().startsWith(zipDir + "/"))
                .toList();
        assertEquals(2, children.size(), "zip 应解压出 2 个子资源（pdf + docx）");
        assertTrue(children.stream().allMatch(c -> c.getStatus() == ResourceStatus.PROCESSED));
        assertTrue(children.stream().allMatch(c -> storageService.exists(c.getFileKey())));
    }

    /** UC-035：不支持的扩展名被拒绝（FILE_TYPE_NOT_ALLOWED = 1702） */
    @Test
    void uploadUnsupportedTypeShouldFail() throws Exception {
        String phone = randomPhone();
        String token = registerAndGetToken(phone);

        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1702));
    }
}
