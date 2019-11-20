/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.organization.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.ClassPathResource;
    
public class JsonSchemaFlattener {
    
    private Map<String, Object> addDefinitions = new HashMap<>();
    
    public JsonSchemaFlattener() {
         
    }
    
    private Optional<JSONObject> handleJSONObject(JSONObject jsonObject) {
        
        Optional<JSONObject> reply = Optional.empty();
        
        if (!jsonObject.isEmpty()) {
            Iterator iterator = jsonObject.keys();
            
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);
                
                if (isKeyToCheck(key)) {
                    Optional<String> updatedString = getUpdatedValue(value);    
                    if (updatedString.isPresent()) {
                        jsonObject.put(key, updatedString.get());
                        
                        reply = Optional.of(jsonObject);
                    }
                    
                } else {                  
                    if (value instanceof JSONObject) {
                        Optional<JSONObject> updatedObject = handleJSONObject((JSONObject) value);
                        if (updatedObject.isPresent()) {
                            jsonObject.put(key, updatedObject.get());
                            
                            reply = Optional.of(jsonObject);
                        }
                        
                    } else if (value instanceof JSONArray) {
                        Optional<JSONArray> updatedArray = handleJSONArray((JSONArray) value);
                        if (updatedArray.isPresent()) {
                            jsonObject.put(key, updatedArray.get());
                            
                            reply = Optional.of(jsonObject);
                        }
                    } 
                }
            }
        }

        return reply;
    }
    
    Optional<JSONArray> handleJSONArray(JSONArray jsonArray) {
        Optional<JSONArray> reply = Optional.empty();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            
            if (value instanceof JSONObject) {
                Optional<JSONObject> updatedObject = handleJSONObject((JSONObject) value);
                if (updatedObject.isPresent()) {
                    jsonArray.put(i, updatedObject.get());
                    reply = Optional.of(jsonArray);
                }
                
            } else if (value instanceof JSONArray) {
                Optional<JSONArray> updatedArray = handleJSONArray((JSONArray) value);
                if (updatedArray.isPresent()) {
                    jsonArray.put(i, updatedArray.get());
                    
                    reply = Optional.of(jsonArray);
                }
            } 
        }

        return reply;
    }   
   
    
    private boolean isKeyToCheck(String key) {
        return Objects.equals("$ref", key);
    }
    
    private Optional<String> getClassPathFileName(String value) {
        String regex = "classpath:\\/\\/(.*)";
        Matcher matcher = Pattern.compile(regex)
                                 .matcher(value);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).toString());  
        }
        
        return Optional.empty();  
    }

    private Optional<String> getUpdatedValue(Object value) {
        
        Optional<String> stringValue = Optional.of(value)
                                        .filter(String.class::isInstance)
                                        .map(String.class::cast)
                                        .filter(s -> !s.startsWith("#"));
        
        if (stringValue.isPresent()) {
         
            Optional<String> stringRef = Optional.empty();
            Optional<String> fileName = getClassPathFileName(stringValue.get());
        
            if (!fileName.isPresent()) {
                String regex = "(.*)\\/#\\/(.*)";
                Matcher matcher = Pattern.compile(regex)
                                         .matcher(stringValue.get());
                if (matcher.find()) {
                    fileName = Optional.of(matcher.group(1).toString());  
                    stringRef = Optional.of(matcher.group(2).toString());
                } else {
                    fileName = Optional.of(stringValue.get());  
                }  
            }
        
            if (fileName.isPresent()) {
            
                String sectionName = getSectionNameFromFileName(fileName.get());
                JSONObject jsonObject = (JSONObject)addDefinitions.get(sectionName);
            
                if (jsonObject == null) {
                    
                    try {
                        jsonObject = loadResourceFromClassPass(fileName.get());                       
                    } catch (IOException e) {
                        jsonObject = null;;
                    }  
                }
                
                if (jsonObject != null) {
                    addDefinitions.put(sectionName, flattenIntern(jsonObject).get());   
                    return Optional.of("#/definitions/" + sectionName + (stringRef.isPresent() ? stringRef.get() : ""));   
                }
            
            } 
        }
        
        return Optional.empty();
    }
        
    private JSONObject loadResourceFromClassPass(String schemaFileName) throws IOException  {
    
        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            return  new JSONObject(new JSONTokener(schemaInputStream));     
        }        
    }
      
    private Optional<JSONObject> flattenIntern(JSONObject jsonSchema) {
        
        if (!jsonSchema.isEmpty()) {
            Optional<JSONObject> reply = handleJSONObject(jsonSchema);  
        
            if (reply.isPresent()) {
                return reply;
           
            }
        }
        return Optional.of(jsonSchema);
    }  
    
    public String getSectionNameFromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        return fileName
                .replaceAll(".json","")
                .replaceAll("[/-]","_")
                .replaceAll("[^a-zA-Z0-9_]+","");
    }
    
    public JSONObject flatten(JSONObject jsonSchema) {
        
        if (jsonSchema == null) {
            return new JSONObject();
        }
        
        addDefinitions.clear();
        Optional<JSONObject> reply = flattenIntern(jsonSchema);  
        JSONObject replyObject = reply.get();

        if (!addDefinitions.isEmpty()) {

            JSONObject definitions = null;
            if (replyObject.has("definitions")) {
                definitions = (JSONObject)replyObject.get("definitions");
            } else {
                definitions = new JSONObject();
            }
            for (Map.Entry<String, Object> entry : addDefinitions.entrySet()) {
                definitions.put(entry.getKey(), entry.getValue());
            }
            
            replyObject.put("definitions", definitions);
        }
        
        return replyObject;
    }  
   
}
