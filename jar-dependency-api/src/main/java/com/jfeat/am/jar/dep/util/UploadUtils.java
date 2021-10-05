package com.jfeat.am.jar.dep.util;

import com.jfeat.crud.base.exception.BusinessCode;
import com.jfeat.crud.base.exception.BusinessException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.AssertTrue;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class UploadUtils {
    static Logger logger = LoggerFactory.getLogger(UploadUtils.class.getName());

    // response.setContentType("application/octet-stream");
    // response.addHeader("Content-Disposition", "attachment; filename=README.doc");

    public static File doMultipartFile(MultipartFile file, String targetDir) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }
        Long fileSize = file.getSize();
        if (fileSize == 0) {
            throw new BusinessException(BusinessCode.BadRequest, "file is empty");
        }

        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        File target = new File(String.join(File.separator, targetDirFile.getAbsolutePath(), originalFileName));
        FileUtils.copyInputStreamToFile(file.getInputStream(), target);
        logger.info("file uploaded to: {}", target.getAbsolutePath());

        return target;
    }


    public static File doBase64File(HttpServletRequest request, String targetDir) throws IOException {
        String base64Data = IOUtils.toString(request.getInputStream(), "UTF-8");
        Assert.isTrue(!StringUtils.isEmpty(base64Data), "upload file error!");

        String dataPrefix = "";
        String data = "";
        String[] d = base64Data.split(";base64,");
        if (d != null && d.length == 2) {
            dataPrefix = d[0];
            data = d[1];
        } else {
            throw new BusinessException(BusinessCode.UploadFileError);
        }
        //Assert.isTrue("data:attachment/jar".equalsIgnoreCase(dataPrefix), "data type must be: data:attachment/jar");
        byte[] dataBytes = Base64Utils.decodeFromString(data);

        // save file
        File target = null;
        String DispositionContent = request.getHeader("Content-Disposition");
        Assert.isTrue(!StringUtils.isEmpty(DispositionContent), "Content-Disposition: <should not empty>");
        Assert.isTrue(DispositionContent.contains("filename="), "Content-Disposition: <no \"filename\" field found>");
        String filename = DispositionContent.split("filename=")[1].trim();
        //String uploadFileName = UUID.randomUUID().toString() + suffix;
        String uploadFileName = filename;

        try {
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
            }

            target = new File(String.join(File.separator, targetDir, uploadFileName));
            target.setReadable(true);
            FileUtils.writeByteArrayToFile(target, dataBytes);
            logger.info("file uploaded to: {}", target.getAbsolutePath());

        }catch (IOException e){
            throw new IOException(e);
        }

        return target;
    }
}
