package org.DmvService.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @JacksonXmlProperty(isAttribute = true)
    String plate;
    String make;
    String model;
    String color;
    Owner owner;
}
