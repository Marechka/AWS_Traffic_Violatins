package lambdaFunctions.plateReader;

import com.amazonaws.services.eventbridge.AmazonEventBridge;
import com.amazonaws.services.eventbridge.AmazonEventBridgeClientBuilder;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.json.JSONObject;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LambdaHandler implements RequestHandler<S3Event, String> {

    private AmazonS3 s3client;
    private AmazonSQS sqsClient;
    private AmazonEventBridge eventBridgeClient;
    private static final String SQS_DOWNWARD_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/945076052403/CaliLicenseDownwardQ";
    private static final Map<String, String> violations = Map.of("no_stop", "$300.00", "no_full_stop_on_right", "$75.00", "no_right_on_red", "$125.00" );

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        String bucketName = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String fileName = s3Event.getRecords().get(0).getS3().getObject().getKey();
        try {
            initS3Client();
            String contentType  = s3client.getObject(bucketName, fileName).getObjectMetadata().getContentType();
            Map<String, String> metadata  = s3client.getObject(bucketName, fileName).getObjectMetadata().getUserMetadata();
            JSONObject messageBody = new JSONObject();
            //populate data with common fields
            for (String key : metadata.keySet()) {
                messageBody.put(key, metadata.get(key));
            }
            String addedFine = violations.get(metadata.get("type"));
            messageBody.put("fine",addedFine);



            context.getLogger().log("====>>> FILE NAME ::: " + fileName +  ", BUCKET NAME ::: " + bucketName);
            context.getLogger().log("====>>> METADATA ::: " + metadata);
            context.getLogger().log("====>>> CONTENT TYPE ::: " + contentType);

            // identify if plate belongs to California or different state
            AmazonRekognition rekognitionClient = new AmazonRekognitionClient();
            DetectTextRequest detectTextRequest = new DetectTextRequest()
                    .withImage(new Image().withS3Object(new S3Object().withName(fileName).withBucket(bucketName)));

//            try {
                DetectTextResult detectTextResponse = rekognitionClient.detectText(detectTextRequest);
                System.out.println("Detected lines and words for " + fileName);
                boolean caliStateFlag = false;
                boolean caliLicenseNumber = false;
                boolean caliRegistration = false;


            int count = 0;
                for (TextDetection text : detectTextResponse.getTextDetections() ) {
//
//                    context.getLogger().log("====>>> COUNT ::: " + count);
//
//                    context.getLogger().log("====>>> TEXT ::: " + text.getDetectedText());
//                    context.getLogger().log("====>>> TYPE ::: " + text.getType());

                    // check if California: contains "california" + string of capital A-Z and digits 0-9
                    String textFragment = text.getDetectedText();
//                    if (textFragment.length() >= 7 && count != 0) {

                    if (textFragment.length() >= 7) {
                        if (!caliStateFlag && textFragment.contains("California")) {
                            caliStateFlag = true;

                        } else if (textFragment.length() == 7 && isValidString(textFragment)) {
//                            System.out.println("DETECTED caliLicenceNumber( 7 characters) " + textFragment);
                            messageBody.put("plate", textFragment);

                            caliLicenseNumber = true;
                        }
//                        System.out.println("DETECTED text (trying to find state name)  " + textFragment + " California flag: " + caliStateFlag);
//                        System.out.println("DETECTED text length   " + textFragment.length() + " !cali flag: " + !caliStateFlag);
//                        System.out.println("regex match: " + textFragment.matches("^[a-zA-Z ]*$"));

                        if (caliStateFlag && caliLicenseNumber) {
                            caliRegistration = true;
//                            System.out.println("DETECTED total flag caliRegistration  " + caliRegistration);

                            break;
                        }
                    }
                    if (count == 0 && textFragment.length() > 3 && !caliStateFlag && textFragment.matches("^[a-zA-Z ]*$")) {

                            messageBody.put("state", textFragment);
//                            System.out.println("ADD NOT CALI STATE  " + messageBody.toString());
                    } else if (count >= 1 && count <= 2 && !caliStateFlag) {
                        if (count == 1 && textFragment.length() <= 4 ) {
                            messageBody.put("plate", textFragment);
//                            System.out.println("ADD NOT CALI PLATE 1  " + messageBody.toString());
                        } else if (count > 1 && textFragment.length() > 2){
                            messageBody.put("plate", messageBody.get("plate") + textFragment);
//                            System.out.println("ADD NOT CALI PLATE 2  " + messageBody.toString());
                            break;
                        }
                    }
                    count++;

                }


            // send to Cali DMV server for processing via SQS
            if (caliRegistration) {

                // Send the message to SQS
                initSQSClient();
                SendMessageRequest sendMessageRequest = new SendMessageRequest()
                        .withQueueUrl(SQS_DOWNWARD_QUEUE_URL)
                        .withMessageBody(messageBody.toString());
                sqsClient.sendMessage(sendMessageRequest);

                context.getLogger().log("====>>> Message sent to Downward SQS: ::: " + messageBody);
            } else {
                // send to Event Bridge if plates are from outside Cali
                initEventBridgeClient();
                PutEventsRequestEntry requestEntry = new PutEventsRequestEntry()
                        .withSource("PlateReaderFunction")
                        .withEventBusName("OutsideOfState")
                        .withDetailType("CustomEvent")
                        .withDetail(messageBody.toString());

                PutEventsRequest request = new PutEventsRequest()
                        .withEntries(requestEntry);
                PutEventsResult result  = eventBridgeClient.putEvents(request);
                context.getLogger().log("====>>> Message sent to EVENT BRIDGE ::: " + messageBody);
                context.getLogger().log("====>>> RESULT INSERT EVENT ::: " + result);
            }

        } catch (Exception ex) {
            context.getLogger().log("====>>> Exception occurred ::: " + ex.getMessage());
            return "Exception occurred  :::" + ex.getMessage();

        }
            return null;
    }

    private void initS3Client() {
        try {
            s3client = AmazonS3ClientBuilder
                    .standard()
                    .build();
        } catch(Exception ex){
        System.out.println("exception " + ex.getMessage());
        }

    }


    private void initSQSClient() {
        try {
            sqsClient = AmazonSQSClientBuilder.defaultClient();
        } catch(Exception ex){
            System.out.println("exception " + ex.getMessage());
        }

    }
    private void initEventBridgeClient() {
        try {
            eventBridgeClient = AmazonEventBridgeClientBuilder.defaultClient();
        } catch(Exception ex){
            System.out.println("exception " + ex.getMessage());
        }

    }


    private static boolean isValidString(String input) {
        String regex = "^[0-9]*([A-Z][0-9]*){3}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        return matcher.matches();
    }


}











