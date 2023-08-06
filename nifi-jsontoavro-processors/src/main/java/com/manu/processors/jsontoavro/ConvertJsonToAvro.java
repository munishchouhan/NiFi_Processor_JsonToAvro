/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.manu.processors.jsontoavro;

import org.apache.avro.Schema;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({"convert","json","avro","convertor"})
@CapabilityDescription("This processor convert data from json to avro")
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class ConvertJsonToAvro extends AbstractProcessor {

    public static final PropertyDescriptor SCHEMA= new PropertyDescriptor
            .Builder().name("Avro schema")
            .displayName("Avro schema")
            .description("Provide the schema in json format for the avro output")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder().name("REL_SUCCESS")
            .description("A Flowfile is routed to this relationship when everything goes well here").build();

    public static final Relationship REL_FAILURE = new Relationship.Builder().name("REL_FAILURE")
            .description(
                    "A Flowfile is routed to this relationship it can not be converted to pdf or a problem happens")
            .build();
    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(SCHEMA);
        this.properties = Collections.unmodifiableList(properties);
        relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        try {
            flowFile = session.write(flowFile, (rawIn, rawOut) -> {
                try (final InputStream in = new BufferedInputStream(rawIn)) {

                    // Convert input json from flow file into a avro
                    try {
                        String json = "{ \"username\": \"mike\" }";

                        JsonAvroConverter converter = new JsonAvroConverter();

                        // conversion to binary Avro
                        byte[] avro = converter.convertToAvro(json.getBytes(), (Schema) context.getProperty(SCHEMA));
                        rawOut.write(avro);
                    } catch (Exception e) {
                        throw new ProcessException("There is problem in converting file to avro, please check the input",e);
                    }
                }
            });
        } catch (ProcessException pe) {
            getLogger().error("Failed to convert: {}", new Object[] { flowFile, pe });
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/avro");
        session.transfer(flowFile, REL_SUCCESS);
    }
}
