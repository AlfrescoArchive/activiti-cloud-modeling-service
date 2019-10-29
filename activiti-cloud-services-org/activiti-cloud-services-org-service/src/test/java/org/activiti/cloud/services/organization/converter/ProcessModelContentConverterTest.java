package org.activiti.cloud.services.organization.converter;

import java.util.HashMap;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class ProcessModelContentConverterTest {

    @InjectMocks
    private ProcessModelContentConverter processModelContentConverter;

//    @Test
//    public void should_overrideProcessId() {
//        FileContent fileContent;
//        HashMap<String, String> map;
//
//        FileContent result = processModelContentConverter.overrideProcessId(fileContent, map);
//
//        //FIXME: finish testing for overrideProcessIdId
//    }

    @Test
    public void should_overrideProcessId_when_newProcessId() {
        HashMap<String, String> modelIdentifiers = new HashMap<>();
        String oldProcessId = "processId";
        String newProcessId = "new-processId";
        modelIdentifiers.put(oldProcessId, newProcessId);

        Process process = new Process();
        process.setId(oldProcessId);
        process.setName("processName");

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        BpmnProcessModelContent processModelContent = new BpmnProcessModelContent(bpmnModel);

        processModelContentConverter.overrideAllProcessDefinition(processModelContent, modelIdentifiers);

        Process processByNewId = processModelContent.getBpmnModel().getProcessById(newProcessId);
        assertThat(processByNewId).isSameAs(process);
        Process processByOldId = processModelContent.getBpmnModel().getProcessById(oldProcessId);
        assertThat(processByOldId).isNull();
    }

    @Test
    public void should_notOverrideProcessId_when_sameProcessId() {
        HashMap<String, String> modelIdentifiers = new HashMap<>();
        String processId = "processId";
        modelIdentifiers.put(processId, processId);

        Process process = new Process();
        process.setId(processId);
        process.setName("processName");

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        BpmnProcessModelContent processModelContent = new BpmnProcessModelContent(bpmnModel);

        processModelContentConverter.overrideAllProcessDefinition(processModelContent, modelIdentifiers);

        Process processByNewId = processModelContent.getBpmnModel().getProcessById(processId);
        assertThat(processByNewId).isSameAs(process);
    }

    @Test
    public void should_notOverrideProcessId_when_processIdNotFound() {
        HashMap<String, String> modelIdentifiers = new HashMap<>();
        String processId = "processId";
        String newProcessId = "newProcessId";
        modelIdentifiers.put(processId, newProcessId);

        Process process = new Process();
        String missingProcessId = "missingProcessId";
        process.setId(missingProcessId);
        process.setName("processName");

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        BpmnProcessModelContent processModelContent = new BpmnProcessModelContent(bpmnModel);


        processModelContentConverter.overrideAllProcessDefinition(processModelContent, modelIdentifiers);

        Process processById = processModelContent.getBpmnModel().getProcessById(missingProcessId);
        assertThat(processById).isSameAs(process);
        Process processByNewId = processModelContent.getBpmnModel().getProcessById(newProcessId);
        assertThat(processByNewId).isNull();
    }

}

