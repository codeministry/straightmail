package com.encircle360.oss.straightmail.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.model.Template;

@Mapper
public interface TemplateMapper {

    TemplateMapper INSTANCE = Mappers.getMapper(TemplateMapper.class);

    @Mapping(target = "html", source = "renderedHtml")
    @Mapping(target = "plain", source = "renderedPlain")
    RenderedTemplateDTO toRendered(Template template, String renderedHtml, String renderedPlain);
}
