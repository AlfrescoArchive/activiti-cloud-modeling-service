/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.organization.core.rest.client.feign;

import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for forms REST service
 */
@FeignClient(url = "#{'${activiti.cloud.modeling.url:activiti-cloud-modeling}'}",
        name = "activiti-cloud-modeling",
        path = "/v1/forms")
public interface FormModelReferenceService extends BaseModelService<ModelReference> {

}
