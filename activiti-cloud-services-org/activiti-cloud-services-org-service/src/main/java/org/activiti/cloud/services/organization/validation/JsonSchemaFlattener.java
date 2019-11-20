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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.ClassPathResource;
    
public class JsonSchemaFlattener {
    
    private Map<String, Object> addDefinitions = new HashMap<>();
    
    public JsonSchemaFlattener() {
         
    }
    
    private String getSectionNameFromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        return fileName
                .replaceAll(".json","")
                .replaceAll("[/-]","_")
                .replaceAll("[^a-zA-Z0-9_]+","");
    }
    
    private JSONObject handleJSONObject(JSONObject jsonObject) {
        
        JSONObject reply = null;
        
        if (!jsonObject.isEmpty()) {
            Iterator iterator = jsonObject.keys();
            
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);
                Object o = null;
                
                if (isKeyToCheck(key)) {
                    o = checkUpdateValue(value);     
                } else {
                    o = handleValue(value); 
                }
                if (o != null) {
                    jsonObject.put(key, o);
                    
                    reply = jsonObject;
                }
            }
        }

        return reply;
    }
    
    JSONArray handleJSONArray(JSONArray jsonArray) {
        JSONArray reply = null;
        
        for (int i = 0; i < jsonArray.length(); i++) {
            Object o = handleValue(jsonArray.get(i));
            if (o != null) {
                jsonArray.put(i, o);
                reply = jsonArray;
            }
        }

        return reply;
    }
    
    
    private Object handleValue(Object value) {
        if (value instanceof JSONObject) {
            return handleJSONObject((JSONObject) value);
        } else if (value instanceof JSONArray) {
            return handleJSONArray((JSONArray) value);
        } else {
            return null;
        }
    }
    
    private boolean isKeyToCheck(String key) {
        return Objects.equals("$ref", key);
    }
    
    private Object checkUpdateValue(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
         
        String s = (String)value;  
        if (s.startsWith("#")) {
            return null;
        }
        
        try {
            JSONObject o = null;
            String name = null, secName = null;
            String suffix =  null;
            
            if (s.startsWith("classpath:")) {
                name = s.substring(11);            
            } else {
                int i = s.indexOf("#/");
                if (i > 0) {
                    name = s.substring(1,i);
                    suffix = s.substring(i);
                }
            }
            
            if (name != null) {
                secName = getSectionNameFromFileName(name);
                o = (JSONObject)addDefinitions.get(secName);
                
                if (o == null) {
                    o = loadResourceFromClassPass(name);  
                    
                    if (o != null) {
                        addDefinitions.put(secName, flattenIntern(o));     
                    }
                    
                }
                
                s = "#/definitions/" + secName + (suffix != null ? suffix : "");
                return s;
             }                        
            
        } catch (IOException e) {
            
        }
  
        return null;
    }
        
    private JSONObject loadResourceFromClassPass(String schemaFileName) throws IOException  {
    
        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            return  new JSONObject(new JSONTokener(schemaInputStream));     
        }        
    }
      
    private JSONObject flattenIntern(JSONObject jsonSchema) {
        
        if (jsonSchema.isEmpty()) {
            return jsonSchema;
        }
        
        JSONObject reply = handleJSONObject(jsonSchema);  
        if (reply == null) {
            reply = jsonSchema;
        }
        return reply;
    }  
    
    public JSONObject flatten(JSONObject jsonSchema) {
        
        JSONObject reply = flattenIntern(jsonSchema);  
   
        if (!addDefinitions.isEmpty()) {
            
            JSONObject definitions = (JSONObject)reply.get("definitions");
            if (definitions == null) {
                definitions = reply.put("definitions", new JSONObject());
            }
            for (Map.Entry<String, Object> entry : addDefinitions.entrySet()) {
                definitions.put(entry.getKey(), entry.getValue());
            }
        }
        return reply;
    }  
   
}



