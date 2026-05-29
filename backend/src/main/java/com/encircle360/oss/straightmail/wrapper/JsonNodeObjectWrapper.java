package com.encircle360.oss.straightmail.wrapper;

import com.encircle360.oss.straightmail.wrapper.model.*;
import com.fasterxml.jackson.databind.node.*;
import freemarker.ext.util.ModelFactory;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Version;

/**
 * Custom FreeMarker {@link DefaultObjectWrapper} that maps Jackson {@link com.fasterxml.jackson.databind.JsonNode}
 * types to their corresponding FreeMarker template model implementations.
 *
 * <p>Without this wrapper, FreeMarker cannot natively traverse Jackson's JSON node graph in templates.
 * Registered on the FreeMarker {@link freemarker.template.Configuration} in
 * {@link com.encircle360.oss.straightmail.service.FreemarkerService} before each rendering pass.
 *
 * <p>The following Jackson node types are mapped:
 * <ul>
 *   <li>{@link com.fasterxml.jackson.databind.node.TextNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonTextNodeModel}</li>
 *   <li>{@link com.fasterxml.jackson.databind.node.NumericNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonNumberNodeModel}</li>
 *   <li>{@link com.fasterxml.jackson.databind.node.BooleanNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonBooleanNodeModel}</li>
 *   <li>{@link com.fasterxml.jackson.databind.node.POJONode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonPojoNodeModel}</li>
 *   <li>{@link com.fasterxml.jackson.databind.node.ArrayNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonArrayNodeModel}</li>
 *   <li>{@link com.fasterxml.jackson.databind.node.ObjectNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonObjectNodeModel}</li>
 *   <li>{@link com.fasterxml.jackson.databind.node.NullNode} → {@link com.encircle360.oss.straightmail.wrapper.model.JsonNullNodeModel}</li>
 * </ul>
 */
public class JsonNodeObjectWrapper extends DefaultObjectWrapper {

    /**
     * Constructs the wrapper with the given FreeMarker compatibility version.
     *
     * @param incompatibleImprovements the minimum FreeMarker version to enable improvements for
     */
    public JsonNodeObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    protected ModelFactory getModelFactory(Class clazz) {
        if (TextNode.class.isAssignableFrom(clazz)) {
            return JsonTextNodeModel.FACTORY;
        } else if (NumericNode.class.isAssignableFrom(clazz)) {
            return JsonNumberNodeModel.FACTORY;
        } else if (BooleanNode.class.isAssignableFrom(clazz)) {
            return JsonBooleanNodeModel.FACTORY;
        } else if (POJONode.class.isAssignableFrom(clazz)) {
            return JsonPojoNodeModel.FACTORY;
        } else if (ArrayNode.class.isAssignableFrom(clazz)) {
            return JsonArrayNodeModel.FACTORY;
        } else if (ObjectNode.class.isAssignableFrom(clazz)) {
            return JsonObjectNodeModel.FACTORY;
        } else if (NullNode.class.isAssignableFrom(clazz)) {
            return JsonNullNodeModel.FACTORY;
        }
        return super.getModelFactory(clazz);
    }
}
