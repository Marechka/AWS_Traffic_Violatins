package lambdaFunctions.ticketEmail;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lambdaFunctions.ticketEmail.entity.NotificationInfo;

import java.io.IOException;

public class LambdaHandler implements RequestHandler<SQSEvent, String> {

    private AmazonSQS sqsClient;
    private AmazonSNS snsClient;
    private static final String UPWARD_SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/945076052403/ViolationNotifyResident";

    String topicArn = "arn:aws:sns:us-east-1:945076052403:ViolationNotification";


    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {

        try {
            initSQSClient();
            initSNSClient();
            for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
                // Retrieve the message body
                String messageBody = message.getBody();

                // Parse the message
                ObjectMapper objectMapper = new ObjectMapper();
                NotificationInfo notification = objectMapper.readValue(messageBody, NotificationInfo.class);
                context.getLogger().log("====>>> JSON SQS BODY: " + notification.toString());

                // Extract receiver's email, compose Letter
                String emailAddress = notification.getContact();
                String messageBodyLetter = composeLetter(notification);


//                // Create a subscription request
//                SubscribeRequest subscribeRequest = new SubscribeRequest(topicArn, "email", emailAddress);
//
//                // Subscribe the email address to the topic
//                SubscribeResult subscribeResult = snsClient.subscribe(subscribeRequest);
//
//                // Print the subscription ARN
//                context.getLogger().log("====>>> Subscription ARN: " + subscribeResult.getSubscriptionArn());

                // Publish the message to the SNS topic
                PublishRequest publishRequest = new PublishRequest(topicArn, messageBodyLetter);
                snsClient.publish(publishRequest);





                // Delete the processed message from the queue
                String receiptHandle = message.getReceiptHandle();
                DeleteMessageRequest deleteRequest = new DeleteMessageRequest()
                        .withQueueUrl(UPWARD_SQS_QUEUE_URL)
                        .withReceiptHandle(receiptHandle);
                sqsClient.deleteMessage(deleteRequest);
            }


        } catch (IOException ex) {
            context.getLogger().log("====>>> IOException exception occurred: " + ex.getMessage());
        } catch (Exception ex) {
            context.getLogger().log("====>>> Exception occurred: " + ex.getMessage());

    }
        return null;
    }


    private String composeLetter(NotificationInfo notification) {
        String vehicle = notification.getColor() + " " + notification.getMake() + " " + notification.getModel();
        String intro = "Dear " + notification.getOwnerName() + ". \nYour vehicle was involved in a traffic violation. Please pay the specified ticket amount by 30 days: \n";
        String formattedTicket = "Vehicle: " + vehicle
                + "\nLicense plate: " + notification.getPlate()
                + "\nDate: " + notification.getDatetime()
                + "\nViolation address: " + notification.getLocation()
                + "\nViolation type: " + notification.getType()
                + "\nTicket amount: " + notification.getFine();
        return intro + formattedTicket;

    }


    private void initSQSClient() {
        try {
            sqsClient = AmazonSQSClientBuilder.defaultClient();
        } catch(Exception ex){
            System.out.println("exception " + ex.getMessage());
        }

    }

    private void initSNSClient() {
        try {
            snsClient = AmazonSNSClientBuilder.defaultClient();
        } catch(Exception ex){
            System.out.println("exception " + ex.getMessage());
        }

    }


}











