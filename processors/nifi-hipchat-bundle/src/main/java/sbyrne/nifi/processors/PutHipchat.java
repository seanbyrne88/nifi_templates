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
package sbyrne.nifi.processors;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.DataOutputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({"put", "hipchat", "atlassian", "notify"})
@CapabilityDescription("Sends a message to your team on hipchat.com")
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
public class PutHipchat extends AbstractProcessor {

    public static final String HIPCHAT_BASEURL_TEMPLATE =
            "https://api.hipchat.com/v2/room/{room_id}/notification?auth_token={auth_token}";

    public static final String RED = "red";
    public static final String GREEN = "green";
    public static final String YELLOW = "yellow";
    public static final String PURPLE = "purple";
    public static final String GRAY = "gray";
    public static final String RANDOM = "random";

    public static final PropertyDescriptor ROOM_ID = new PropertyDescriptor
            .Builder()
            .name("room-id")
            .displayName("RoomID or Name")
            .description("The RoomID or Name to send message to")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor AUTH_TOKEN = new PropertyDescriptor
            .Builder()
            .name("auth-token")
            .displayName("Auth Token")
            .description("User Auth token")
            .required(true)
            .expressionLanguageSupported(false)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MESSAGE_TEXT = new PropertyDescriptor
            .Builder()
            .name("message-text")
            .displayName("Message Text")
            .description("The text sent in the Hipchat message")
            .required(true)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MESSAGE_BG_COLOR = new PropertyDescriptor
            .Builder()
            .name("message-bg-color")
            .displayName("Message Background Color")
            .description("Color to send background message")
            .required(true)
            .allowableValues(RED, YELLOW, GREEN, GRAY, PURPLE, RANDOM)
            .build();

    public static final PropertyDescriptor FROM = new PropertyDescriptor
            .Builder()
            .name("username")
            .displayName("From Note")
            .description("The auth key will determine who the notification will show from, but this parameter will let you add an additional note after the name")
            .required(false)
            .expressionLanguageSupported(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles are routed to success after being successfully sent to Hipchat")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("FlowFiles are routed to failure if unable to be sent to Hipchat")
            .build();

    public static final List<PropertyDescriptor> descriptors = Collections.unmodifiableList(
            Arrays.asList(ROOM_ID, MESSAGE_TEXT, FROM, MESSAGE_BG_COLOR, AUTH_TOKEN));

    public static final Set<Relationship> relationships = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(REL_SUCCESS, REL_FAILURE)));

    private static String buildHipchatUrl(String base, String room, String authToken)
    {
        return base.replace("{room_id}", room).replace("{auth_token}", authToken);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {

        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

        String roomID = context.getProperty(ROOM_ID).evaluateAttributeExpressions().getValue();
        //to support room name we need to encode roomID for any spaces or other special characters used in Hipchat room name
        try
        {
            roomID = URLEncoder.encode(roomID, "UTF-8").replace("+", "%20");
        }
        catch(UnsupportedEncodingException e)
        {
            getLogger().error("Failed to encode roomID "+ roomID + "with exception ", e);
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            context.yield();
        }

        String authToken = context.getProperty(AUTH_TOKEN).getValue();
        String hipchat_url = buildHipchatUrl(HIPCHAT_BASEURL_TEMPLATE, roomID, authToken);

        JsonObjectBuilder builder = Json.createObjectBuilder();

        String message = context.getProperty(MESSAGE_TEXT).evaluateAttributeExpressions(flowFile).getValue();
        if (message != null && !message.isEmpty()) {
            builder.add("message", message);
        } else {
            // Hipchat requires a text attribute
            getLogger().error("FlowFile should have non-empty " + MESSAGE_TEXT.getName());
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        builder.add("color", context.getProperty(MESSAGE_BG_COLOR).getValue());

        String from = context.getProperty(FROM).evaluateAttributeExpressions(flowFile).getValue();
        if (from != null && !from.isEmpty()) {
            builder.add("from", from);
        }

        JsonObject jsonObject = builder.build();
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stringWriter);
        jsonWriter.writeObject(jsonObject);
        jsonWriter.close();

        try {
            URL url = new URL(hipchat_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("content-type", "application/json; charset=utf-8");

            DataOutputStream outputStream  = new DataOutputStream(conn.getOutputStream());
            String payload = stringWriter.getBuffer().toString();
            outputStream.writeBytes(payload);
            outputStream.close();

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                getLogger().info("Successfully posted message to Hipchat");
                session.transfer(flowFile, REL_SUCCESS);
                session.getProvenanceReporter().send(flowFile, context.getProperty(ROOM_ID).getValue());
            } else {
                getLogger().error("Failed to post message to Hipchat with response code {}", new Object[]{responseCode});
                getLogger().error("payload= "+ payload);
                flowFile = session.penalize(flowFile);
                session.transfer(flowFile, REL_FAILURE);
                context.yield();
            }
        } catch (IOException e) {
            getLogger().error("Failed to open connection", e);
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            context.yield();
        }
    }
}
