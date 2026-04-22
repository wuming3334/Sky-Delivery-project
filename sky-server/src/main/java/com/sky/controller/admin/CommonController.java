package com.sky.controller.admin;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyuncs.exceptions.ClientException;
import com.sky.properties.AliOssProperties;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {
@Autowired
private AliOssUtil aliOssUtil;

    /**
     * 文件上传接口
     */
    @RequestMapping("/upload")
    @ApiOperation("文件上传接口")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file);
        try {
            //拼接文件名
            //获取原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取文件名
            String fileLast = originalFilename.substring(originalFilename.lastIndexOf("."));
            //uuid
            String uuid = UUID.randomUUID().toString();
            //构造新文件名称
            String newFileName = uuid+fileLast;
            //文件访问路径
            String filePath = aliOssUtil.upload(file.getBytes(), newFileName);
            return Result.success(filePath);
        } catch (IOException e) {
           log.error("文件上传失败：{}", e.getMessage());
        }
        return null;
    }
}
