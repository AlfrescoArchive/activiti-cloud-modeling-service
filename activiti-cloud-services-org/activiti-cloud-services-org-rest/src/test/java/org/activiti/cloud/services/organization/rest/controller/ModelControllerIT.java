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

package org.activiti.cloud.services.organization.rest.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.ConstantsBuilder.constantsFor;
import static org.activiti.cloud.services.organization.mock.IsObjectEquals.isBooleanEquals;
import static org.activiti.cloud.services.organization.mock.IsObjectEquals.isDateEquals;
import static org.activiti.cloud.services.organization.mock.IsObjectEquals.isIntegerEquals;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.extensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.multipartExtensionsFile;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.mock.MockMultipartRequestBuilder.putMultipart;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.ConnectorModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.security.WithMockModelerUser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class ModelControllerIT {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ModelRepository modelRepository;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_returnAllProjectModels_when_gettingProjectModels() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("parent-project"));

        modelRepository.createModel(processModel(project,
                                                 "Process Model 1"));
        modelRepository.createModel(processModel(project,
                                                 "Process Model 2"));

        final ResultActions resultActions = mockMvc
                .perform(get("{version}/projects/{projectId}/models?type=PROCESS",
                             API_VERSION,
                             project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is("Process Model 1")))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is("Process Model 2")));
    }

    @Test
    public void should_returnStatusCreatedAndProcessModelDetails_when_creatingProcessModel() throws Exception {
        Project project = projectRepository.createProject(project("parent-project"));

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("process-model"))))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    equalTo("process-model")))
                .andExpect(jsonPath("$.type",
                                    equalTo(PROCESS)))
                .andExpect(jsonPath("$.extensions.properties",
                                    notNullValue()))
                .andExpect(jsonPath("$.extensions.mappings",
                                    notNullValue()));
    }

    @Test
    public void should_returnStatusCreatedAndConnectorModelDetails_when_creatingConnectorModel() throws Exception {
        Project project = projectRepository.createProject(project("parent-project"));

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(connectorModel("connector-model"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    equalTo("connector-model")))
                .andExpect(jsonPath("$.type",
                                    equalTo(ConnectorModelType.NAME)))
                .andExpect(jsonPath("$",
                                    hasNoJsonPath("extensions")));
    }

    @Test
    public void should_returnStatusCreatedAndProcessModelExtensions_when_creatingProcessModelWithExtensions() throws Exception {
       Project project = projectRepository.createProject(project("parent-project"));

        Extensions extensions = extensions("ServiceTask",
                                           "variable1",
                                           "variable2");
        extensions.setConstants(constantsFor("ServiceTask")
                                        .add("myStringConstant",
                                             "myStringConstantValue")
                                        .add("myIntegerConstant",
                                             10)
                                        .build());
        ModelEntity processModel = processModelWithExtensions("process-model-extensions",
                                                              extensions);
        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extensions.properties",
                                    allOf(hasKey("variable1"),
                                          hasKey("variable2"))))
                .andExpect(jsonPath("$.extensions.mappings",
                                    hasEntry(equalTo("ServiceTask"),
                                             allOf(hasEntry(equalTo("inputs"),
                                                            allOf(hasKey("variable1"),
                                                                  hasKey("variable2"))),
                                                   hasEntry(equalTo("outputs"),
                                                            allOf(hasKey("variable1"),
                                                                  hasKey("variable2")))
                                             ))

                )).andExpect(jsonPath("$.extensions.constants",
                                      hasEntry(equalTo("ServiceTask"),
                                               hasEntry(
                                                       equalTo("myStringConstant"),
                                                       hasEntry("value",
                                                                "myStringConstantValue")
                                               )

                                      )))
                .andExpect(jsonPath("$.extensions.constants",
                                    hasEntry(equalTo("ServiceTask"),
                                             hasEntry(
                                                     equalTo("myIntegerConstant"),
                                                     hasEntry("value",
                                                              10)
                                             )

                                    )));
    }

    @Test
    public void should_throwBadRequestException_when_creatingModelOfUnknownType() throws Exception {
        Project project = projectRepository.createProject(project("parent-project"));

        Model formModel = new ModelEntity("name",
                                          "FORM");

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(formModel)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_throwConflictException_when_CreatingModelWithExistingName() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("parent-project"));
        modelRepository.createModel(processModel(project,
                                                 "process-model"));

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("process-model"))))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    public void should_returnStatusOk_when_gettingAnExistingModel() throws Exception {
        Model processModel = modelRepository.createModel(processModel("process-model"));

        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void should_returnStatusOkAndExtensions_when_gettingAnExistingModelWithExtensions() throws Exception {
        Model processModel = modelRepository
                .createModel(processModelWithExtensions("process-model-with-extensions",
                                                        extensions("ServiceTask",
                                                                   "stringVariable",
                                                                   "integerVariable",
                                                                   "booleanVariable",
                                                                   "dateVariable",
                                                                   "jsonVariable")));
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extensions.properties",
                                    allOf(hasEntry(equalTo("stringVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("stringVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("stringVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("string")),
                                                         hasEntry(equalTo("value"),
                                                                  equalTo("stringVariable"))
                                                   )),
                                          hasEntry(equalTo("integerVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("integerVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("integerVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("integer")),
                                                         hasEntry(equalTo("value"),
                                                                  isIntegerEquals(15)))),
                                          hasEntry(equalTo("booleanVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("booleanVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("booleanVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("boolean")),
                                                         hasEntry(equalTo("value"),
                                                                  isBooleanEquals(true)))),
                                          hasEntry(equalTo("dateVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("dateVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("dateVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("date")),
                                                         hasEntry(equalTo("value"),
                                                                  isDateEquals(0)))),
                                          hasEntry(equalTo("jsonVariable"),
                                                   allOf(hasEntry(equalTo("id"),
                                                                  equalTo("jsonVariable")),
                                                         hasEntry(equalTo("name"),
                                                                  equalTo("jsonVariable")),
                                                         hasEntry(equalTo("type"),
                                                                  equalTo("json")),
                                                         hasEntry(equalTo("value"),
                                                                  isJson(withJsonPath("json-field-name")))))
                                    )))
                .andExpect(jsonPath("$.extensions.mappings",
                                    hasEntry(equalTo("ServiceTask"),
                                             allOf(hasEntry(equalTo("inputs"),
                                                            allOf(hasEntry(equalTo("stringVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("stringVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("integerVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("integerVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("booleanVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("booleanVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("dateVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("dateVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("jsonVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("jsonVariable"))
                                                                           )))),
                                                   hasEntry(equalTo("outputs"),
                                                            allOf(hasEntry(equalTo("stringVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("stringVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("integerVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("integerVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("booleanVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("booleanVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("dateVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("dateVariable"))
                                                                           )),
                                                                  hasEntry(equalTo("jsonVariable"),
                                                                           allOf(hasEntry(equalTo("type"),
                                                                                          equalTo("value")),
                                                                                 hasEntry(equalTo("value"),
                                                                                          equalTo("jsonVariable"))
                                                                           ))))
                                             ))
                ));
    }

    @Test
    public void should_returnStatusOk_when_creatingProcessModelInProject() throws Exception {
        Project parentProject = projectRepository.createProject(project("parent-project"));

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             API_VERSION,
                             parentProject.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("process-model"))))
                .andExpect(status().isCreated());
    }

    @Test
    public void should_returnStatusOk_when_updatingModel() throws Exception {
        Model processModel = modelRepository.createModel(processModel("process-model"));

        mockMvc.perform(put("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel("new-process-model"))))
                .andExpect(status().isOk());

        Optional<Model> optionalModel = modelRepository.findModelById(processModel.getId());
        assertThat(optionalModel).hasValueSatisfying(
                model -> assertThat(model.getName()).isEqualTo("new-process-model")
        );
    }

    @Test
    public void should_returnStatusOk_when_updatingModelWithExtensions() throws Exception {
        ModelEntity processModel = processModelWithExtensions("process-model-extensions",
                                                              extensions("ServiceTask",
                                                                         "variable1"));
        modelRepository.createModel(processModel);

        ModelEntity newModel = processModelWithExtensions("process-model-extensions",
                                                          extensions("variable2",
                                                                     "variable3"));
        mockMvc.perform(put("{version}/models/{modelId}",
                            API_VERSION,
                            processModel.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(newModel)))
                .andExpect(status().isOk());
    }

    @Test
    public void should_returnStatusNoContent_when_deletingModel() throws Exception {
        Model processModel = modelRepository.createModel(processModel("process-model"));

        mockMvc.perform(delete("{version}/models/{modelId}",
                               API_VERSION,
                               processModel.getId()))
                .andExpect(status().isNoContent());

        assertThat(modelRepository.findModelById(processModel.getId())).isEmpty();
    }

    @Test
    public void should_returnExistingModelTypes_when_gettingModelTypes() throws Exception {

        mockMvc.perform(get("{version}/model-types",
                            API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.model-types",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.model-types[0].name",
                                    is(PROCESS)))
                .andExpect(jsonPath("$._embedded.model-types[1].name",
                                    is(ConnectorModelType.NAME)));
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessModelWithValidContent() throws Exception {
        byte[] validContent = resourceAsByteArray("process/x-19022.bpmn20.xml");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "process.xml",
                                                       CONTENT_TYPE_XML,
                                                       validContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId())
                                 .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_throwBadRequestException_when_validatingProcessModelWithInvalidContent() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       CONTENT_TYPE_XML,
                                                       "BPMN diagram".getBytes());
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId())
                                 .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContent() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
                processModelWithExtensions(project,
                                           "process-model",
                                           new Extensions(),
                                           resourceAsByteArray("process/x-19022.bpmn20.xml")));
        MockMultipartFile file = multipartExtensionsFile(
                processModel,
                resourceAsByteArray("process-extensions/valid-extensions.json"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate/extensions",
                                  RepositoryRestConfig.API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingProcessExtensionsWithValidContentAndNoValues() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions(),
                                                                                    resourceAsByteArray("process/x-19022.bpmn20.xml")));

        MockMultipartFile file = multipartExtensionsFile(
                processModel,
                resourceAsByteArray("process-extensions/valid-extensions-no-value.json"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate/extensions",
                                  RepositoryRestConfig.API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidMappingContent() throws Exception {

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(
                processModelWithExtensions(project,
                                           "process-model",
                                           new Extensions(),
                                           resourceAsByteArray("process/x-19022.bpmn20.xml")));
        MockMultipartFile file = multipartExtensionsFile(
                processModel,
                resourceAsByteArray("process-extensions/invalid-mapping-extensions.json"));

        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());
        assertThat(resultActions.andReturn().getResponse().getErrorMessage()).isEqualTo("#: #: only 1 subschema matches out of 2");

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .hasSize(2)
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsOnly(tuple("inputds is not a valid enum value",
                                    "#/extensions/mappings/ServiceTask_06crg3b/inputds: inputds is not a valid enum value"),
                              tuple("outputss is not a valid enum value",
                                    "#/extensions/mappings/ServiceTask_06crg3b/outputss: outputss is not a valid enum value"));
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidStringVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-string-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: String, found: Integer",
                                       "Mismatch value type - stringVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is string"));
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidIntegerVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-integer-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: Number, found: String",
                                       "Mismatch value type - integerVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is integer"));
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidBooleanVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-boolean-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: Boolean, found: Integer",
                                       "Mismatch value type - booleanVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is boolean"));
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidObjectVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-object-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(tuple("expected type: JSONObject, found: Integer",
                                       "Mismatch value type - objectVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is json"));
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidDateVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-date-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;
        assertThat(semanticModelValidationException.getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsExactly(
                        tuple("expected type: String, found: Integer",
                              "Mismatch value type - dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68). Expected type is date"),
                        tuple("string [aloha] does not match pattern ^[0-9]{4}-(((0[13578]|(10|12))-(0[1-9]|[1-2][0-9]|3[0-1]))|(02-(0[1-9]|[1-2][0-9]))|((0[469]|11)-(0[1-9]|[1-2][0-9]|30)))$",
                              "Invalid date - dateVariable(c297ec88-0ecf-4841-9b0f-2ae814957c68)")
                );
    }

    @Test
    public void should_thowBadRequestException_when_validatingProcessExtensionsWithInvalidDateTimeVariableContent() throws Exception {

        byte[] invalidContent = resourceAsByteArray("process-extensions/invalid-datetime-variable-extensions.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "extensions.json",
                                                       CONTENT_TYPE_JSON,
                                                       invalidContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModelWithExtensions(project,
                                                                                    "process-model",
                                                                                    new Extensions()));
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate/extensions",
                                   RepositoryRestConfig.API_VERSION,
                                   processModel.getId()).file(file))
                .andDo(print());
        resultActions.andExpect(status().isBadRequest());

        final Exception resolvedException = resultActions.andReturn().getResolvedException();
        assertThat(resolvedException).isInstanceOf(SemanticModelValidationException.class);

        SemanticModelValidationException semanticModelValidationException = (SemanticModelValidationException) resolvedException;

        assertThat(semanticModelValidationException.getValidationErrors())
            .extracting(ModelValidationError::getProblem, ModelValidationError::getDescription)
            .containsExactlyInAnyOrder(
                tuple("expected type: String, found: Integer",
                        "Mismatch value type - case4(e0740a3a-fec4-4ee5-bece-61f39df2a47k). Expected type is datetime"),
                tuple("string [2019-12-06T00:60:00] does not match pattern ^((19|20)[0-9][0-9])[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][0-9]|[2][0-3])[:]([0-5][0-9])[:]([0-5][0-9])([+|-]([01][0-9]|[2][0-3])[:]([0-5][0-9])){0,1}$",
                    "Invalid datetime - case4(e0740a3a-fec4-4ee5-bece-61f39df2a47g)"),
                tuple("string [2019-12-06T00:00:60] does not match pattern ^((19|20)[0-9][0-9])[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][0-9]|[2][0-3])[:]([0-5][0-9])[:]([0-5][0-9])([+|-]([01][0-9]|[2][0-3])[:]([0-5][0-9])){0,1}$",
                    "Invalid datetime - case4(e0740a3a-fec4-4ee5-bece-61f39df2a47f)"),
                tuple("string [2019-12-06T24:00:00] does not match pattern ^((19|20)[0-9][0-9])[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[T]([01][0-9]|[2][0-3])[:]([0-5][0-9])[:]([0-5][0-9])([+|-]([01][0-9]|[2][0-3])[:]([0-5][0-9])){0,1}$",
                    "Invalid datetime - case4(e0740a3a-fec4-4ee5-bece-61f39df2a47e)")
            );
    }

    @Test
    public void should_thowNotFoundException_when_validaingeModelThatNotExistsShouldThrowException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  RepositoryRestConfig.API_VERSION,
                                  "model_id")
                                .file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_thowBadRequestException_when_validatingInvalidProcessModelUsingTextContentType() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpmn20.xml",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model processModel = modelRepository.createModel(processModel(project,
                                                                      "process-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  processModel.getId())
                                .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorValidContent() throws Exception {
        byte[] validContent = resourceAsByteArray("connector/connector-simple.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "connector-simple.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model connectorModel = modelRepository.createModel(connectorModel(project,
                                                                          "connector-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  connectorModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnStatusNoContent_when_validatingConnectorValidContentWithTemplate() throws Exception {
        byte[] validContent = resourceAsByteArray("connector/connector-template.json");
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "connector-template.json",
                                                       CONTENT_TYPE_JSON,
                                                       validContent);

        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-test"));
        Model connectorModel = modelRepository.createModel(connectorModel(project,
                                                                          "connector-model"));

        mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                  API_VERSION,
                                  connectorModel.getId())
                                .file(file))
                .andExpect(status().isNoContent());
    }

    @Test
    public void should_returnProcessFile_when_exportingProcessModel() throws Exception {
        Model processModel = modelRepository.createModel(processModelWithContent("process_model_id",
                                                                                 "Process Model Content"));
        MvcResult response = mockMvc.perform(
                get("{version}/models/{modelId}/export",
                    API_VERSION,
                    processModel.getId()))
                .andExpect(status().isOk())
                .andReturn();

        assertThatResponseContent(response)
                .isFile()
                .hasName("process_model_id.bpmn20.xml")
                .hasContentType(CONTENT_TYPE_XML)
                .hasContent("Process Model Content");
    }

    @Test
    public void should_throwNotFoundException_when_exportingNotExistingModel() throws Exception {
        mockMvc.perform(
                get("{version}/models/not_existing_model/export",
                    API_VERSION))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void should_returnStatusCreated_when_importingProcessModel() throws Exception {
        Project parentProject = projectRepository.createProject(project("parent-project"));

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.bpmn20.xml",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        mockMvc.perform(multipart("{version}/projects/{projectId}/models/import",
                                  API_VERSION,
                                  parentProject.getId())
                                .file(zipFile)
                                .param("type",
                                       PROCESS)
                                .accept(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    public void should_throwBadRequestException_when_importingModelWrongFileName() throws Exception {
        Project parentProject = project("parent-project");
        projectRepository.createProject(parentProject);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        mockMvc.perform(multipart("{version}/projects/{projectId}/models/import",
                                  API_VERSION,
                                  parentProject.getId())
                                .file(zipFile)
                                .param("type",
                                       PROCESS)
                                .accept(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("Unexpected extension was found for file to import model of type PROCESS: x-19022")));
    }

    @Test
    public void should_throwBadRequestException_when_importingModelWrongModelType() throws Exception {
        Project parentProject = project("parent-project");
        projectRepository.createProject(parentProject);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.xml",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        mockMvc.perform(multipart("{version}/projects/{projectId}/models/import",
                                  API_VERSION,
                                  parentProject.getId())
                                .file(zipFile)
                                .param("type",
                                       "WRONG_TYPE"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("Unknown model type: WRONG_TYPE")));
    }

    @Test
    public void should_throwNotFoundException_when_importingModelFromNotExistingProject() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.xml",
                                                          "project/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        mockMvc.perform(multipart("{version}/projects/not_existing_project/models/import",
                                  API_VERSION)
                                .file(zipFile)
                                .param("type",
                                       PROCESS))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_returnStatusOk_when_updatingConnectorTemplate() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("Connector With Template"));

        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     connectorModel.getId())
                                .file("file",
                                      "connector-template.json",
                                      "application/json",
                                      resourceAsByteArray("connector/connector-template.json")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            connectorModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template",
                                    is("ConnectorTemplate")));
    }

    @Test
    public void should_returnStatusOk_when_updatingConnectorCustom() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("SimpleConnector"));

        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     connectorModel.getId())
                                .file("file",
                                      "connector-simple.json",
                                      "application/json",
                                      resourceAsByteArray("connector/connector-simple.json")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            connectorModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template").doesNotExist());
    }
}
