package com.alcode.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.UUID;

@Component
public class AttachService {
    @Value("${attach.upload.folder}")
    private String attachUploadFolder;

    public String saveImageFromUrl(String imageUrl) {
        String fileName = UUID.randomUUID().toString(); // dasdasd-dasdasda-asdasda-asdasd
        String pathFolder = getYmDString(); // 2022/04/23
        String extension = getExtension(imageUrl); //zari.jpg

        File folder = new File(attachUploadFolder + pathFolder); // attaches/2022/04/23

        if (!folder.exists()) folder.mkdirs();

        String localFileUrl = attachUploadFolder + pathFolder + "/" + fileName + "." + extension;

        try {
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            Path saveFilePath = Paths.get(localFileUrl);
            OutputStream outputStream = Files.newOutputStream(saveFilePath);

            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return localFileUrl;
    }

    public String getExtension(String fileName) {
        // mp3/jpg/npg/mp4.....
        if (fileName == null) {
            throw new RuntimeException("File name null");
        }
        int lastIndex = fileName.lastIndexOf(".");
        return fileName.substring(lastIndex + 1);
    }

    public String getYmDString() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int day = Calendar.getInstance().get(Calendar.DATE);

        return year + "/" + month + "/" + day; // 2022/04/23
    }
}
