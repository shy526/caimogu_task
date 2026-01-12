package com.github.shy526.github;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class LocalFile {

    public static void  writeFile(String fileName, Set<String> contents){
        String runDir = new File("").getAbsoluteFile().getAbsolutePath();
        String filePath = runDir + File.separator + fileName;
        try {
            Files.write(Paths.get(filePath), contents, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    public static Set<String>  readFile(String fileName){
        String runDir = new File("").getAbsoluteFile().getAbsolutePath();
        String filePath = runDir + File.separator + fileName;
        try {
            return new HashSet<>(Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8));
        } catch (IOException ignored) {

        }
        return new HashSet<>();
    }

}
