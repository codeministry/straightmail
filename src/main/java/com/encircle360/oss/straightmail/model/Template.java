package com.encircle360.oss.straightmail.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Template {

    private String id;

    private String subject;

    private String name;

    private String html;

    private String plain;

    private String locale;
}
