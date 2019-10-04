/*
 * Copyright 2005-2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.activiti.cloud.services.organization.validation.extensions;

import java.util.stream.Stream;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ValidationContext;
import org.everit.json.schema.loader.SchemaLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ExtensionsJsonSchemaValidator} for validating the common extensions schema
 */
@Component
public class ExtensionsModelValidator extends ExtensionsJsonSchemaValidator {

    private final SchemaLoader modelExtensionsSchemaLoader;

    @Autowired
    public ExtensionsModelValidator(SchemaLoader modelExtensionsSchemaLoader) {
        this.modelExtensionsSchemaLoader = modelExtensionsSchemaLoader;
    }

    @Override
    public ModelType getHandledModelType() {
        return null;
    }

    @Override
    protected SchemaLoader schemaLoader() {
        return modelExtensionsSchemaLoader;
    }

    @Override
    protected Stream<ModelValidationError> validateModelExtensions(Model model,
                                                                   ValidationContext validationContext) {
        // No further validation needed
        return Stream.empty();
    }

}
