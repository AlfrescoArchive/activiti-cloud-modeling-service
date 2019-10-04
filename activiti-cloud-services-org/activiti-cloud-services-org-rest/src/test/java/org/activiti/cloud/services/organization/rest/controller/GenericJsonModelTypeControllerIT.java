package org.activiti.cloud.services.organization.rest.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.mock.MockMultipartRequestBuilder.putMultipart;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.organization.api.ContentUpdateListener;
import org.activiti.cloud.organization.api.JsonModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelContentValidator;
import org.activiti.cloud.organization.api.ModelExtensionsValidator;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class GenericJsonModelTypeControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private static final String GENERIC_PROJECT_NAME = "project-with-generic-model";

    @Before
    public void setUp() {
        webAppContextSetup(context);
    }

    @Test
    public void testCreateGenericJsonModel() throws Exception {
        String name = GENERIC_MODEL_NAME;

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                        genericJsonModelType.getName())))
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                .then().expect(status().isCreated()).body("name",
                                                          equalTo(GENERIC_MODEL_NAME));
    }

    @Test
    public void testCreateGenericJsonModelInvalidPayloadNameNull() throws Exception {
        String name = null;

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                // WHEN
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                // THEN
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("field.required").hasValidationErrorMessages("The model name is required");
    }

    @Test
    public void testCreateGenericJsonModelInvalidPayloadNameEmpty() throws Exception {
        String name = "";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                // WHEN
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                // THEN
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("field.empty",
                                                                                                         "regex.mismatch")
                        .hasValidationErrorMessages("The model name cannot be empty",
                                                    "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: ''");
    }

    @Test
    public void testCreateGenericJsonModelInvalidPayloadNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                // WHEN
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                // THEN
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("length.greater",
                                                                                                         "regex.mismatch")
                        .hasValidationErrorMessages("The model name length cannot be greater than 26: '123456789_123456789_1234567'",
                                                    "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: '123456789_123456789_1234567'");
    }

    @Test
    public void testCreateGenericJsonModelInvalidPayloadNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                // WHEN
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                // THEN
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void testCreateGenericJsonModelInvalidPayloadNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        assertThatResponse(given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                                                           genericJsonModelType
                                                                                                                                                                   .getName())))
                // WHEN
                .post("/v1/projects/{projectId}/models",
                      project.getId())
                // THEN
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }

    @Test
    public void testUpdateGenericJsonModel() throws Exception {
        String name = "updated-connector-name";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                         genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().log().all().expect(status().isOk()).body("name",
                                                                 equalTo("updated-connector-name"));
    }

    @Test
    public void testUpdateGenericJsonModelInvalidPayloadNameNull() throws Exception {
        String name = null;

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                         genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isOk()).body("name",
                                                     equalTo(GENERIC_MODEL_NAME));
    }

    @Test
    public void testUpdateGenericJsonModelInvalidPayloadNameEmpty() throws Exception {
        String name = "";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("field.empty",
                                                                                                         "regex.mismatch")
                        .hasValidationErrorMessages("The model name cannot be empty",
                                                    "The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: ''");
    }

    @Test
    public void testUpdateGenericJsonModelInvalidPayloadNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: '123456789_123456789_1234567'");
    }

    @Test
    public void testUpdateGenericJsonModelInvalidPayloadNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void testUpdateGenericJsonModelInvalidPayloadNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        assertThatResponse(given().contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(new ModelEntity(name,
                                                                                                                            genericJsonModelType.getName())))
                .put("/v1/models/{modelId}",
                     genericJsonModel.getId())
                .then().expect(status().isBadRequest())).isValidationException().hasValidationErrorCodes("regex.mismatch")
                        .hasValidationErrorMessages("The model name should follow DNS-1035 conventions: it must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }

    @Test
    public void testCreateGenericJsonModelWithNullExtensions() throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));
        Map<String, Object> extensions = null;

        genericJsonModel.setExtensions(extensions);

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(genericJsonModel)).post("/v1/projects/{projectId}/models",
                                                                                                                                                project.getId())
                .then().expect(status().isCreated()).body("extensions",
                                                          nullValue());
    }

    @Test
    public void testCreateGenericJsonModelWithEmptyExtensions() throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));
        Map<String, Object> extensions = new HashMap();

        genericJsonModel.setExtensions(extensions);

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(genericJsonModel)).post("/v1/projects/{projectId}/models",
                                                                                                                                                project.getId())
                .then().expect(status().isCreated()).body("extensions",
                                                          notNullValue());
    }

    @Test
    public void testCreateGenericJsonModelWithValidExtensions() throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));
        Map<String, Object> extensions = new HashMap();
        extensions.put("string",
                       "value");
        extensions.put("number",
                       2f);
        extensions.put("array",
                       new String[] { "a", "b", "c" });
        extensions.put("list",
                       Arrays.asList("a",
                                     "b",
                                     "c",
                                     "d"));

        genericJsonModel.setExtensions(extensions);

        given().accept(APPLICATION_JSON_VALUE).contentType(APPLICATION_JSON_VALUE).body(objectMapper.writeValueAsString(genericJsonModel)).post("/v1/projects/{projectId}/models",
                                                                                                                                                project.getId())
                .then().expect(status().isCreated()).body("extensions",
                                                          notNullValue())
                .body("extensions.string",
                      equalTo("value"))
                .body("extensions.number",
                      equalTo(2f))
                .body("extensions.array",
                      org.hamcrest.Matchers.hasSize(3))
                .body("extensions.list",
                      org.hamcrest.Matchers.hasSize(4));
    }
}
