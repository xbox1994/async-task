package com.wty.async.task.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.util.Collection;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonFactory.Feature.INTERN_FIELD_NAMES;

public class ObjectMapperUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper(new JsonFactory().disable(INTERN_FIELD_NAMES))
            .registerModule(new GuavaModule());
    public static final String EMPTY_JSON = "{}";
    public static final String EMPTY_ARRAY_JSON = "[]";

    static {
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MAPPER.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        MAPPER.enable(JsonParser.Feature.ALLOW_COMMENTS);
        MAPPER.registerModule(new ParameterNamesModule());
    }

    public static String toJSON(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJSON(String json, Class<T> valueType) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <E, T extends Collection<E>> T fromJSON(String json, Class<? extends Collection> collectionType, Class<E> valueType) {
        if (StringUtils.isEmpty(json)) {
            json = EMPTY_ARRAY_JSON;
        }
        try {
            return MAPPER.readValue(json, TypeFactory.defaultInstance().constructCollectionType(collectionType, valueType));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V, T extends Map<K, V>> T fromJSON(String json, Class<? extends Map> mapType, Class<K> keyType, Class<V> valueType) {
        if (StringUtils.isEmpty(json)) {
            json = EMPTY_JSON;
        }
        try {
            return MAPPER.readValue(json, TypeFactory.defaultInstance().constructMapType(mapType, keyType, valueType));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
