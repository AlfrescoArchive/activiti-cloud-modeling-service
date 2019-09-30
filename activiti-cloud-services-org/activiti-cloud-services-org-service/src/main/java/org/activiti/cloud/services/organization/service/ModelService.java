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

package org.activiti.cloud.services.organization.service;

import org.activiti.cloud.organization.api.*;
import org.activiti.cloud.organization.api.process.Extensions;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.core.error.ImportModelException;
import org.activiti.cloud.organization.core.error.UnknownModelTypeException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.activiti.cloud.services.organization.validation.ProjectValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.organization.api.ValidationContext.EMPTY_CONTEXT;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.*;
import static org.apache.commons.lang3.StringUtils.removeEnd;

/**
 * Business logic related to {@link Model} entities including process models, form models, connectors, data models and decision table models.
 */
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
@Transactional
public class ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    private final ModelRepository modelRepository;

    private final ModelTypeService modelTypeService;

    private final ModelContentService modelContentService;

    private final JsonConverter<Model> jsonConverter;

    private final HashMap<String, String> modelIdentifiers = new HashMap();

    @Autowired
    public ModelService(ModelRepository modelRepository,
                        ModelTypeService modelTypeService,
                        ModelContentService modelContentService,
                        JsonConverter<Model> jsonConverter) {
        this.modelRepository = modelRepository;
        this.modelTypeService = modelTypeService;
        this.modelContentService = modelContentService;
        this.jsonConverter = jsonConverter;
    }

    public List<Model> getAllModels(Project project) {
        return modelTypeService.getAvailableModelTypes().stream().map(modelType -> getModels(project,
                                                                                             modelType,
                                                                                             Pageable.unpaged()))
                .map(Page::getContent).flatMap(List::stream).collect(Collectors.toList());
    }

    public Page<Model> getModels(Project project,
                                 ModelType modelType,
                                 Pageable pageable) {
        return modelRepository.getModels(project,
                                         modelType,
                                         pageable);
    }

    public Model buildModel(String type,
                            String name) {
        try {
            Model model = (Model) modelRepository.getModelType().getConstructor().newInstance();
            model.setType(type);
            model.setName(name);
            return model;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Model createModel(Project project,
                             Model model) {
        model.setId(null);
        ModelType modelType = findModelType(model);
        model.setProject(project);
        if (model.getExtensions() == null && PROCESS.equals(modelType.getName())) {
            model.setExtensions(new Extensions());
        }
        return modelRepository.createModel(model);
    }

    public Model updateModel(Model modelToBeUpdated,
                             Model newModel) {
        return modelRepository.updateModel(modelToBeUpdated,
                                           newModel);
    }

    public void deleteModel(Model model) {
        modelRepository.deleteModel(model);
    }

    public Optional<Model> findModelById(String modelId) {
        return modelRepository.findModelById(modelId);
    }

    public void cleanModelIdList() {
        this.modelIdentifiers.clear();
    }

    public Optional<FileContent> getModelDiagramFile(String modelId) {
        //TODO: to implement
        return Optional.empty();
    }

    public String getMetadataFilename(Model model) {
        return toJsonFilename(model.getName() + findModelType(model).getMetadataFileSuffix());
    }

    public FileContent getModelContentFile(Model model) {
        return getModelFileContent(model,
                                   modelRepository.getModelContent(model));
    }

    public FileContent exportModel(Model model) {
        return getModelFileContent(model,
                                   modelRepository.getModelExport(model));
    }

    private FileContent getModelFileContent(Model model,
                                            byte[] modelBytes) {
        return new FileContent(setExtension(model.getName(),
                                            findModelType(model).getContentFileExtension()),
                               model.getContentType(),
                               modelBytes);
    }

    public Model updateModelContent(Model modelToBeUpdate,
                                    FileContent fileContent) {
        FileContent fixedFileContent = this.modelIdentifiers.isEmpty()
                ? fileContent
                : overrideModelContentId(modelToBeUpdate,
                                         fileContent);
        modelToBeUpdate.setContentType(fixedFileContent.getContentType());
        modelToBeUpdate.setContent(fixedFileContent.toString());

        Optional.ofNullable(modelToBeUpdate.getType()).flatMap(modelContentService::findModelContentConverter)
                .flatMap(validator -> validator.convertToModelContent(fixedFileContent.getFileContent()))
                .ifPresent(modelContent -> modelToBeUpdate.setTemplate(modelContent.getTemplate()));

        return modelRepository.updateModelContent(modelToBeUpdate,
                                                  fixedFileContent);
    }

    public FileContent overrideModelContentId(Model model,
                                              FileContent fileContent) {
        return modelContentService.findModelContentConverter(model.getType()).map(modelContentConverter -> modelContentConverter.overrideModelId(fileContent,
                                                                                                                                                 this.modelIdentifiers,
                                                                                                                                                 model.getId()))
                .orElse(fileContent);
    }

    public Optional<ModelContent> createModelContentFromModel(Model model,
                                                              FileContent fileContent) {
        return (Optional<ModelContent>) modelContentService.findModelContentConverter(model.getType())
                .map(modelContentConverter -> modelContentConverter.convertToModelContent(fileContent.getFileContent())).orElse(Optional.empty());
    }

    public Model importSingleModel(Project project,
                                   ModelType modelType,
                                   FileContent fileContent) {
        this.cleanModelIdList();
        Model model = this.importModel(project,
                                       modelType,
                                       fileContent);
        return this.updateModelContent(model,
                                       fileContent);
    }

    public Model importModel(Project project,
                             ModelType modelType,
                             FileContent fileContent) {
        logger.debug(MessageFormat.format("Importing model type {0} from file {1}: {2}",
                                          modelType,
                                          fileContent.getFilename(),
                                          fileContent));

        Model model = importModelFromContent(project,
                                             modelType,
                                             fileContent);
        return model;
    }

    public Model importModelFromContent(Project project,
                                        ModelType modelType,
                                        FileContent fileContent) {
        Model model = null;
        if (modelTypeService.isJson(modelType) || ContentTypeUtils.isJsonContentType(fileContent.getContentType())) {
            model = convertContentToModel(modelType,
                                          fileContent);
        } else {
            model = createModelFromContent(modelType,
                                           fileContent);
        }
        String convertedId = model.getId() != null
                ? model.getId()
                : retrieveModelIdFromModelContent(model,
                                                  fileContent);
        createModel(project,
                    model);
        if (convertedId != null) {
            modelIdentifiers.put(convertedId,
                                 String.join("-",
                                             model.getType().toLowerCase(),
                                             model.getId()));
        }
        return model;
    }

    private String retrieveModelIdFromModelContent(Model model,
                                                   FileContent fileContent) {
        Optional<ModelContent> modelContent = this.createModelContentFromModel(model,
                                                                               fileContent);
        return modelContent.isPresent() ? modelContent.get().getId() : null;
    }

    public Model convertContentToModel(ModelType modelType,
                                       FileContent fileContent) {
        Model model = jsonConverter.tryConvertToEntity(fileContent.getFileContent())
                .orElseThrow(() -> new ImportModelException("Cannot convert json file content to model: " + fileContent));
        model.setName(removeEnd(removeExtension(fileContent.getFilename(),
                                                JSON),
                                modelType.getMetadataFileSuffix()));
        model.setType(modelType.getName());

        return model;
    }

    public Model createModelFromContent(ModelType modelType,
                                        FileContent fileContent) {
        return contentFilenameToModelName(fileContent.getFilename(),
                                          modelType).map(
                                                         modelName -> buildModel(modelType.getName(),
                                                                                 modelName))
                                                  .orElseThrow(() -> new ImportModelException(MessageFormat
                                                          .format("Unexpected extension was found for file to import model of type {0}: {1}",
                                                                  modelType.getName(),
                                                                  fileContent.getFilename())));
    }

    public Optional<String> contentFilenameToModelName(String filename,
                                                       ModelType modelType) {
        return Arrays.stream(modelType.getAllowedContentFileExtension()).filter(filename::endsWith).findFirst().map(extension -> removeExtension(filename,
                                                                                                                                                 extension));
    }

    public void validateModelContent(Model model,
                                     ValidationContext validationContext) {
        validateModelContent(model.getType(),
                             modelRepository.getModelContent(model),
                             model.getContentType(),
                             validationContext);
    }

    public void validateModelContent(Model model,
                                     FileContent fileContent) {
        ValidationContext validationContext = !modelTypeService.isJson(findModelType(model)) && fileContent.getContentType().equals(CONTENT_TYPE_JSON)
                ? EMPTY_CONTEXT
                : Optional.ofNullable(model.getProject()).map(this::createValidationContext).orElseGet(() -> createValidationContext(model));
        validateModelContent(model.getType(),
                             fileContent.getFileContent(),
                             fileContent.getContentType(),
                             validationContext);
    }

    private ValidationContext createValidationContext(Project project) {
        return new ProjectValidationContext(getAllModels(project));
    }

    private ValidationContext createValidationContext(Model model) {
        return new ProjectValidationContext(model);
    }

    public void validateModelContent(Model model,
                                     FileContent fileContent,
                                     ValidationContext validationContext) {
        validateModelContent(model.getType(),
                             fileContent.getFileContent(),
                             fileContent.getContentType(),
                             validationContext);
    }

    private void validateModelContent(String modelType,
                                      byte[] modelContent,
                                      String contentType,
                                      ValidationContext validationContext) {
        modelContentService.findModelValidator(modelType,
                                               contentType)
                .ifPresent(modelValidator -> modelValidator.validateModelContent(modelContent,
                                                                                 validationContext));
    }

    private ModelType findModelType(Model model) {
        return Optional.ofNullable(model.getType()).flatMap(modelTypeService::findModelTypeByName)
                .orElseThrow(() -> new UnknownModelTypeException("Unknown model type: " + model.getType()));
    }

    public Model newModelInstance(String type,
                                  String name) {
        Model model = newModelInstance();
        model.setType(type);
        model.setName(name);
        return model;
    }

    public Model newModelInstance() {
        try {
            return (Model) modelRepository.getModelType().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<FileContent> getModelExtensionsFileContent(Model model) {
        if (isJsonContentType(model.getContentType())) {
            return Optional.empty();
        }

        Model fullModel = findModelById(model.getId()).orElse(model);
        byte[] modelContent = modelRepository.getModelContent(model);
        final String bpmnModelId = modelContentService.findModelContentConverter(model.getType()).flatMap(converter -> converter.convertToModelContent(modelContent))
                .map(ModelContent::getId).orElseGet(() -> modelContentService.getModelContentId(model));

        Model modelToFile = newModelInstance(fullModel.getType(),
                                             fullModel.getName());
        modelToFile.setId(bpmnModelId);
        modelToFile.setExtensions(fullModel.getExtensions());

        FileContent metadataFileContent = new FileContent(getExtensionsFilename(model),
                                                          CONTENT_TYPE_JSON,
                                                          jsonConverter.convertToJsonBytes(modelToFile));
        return Optional.of(metadataFileContent);
    }

    public Optional<FileContent> getModelMetadataFileContent(Model model) {
        Model fullModel = findModelById(model.getId()).orElse(model);

        if (model.getMetadata() == null || model.getMetadata().isEmpty()) {
            return Optional.empty();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            FileContent metadataFileContent = new FileContent(getMetadataFilename(model),
                                                              CONTENT_TYPE_JSON,
                                                              objectMapper.writeValueAsString(fullModel.getMetadata()).getBytes());
            return Optional.of(metadataFileContent);
        } catch (JsonProcessingException e) {
            logger.error("Unable to generate model metadata for model with id: " + model.getId(),
                         e);
            return Optional.empty();
        }
    }

    public String getExtensionsFilename(Model model) {
        return toJsonFilename(model.getName() + findModelType(model).getExtensionsFileSuffix());
    }

    public void validateModelMetadata(Model model,
                                      ValidationContext validationContext) {
        validateModelMetadata(model.getType(),
                              modelRepository.getModelContent(model),
                              validationContext);
    }

    public void validateModelMetadata(Model model,
                                      FileContent fileContent) {
        ValidationContext validationContext = !modelTypeService.isJson(findModelType(model))
                ? EMPTY_CONTEXT
                : Optional.ofNullable(model.getProject()).map(this::createValidationContext).orElseGet(() -> createValidationContext(model));
        validateModelMetadata(model.getType(),
                              fileContent.getFileContent(),
                              validationContext);
    }

    public void validateModelMetadata(Model model,
                                      FileContent fileContent,
                                      ValidationContext validationContext) {
        validateModelMetadata(model.getType(),
                              fileContent.getFileContent(),
                              validationContext);
    }

    private void validateModelMetadata(String modelType,
                                       byte[] modelContent,
                                       ValidationContext validationContext) {
        modelContentService.findMetadataValidator(modelType).ifPresent(modelValidator -> modelValidator.validateModelContent(modelContent,
                                                                                                                             validationContext));
    }
}
