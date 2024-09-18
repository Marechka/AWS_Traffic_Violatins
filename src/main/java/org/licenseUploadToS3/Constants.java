package org.licenseUploadToS3;

import java.util.ArrayList;
import java.util.Random;

public  class Constants {

    private final String[] violationType = new String[] {"no_stop", "no_full_stop_on_right", "no_right_on_red"};
    private final String[] addresses = new String[] { "2222 157th Ave SE and 7th Ave, Redlands CA ", "1111 Cool str and 38th Ave SE, Yucaipa CA", "4444 Bulka str and 8th Ave SE, Costa Mesa CA" };



    public  String getRandomAddress() {
        int index = new Random().nextInt(addresses.length);
        return addresses[index];
    }
    public  String getRandomViolation() {
        int index = new Random().nextInt(violationType.length);
        return violationType[index];
    }

}
