package org.licenseUploadToS3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Scanner;


public class S3Uploader {
    private static final String bucketName = "license-upload-mv-prj3";
    private static final Constants data = new Constants();

    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);
        String entry = "";
        while (!entry.equalsIgnoreCase("q")) {
            System.out.println("Please enter full file path or q to leave: ");
            entry = in.nextLine();
            System.out.println("You entered: " + entry);

            if (entry.equalsIgnoreCase("q")) {
                System.out.println("Exit :) " );
                break;
            } else {


                File file = new File(entry);
                String fileName = file.getName();
                System.out.println("File upload in progress: " + fileName);

                String objectKey = file.getName();

                try {
                    S3Client s3Client = S3Client.builder().build();
                    // Create PutObjectRequest
                    HashMap<String, String> metaData = new HashMap<>();
                    metaData.put("Location", data.getRandomAddress());
                    metaData.put("DateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a")));
                    metaData.put("Type", data.getRandomViolation());

                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey).metadata(metaData)
                            .build();

                    // Upload the file
                    s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                    System.out.println("File uploaded successfully!\n");

                } catch (Exception ex) {
                    System.out.println("File upload is failed.");
                    System.out.println("Error while uploading file. " + ex.getMessage());
                }
            }
        }

    }
}


