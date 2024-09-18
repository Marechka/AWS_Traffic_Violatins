package org.DmvService.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Owner {
    @JacksonXmlProperty(isAttribute = true)
    String preferredLanguage;
    String name;
    String contact;
}
