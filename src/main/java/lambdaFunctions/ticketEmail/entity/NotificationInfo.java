package lambdaFunctions.ticketEmail.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInfo {

    String plate;
    String make;
    String model;
    String color;
    String ownerName;
    String contact;
    String location;
    String datetime;
    String type;
    String fine;

}
