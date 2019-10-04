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
package org.activiti.cloud.services.organization.validation.extensions;

import org.activiti.cloud.organization.api.ModelExtensionsValidator;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.services.organization.validation.JsonSchemaModelValidator;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * The generic JSON extensions schema for all the models
 */
@Component
@ConditionalOnMissingBean(name = "ExtensionsSchemaValidator")
public class ExtensionsSchemaValidator extends JsonSchemaModelValidator implements ModelExtensionsValidator {

    private final SchemaLoader modelExtensionsSchemaLoader;

    @Autowired
    public ExtensionsSchemaValidator(SchemaLoader modelExtensionsSchemaLoader) {
        this.modelExtensionsSchemaLoader = modelExtensionsSchemaLoader;
    }

    @Override
    public void validateModelExtensions(byte[] bytes,
                                     ValidationContext validationContext) {
        super.validate(bytes,
                            validationContext);
    }

    @Override
    public ModelType getHandledModelType() {
        return null;
    }

    @Override
    public SchemaLoader schemaLoader() {
        return modelExtensionsSchemaLoader;
    }
}
