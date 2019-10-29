package org.activiti.cloud.services.organization.converter;

import java.util.HashMap;
import java.util.Map;

import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ReferenceIdOverriderTest {

    private ReferenceIdOverrider referenceIdOverrider;

    @Before
    public void setUp() {
        Map<String, String> map = new HashMap<>();
        map.put("oldFormKey", "newFormKey");
        map.put("oldCalledElement", "newCalledElement");
        referenceIdOverrider = new ReferenceIdOverrider(map);
    }

    @Test
    public void should_overrideUserTask_when_hasNewValue() {
        UserTask userTask = new UserTask();
        userTask.setFormKey("oldFormKey");

        referenceIdOverrider.override(userTask);

        assertThat(userTask.getFormKey()).isEqualTo("newFormKey");
    }

    @Test
    public void should_overrideUserTask_when_noNewValue() {
        UserTask userTask = new UserTask();
        userTask.setFormKey("noNewFormKey");

        referenceIdOverrider.override(userTask);

        assertThat(userTask.getFormKey()).isEqualTo("noNewFormKey");
    }

    @Test
    public void should_overrideCallActivity_when_hasNewValue() {
        CallActivity callActivity = new CallActivity();
        callActivity.setCalledElement("oldCalledElement");

        referenceIdOverrider.override(callActivity);

        assertThat(callActivity.getCalledElement()).isEqualTo("newCalledElement");
    }

    @Test
    public void should_overrideCallActivity_when_noNewValue() {
        CallActivity callActivity = new CallActivity();
        callActivity.setCalledElement("noNewCalledElement");

        referenceIdOverrider.override(callActivity);

        assertThat(callActivity.getCalledElement()).isEqualTo("noNewCalledElement");
    }

    @Test
    public void should_overrideStartEvent_when_hasNewValue() {
        StartEvent startEvent = new StartEvent();
        startEvent.setFormKey("oldFormKey");

        referenceIdOverrider.override(startEvent);

        assertThat(startEvent.getFormKey()).isEqualTo("newFormKey");
    }

    @Test
    public void should_overrideStartEvent_when_noNewValue() {
        StartEvent startEvent = new StartEvent();
        startEvent.setFormKey("noNewFormKey");

        referenceIdOverrider.override(startEvent);

        assertThat(startEvent.getFormKey()).isEqualTo("noNewFormKey");
    }
}