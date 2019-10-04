package org.activiti.cloud.services.organization.rest.controller;

import static org.activiti.cloud.services.organization.mock.MockMultipartRequestBuilder.putMultipart;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.activiti.cloud.organization.api.ContentUpdateListener;
import org.activiti.cloud.organization.api.JsonModelType;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.repository.ModelRepository;
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
import org.springframework.test.web.servlet.MockMvc;
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
public class GenericJsonModelTypeContentUpdateListenerControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private MockMvc mockMvc;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(context).build();
    }

    @Test
    public void testUpdateContentGenericJsonModelCallsContentUpdateListenerForTheModel() throws Exception {
        // GIVEN
        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        String stringModel = objectMapper.writeValueAsString(genericJsonModel);

        // WHEN
        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     genericJsonModel.getId()).file("file",
                                                                    "simple-model.json",
                                                                    "application/json",
                                                                    stringModel.getBytes()))
                .andExpect(status().isNoContent());

        // THEN
        Mockito.verify(genericJsonContentUpdateListener,
                       Mockito.times(1))
                .execute(Mockito.argThat(model -> model.getId().equals(genericJsonModel.getId())),
                         Mockito.argThat(content -> new String(content.getFileContent()).equals(stringModel)));
    }

    @Test
    public void testUpdateContentGenericJsonModelNotCallsContentUpdateListenerForOtherModel() throws Exception {

        // GIVEN
        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        String stringModel = objectMapper.writeValueAsString(genericJsonModel);

        // WHEN
        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     genericJsonModel.getId()).file("file",
                                                                    "simple-model.json",
                                                                    "application/json",
                                                                    stringModel.getBytes()))
                .andExpect(status().isNoContent());

        // THEN
        Mockito.verify(genericNonJsonContentUpdateListener,
                       Mockito.times(0))
                .execute(Mockito.any(),
                         Mockito.any());
    }
}
