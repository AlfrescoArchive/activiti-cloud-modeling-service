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

package org.activiti.cloud.services.organization.validation.process;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.services.organization.validation.DNSNameValidator;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of {@link BpmnModelValidator} for validating process name
 */
public class BpmnModelNameValidator implements BpmnModelValidator,
                                               DNSNameValidator {

    @Override
    public Stream<ModelValidationError> validate(BpmnModel bpmnModel,
                                                 ValidationContext validationContext) {
        return validateDNSName(Optional.ofNullable(bpmnModel)
                                       .map(BpmnModel::getMainProcess)
                                       .map(Process::getName)
                                       .orElse(null),
                               "process");
    }
}
