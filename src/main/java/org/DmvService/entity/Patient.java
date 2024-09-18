package org.DmvService.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @JacksonXmlProperty(isAttribute = true)
    String id;
    Policy policy;
//    String policyNumber;
//    String provider;


}
