package org.craftercms.search.service.impl.elasticsearch;

import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateComponentTest {
    private static final String INDEX = "index";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String JSON = "{\"field\":\"value\"}";

    @Mock
    private Client client;
    @InjectMocks
    private UpdateComponent updateComponent;
    @Mock
    private UpdateRequestBuilder requestBuilder;
    @Mock
    private UpdateResponse updateResponse;

    @Before
    public void setUp() {
        when(client.prepareUpdate(anyString(), anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.setDoc(anyString())).thenReturn(requestBuilder);
        when(requestBuilder.get()).thenReturn(updateResponse);
    }

    @Test
    public void verifyDeleteReturnsExpectedResponse() {
        assertEquals(updateResponse, updateComponent.update(INDEX, TYPE, ID, JSON));
    }

    @Test
    public void verifyDeletePreparesRightDeleteRequest() {
        updateComponent.update(INDEX, TYPE, ID, JSON);
        verify(client).prepareDelete(INDEX, TYPE, ID);
    }

    @Test
    public void verifyDeleteSetsRightDocument() {
        updateComponent.update(INDEX, TYPE, ID, JSON);
        verify(requestBuilder).setDoc(JSON);
    }
}