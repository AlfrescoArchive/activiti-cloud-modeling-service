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

import static org.activiti.cloud.services.common.util.HttpUtils.writeFileToResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.organization.rest.api.ProjectRestApi;
import org.activiti.cloud.services.organization.rest.assembler.ProjectResourceAssembler;
import org.activiti.cloud.services.organization.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for {@link Project} resources
 */
@RestController
public class ProjectController implements ProjectRestApi {

    private final ProjectService projectService;

    private final ProjectResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<Project> pagedResourcesAssembler;
    
    private final SecurityManager securityManager;

    @Autowired
    public ProjectController(ProjectService projectService,
                             ProjectResourceAssembler resourceAssembler,
                             AlfrescoPagedResourcesAssembler<Project> pagedResourcesAssembler,
                             SecurityManager securityManager) {
        this.projectService = projectService;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.securityManager = securityManager;
    }

    @Override
    public PagedResources<Resource<Project>> getProjects(
            Pageable pageable,
            @RequestParam(
                    name = PROJECT_NAME_PARAM_NAME,
                    required = false) String name) {
        return pagedResourcesAssembler.toResource(
                pageable,
                getProjectsWithCheckRights(pageable,
                                           name),
                resourceAssembler);
    }

    @Override
    public Resource<Project> getProject(
            @PathVariable String projectId) {
        return resourceAssembler.toResource(findProjectById(projectId));
    }

    @Override
    public Resource<Project> createProject(
            @RequestBody @Valid Project project) {
        return resourceAssembler.toResource(projectService.createProject(project));
    }

    @Override
    public Resource<Project> updateProject(
            @PathVariable String projectId,
            @RequestBody @Valid Project project) {
        Project projectToUpdate = findProjectById(projectId);
        return resourceAssembler.toResource(projectService.updateProject(projectToUpdate,
                                                                         project));
    }

    @Override
    public void deleteProject(
            @PathVariable String projectId) {
        projectService.deleteProject(findProjectById(projectId));
    }

    @Override
    public Resource<Project> importProject(
            @RequestParam(UPLOAD_FILE_PARAM_NAME) MultipartFile file,
            @RequestParam(name = PROJECT_NAME_PARAM_NAME,
                          required = false) String name) throws IOException {
        return resourceAssembler.toResource(projectService.importProject(file, name));
    }

    @Override
    public void exportProject(
            HttpServletResponse response,
            @PathVariable String projectId,
            @RequestParam(name = EXPORT_AS_ATTACHMENT_PARAM_NAME,
                    required = false,
                    defaultValue = "true") boolean attachment) throws IOException {
        Project project = findProjectById(projectId);
        FileContent fileContent = projectService.exportProject(project);
        writeFileToResponse(response,
                            fileContent,
                            attachment);
    }

    public Project findProjectById(String projectId) {
        
        return findProjectWithCheckRights(projectId);
    }
    
    private Project findProjectWithCheckRights(String projectId) {
        String authenticatedUserId = securityManager.getAuthenticatedUserId();
        
        if (authenticatedUserId != null && !authenticatedUserId.isEmpty()) {
            
            //Maybe later we will need this check
            //List<String> userGroups = securityManager.getAuthenticatedUserGroups();
    
            Project project = projectService.findProjectById(projectId)
                              .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
            
            if (Objects.equals(project.getCreatedBy(), authenticatedUserId)) {
                return project;
            }

        }
        throw new IllegalStateException("You have no rights for the project: " + projectId);       
    }
    
    private Page<Project> getProjectsWithCheckRights(Pageable pageable,
                                               String name) {
        
        String authenticatedUserId = securityManager.getAuthenticatedUserId();
        
        if (authenticatedUserId != null && !authenticatedUserId.isEmpty()) {
            
            //To do: this should be done later in a proper way
            List<Project> projects = projectService.getProjects(Pageable.unpaged(),
                                                                name)
                    .stream()
                    .filter(project -> Objects.equals(project.getCreatedBy(), authenticatedUserId))
                    .collect(Collectors.toList());
                    
            return new PageImpl<Project>(projects, 
                                pageable, 
                                projects.size());           
                                                             
        }
        
        throw new IllegalStateException("You have no rights for projects");    
        
    }
 
}
