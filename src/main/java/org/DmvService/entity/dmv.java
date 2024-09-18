package org.DmvService.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class dmv {
    @JacksonXmlProperty(localName = "vehicle")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<Vehicle> vehicles;

}
