package com.example.takeout.controller;

import com.example.takeout.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${takeout.path}")
    private String basePath;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        //参数名要和前端一致
        //原始文件名
        String originalFilename = file.getOriginalFilename();
        //文件后缀名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        File dir = new File(basePath);
        if(!dir.exists()){
            dir.mkdir();
        }
        //随机文件名
        String fileName = UUID.randomUUID().toString() + suffix;
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.success(fileName);
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response, String name){
        try {
            //通过输入流读取文件内容
            FileInputStream inputStream = new FileInputStream(new File(basePath + name));
            //通过输出流将文件写回浏览器，在浏览器展示图片
            ServletOutputStream outputStream = response.getOutputStream();

            response.setContentType("image/jpeg");

            int len = 0;
            byte[] bytes = new byte[1024];
            while((len = inputStream.read(bytes)) != -1){
                outputStream.write(bytes);
                outputStream.flush();
            }

            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
