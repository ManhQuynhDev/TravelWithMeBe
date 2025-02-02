package com.quynhlm.dev.be.core;

import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.coyote.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import com.quynhlm.dev.be.core.exception.UnknownException;

public class AppConstant {

    private static final String UPLOAD_DIR = "be/uploads/";

    public static enum AccountUserStatus {
        ACTIVE(0), DEACTIVE(1);

        public final int rawValue;

        AccountUserStatus(int rawValue) {
            this.rawValue = rawValue;
        }
    }

    public static class UserAccountRegex {
        public static final String USERNAME = "^[a-zA-Z0-9._-]{3,}$";
        public static final String PHONE_NUMBER = "([\\+84|84|0]+(3|5|7|8|9|1[2|6|8|9]))+([0-9]{8})";
        public static final String DOB = "^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|1\\d|2[0-8])$|^(19|20)([02468][048]|[13579][26])-02-29$";
        public static final String EMAIL = "^[a-zA-Z]+[a-zA-Z0-9._]*@[a-zA-Z]+(\\.[a-zA-Z]{2,})+$";
    }

    public static String uploadFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();

            if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
                throw new BadRequestException("Only PNG, JPG, and JPEG files are allowed");
            }

            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            Path path = Paths.get(UPLOAD_DIR + fileName);

            Files.write(path, file.getBytes());

            String mediaUrl = "http://localhost:8080/image/" + fileName;

            return mediaUrl;
        } catch (IOException e) {
            throw new UnknownException("Error when saving file");
        }
    }
}
