package org.DmvService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.dmv.service.entity.Vehicle;
import org.dmv.service.entity.dmv;
import org.json.JSONException;
import org.json.JSONObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DmvService {

    private static final String filePath = "D:\\Shkola\\CLASSES\\Cloud comp\\Project3\\Project3\\Project3\\DMVDatabase.xml";
    private static SqsClient sqsClient;
    private static dmv dataFromXML;
    private static final String RECEIVE_DOWNWARD_SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/945076052403/CaliLicenseDownwardQ";
    private static final String RESIDENT_NOTIFY_UPWARD_SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/945076052403/ViolationNotifyResident";


    public static void main(String[] args) throws JSONException {
            getDB(filePath);
            initSQSClient();

        while (true) {
            try {

                ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                        .queueUrl(RECEIVE_DOWNWARD_SQS_QUEUE_URL)
                        .waitTimeSeconds(20)
                        .maxNumberOfMessages(10)
                        .build();

                List<Message> sqsMessages = sqsClient.receiveMessage(receiveMessageRequest)
                        .messages();

                for (Message message : sqsMessages) {

                    // Parse the message
                    ObjectMapper objectMapper = new ObjectMapper();
//                    JSONObject parsedMessage = objectMapper.readValue(message.body(), JSONObject.class);

                    HashMap<String, String> parsedMessage = objectMapper.readValue(message.body(), HashMap.class);
//                    System.out.println("====>>> DATE: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "  ====>>> READ MESSAGE: " + new JSONObject().put("id", parsedMessage.get("id").toString()));
                    System.out.println("====>>> DATE: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "  ====>>> READ MESSAGE: " + parsedMessage.toString());

                    // DELETE MESSAGE FROM QUEUE
                    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                            .queueUrl(RECEIVE_DOWNWARD_SQS_QUEUE_URL)
                            .receiptHandle(message.receiptHandle())
                            .build();

                    sqsClient.deleteMessage(deleteMessageRequest);


                    // FIND VEHICLE IN DB
                    JSONObject registartionInfo = new JSONObject();
                    for (Vehicle vehicle : dataFromXML.getVehicles()) {
                        if (vehicle.getPlate().equalsIgnoreCase(parsedMessage.get("plate"))) {

                            registartionInfo
                                    .put("make", vehicle.getMake())
                                    .put("model", vehicle.getModel())
                                    .put("color", vehicle.getColor())
                                    .put("ownerName", vehicle.getOwner().getName())
                                    .put ("contact",vehicle.getOwner().getContact());
                            for ( Map.Entry<String, String> entry : parsedMessage.entrySet()) {
                                registartionInfo.put(entry.getKey(),entry.getValue());
                            }
                            System.out.printf("====>>> Vehicle registration: " + vehicle.getPlate() + " " + registartionInfo + "%n");
                            SendMessageRequest sendMessageStandardQueue = SendMessageRequest.builder()
                                    .queueUrl(RESIDENT_NOTIFY_UPWARD_SQS_QUEUE_URL)
                                    .messageBody(registartionInfo.toString())
                                    .build();
                            sqsClient.sendMessage(sendMessageStandardQueue);
                            System.out.println("====>>> DATE: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "  ====>>> POSTED MESSAGE: " + registartionInfo);

                            break;
//                        } else {
//                            registartionInfo = "";
                        }

                    }
//                        if (registartionInfo.isBlank()) {
//                            registartionInfo = new JSONObject()
//                                    .put("id", parsedMessage.get("id").toString())
//                                    .put("hasInsurance", false).toString();
//                            System.out.printf("====>>> Patient with ID: " + parsedMessage.get("id").toString() + " " + policyCheck + "%n");
//                        }



                    // SEND MESSAGE TO SQS

                }

            } catch (IOException ex) {
                System.out.println("====>>> IOException exception occurred: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("====>>> Exception occurred: " + ex.getMessage());
                throw ex;
            }
        }

    }

    private static void initSQSClient() {
        try {
            sqsClient = SqsClient.builder()
                    .region(Region.US_EAST_1).build();
        } catch (Exception ex) {
            System.out.println("exception " + ex.getMessage());
        }

    }


    public static void getDB(String filePath) {
        try {
            // Read all bytes from the file and convert to a string
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);

            XmlMapper xmlMapper = new XmlMapper();
            dataFromXML = xmlMapper.readValue(fileContent, dmv.class);
            System.out.println("====>>> Mapped XML DB CONTENT: " + dataFromXML.toString());

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }

    }




}

