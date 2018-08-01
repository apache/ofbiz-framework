package org.apache.ofbiz.widget.renderer;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormFieldBuilder;
import org.junit.Before;
import org.junit.Test;

public class FormRendererTest {
    private ModelForm model;
    private FormRenderer renderer;
    private ArrayList<ModelFormField> fields;
    private HashMap<String, Object> context;
    private HashSet<String> useWhenFields;

    @Before
    public void setUp() throws Exception {
        model = mock(ModelForm.class);
        fields = new ArrayList<>();
        when(model.getFieldList()).thenReturn(fields);
        renderer = new FormRenderer(model, null);
        context = new HashMap<>();
        useWhenFields = new HashSet<>();
        when(model.getUseWhenFields()).thenReturn(useWhenFields);
    }

    @Test
    public void fieldsToRenderBasic() {
        ModelFormField a = ModelFormField.from(new ModelFormFieldBuilder().setName("A"));
        fields.add(a);
        ModelFormField b = ModelFormField.from(new ModelFormFieldBuilder().setName("B"));
        fields.add(b);
        assertThat(renderer.getUsedFields(context), hasItems(a, b));
        assertThat(renderer.getUsedFields(context).size(), is(2));
    }

    @Test
    public void fieldsToRenderDuplicates() {
        ModelFormField a1 = ModelFormField.from(new ModelFormFieldBuilder().setName("A"));
        fields.add(a1);
        ModelFormField b = ModelFormField.from(new ModelFormFieldBuilder().setName("B"));
        fields.add(b);
        ModelFormField a2 = ModelFormField.from(new ModelFormFieldBuilder().setName("A"));
        fields.add(a2);
        assertThat(renderer.getUsedFields(context), hasItems(a1, a2, b));
        assertThat(renderer.getUsedFields(context).size(), is(3));
    }

    @Test
    public void fieldsToRenderBasicUseWhen() {
        ModelFormField a1 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("true"));
        fields.add(a1);
        useWhenFields.add(a1.getName());
        ModelFormField a2 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("false"));
        fields.add(a2);
        useWhenFields.add(a2.getName());
        assertThat(renderer.getUsedFields(context), hasItems(a1, a2));
        assertThat(renderer.getUsedFields(context).size(), is(2));
    }

    @Test
    public void fieldsToRenderDuplicatesUseWhen() {
        ModelFormField a1 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("true"));
        fields.add(a1);
        useWhenFields.add(a1.getName());
        ModelFormField a2 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("false"));
        fields.add(a2);
        useWhenFields.add(a2.getName());
        ModelFormField a3 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("true"));
        fields.add(a3);
        useWhenFields.add(a3.getName());
        assertThat(renderer.getUsedFields(context), hasItems(a1, a2));
        assertThat(renderer.getUsedFields(context).size(), is(2));
    }

}
