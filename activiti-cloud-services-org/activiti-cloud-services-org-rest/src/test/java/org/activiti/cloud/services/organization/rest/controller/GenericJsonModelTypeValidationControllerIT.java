package org.activiti.cloud.services.organization.rest.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.asserts.AssertResponse.assertThatResponse;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Collections;

import org.activiti.cloud.organization.api.JsonModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelContentValidator;
import org.activiti.cloud.organization.api.ModelExtensionsValidator;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
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
 * Integration tests for models rest api dealing with JSON models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class GenericJsonModelTypeValidationControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @SpyBean(name = "genericJsonExtensionsValidator")
    ModelExtensionsValidator genericJsonExtensionsValidator;

    @SpyBean(name = "genericJsonContentValidator")
    ModelContentValidator genericJsonContentValidator;

    @Autowired
    JsonModelType genericJsonModelType;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private Model genericJsonModel;

    @Before
    public void setUp() {
        webAppContextSetup(context);
        genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                       genericJsonModelType.getName()));
    }

    private void validateInvalidContent() {
        SemanticModelValidationException exception = new SemanticModelValidationException(Collections
                .singletonList(genericJsonContentValidator.createModelValidationError("Content invalid",
                                                                                      "The content is invalid!!")));

        doThrow(exception).when(genericJsonContentValidator).validateModelContent(Mockito.any(byte[].class),
                                                                                  Mockito.any(ValidationContext.class));
    }

    private void validateInvalidExtensions() {
        SemanticModelValidationException exception = new SemanticModelValidationException(Collections
                .singletonList(genericJsonContentValidator.createModelValidationError("Extensions invalid",
                                                                                      "The extensions are invalid!!")));

        doThrow(exception).when(genericJsonExtensionsValidator).validateModelExtensions(Mockito.any(byte[].class),
                                                                                        Mockito.any(ValidationContext.class));
    }

    @Test
    public void testValidateModelContent() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        given().multiPart("file",
                          "simple-model.json",
                          fileContent,
                          "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate",
                      genericJsonModel.getId())
                //THEN
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateModelContentTextContentType() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        given().multiPart("file",
                          "simple-model.json",
                          fileContent,
                          "text/plain")
                //WHEN
                .post("/v1/models/{modelId}/validate",
                      genericJsonModel.getId())
                //THEN
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateInvalidModelContent() throws IOException {
        //GIVEN
        this.validateInvalidContent();

        byte[] fileContent = resourceAsByteArray("generic/model-simple.json");

        assertThatResponse(given().multiPart("file",
                                             "invalid-simple-model.json",
                                             fileContent,
                                             "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate",
                      genericJsonModel.getId())
                //THEN
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("Content invalid");

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(0))
                .validateModelExtensions(Mockito.any(),
                                         Mockito.any());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(1))
                .validateModelContent(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                      Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateModelValidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        given().multiPart("file",
                          "simple-model-extensions.json",
                          fileContent,
                          "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                //THEN
                .then().expect(status().isNoContent()).body(isEmptyString());

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateModelInvalidExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                //THEN
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("required key [id] not found");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateModelInvalidJsonExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-json-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                //THEN
                .then().log().all().expect(status().isBadRequest())).isSyntacticValidationException()
                        .hasValidationErrors("org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateModelInvalidTypeExtensions() throws IOException {
        byte[] fileContent = resourceAsByteArray("generic/model-simple-invalid-type-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                //THEN
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("expected type: String, found: Boolean");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }

    @Test
    public void testValidateModelInvalidSemanticExtensions() throws IOException {
        this.validateInvalidExtensions();

        byte[] fileContent = resourceAsByteArray("generic/model-simple-valid-extensions.json");

        assertThatResponse(given().multiPart("file",
                                             "simple-model-extensions.json",
                                             fileContent,
                                             "application/json")
                //WHEN
                .post("/v1/models/{modelId}/validate/extensions",
                      genericJsonModel.getId())
                //THEN
                .then().log().all().expect(status().isBadRequest())).isSemanticValidationException().hasValidationErrors("Extensions invalid");

        Mockito.verify(genericJsonContentValidator,
                       Mockito.times(0))
                .validateModelContent(Mockito.any(),
                                      Mockito.any());

        Mockito.verify(genericJsonExtensionsValidator,
                       Mockito.times(1))
                .validateModelExtensions(Mockito.argThat(content -> new String(content).equals(new String(fileContent))),
                                         Mockito.argThat(context -> !context.isEmpty()));
    }
}
