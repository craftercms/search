package org.craftercms.search.service.impl.elasticsearch;

import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeletionComponentTest {
    private static final String INDEX = "index";
    private static final String TYPE = "type";
    private static final String ID = "id";

    @Mock
    private Client client;
    @InjectMocks
    private DeletionComponent deletionComponent;
    @Mock
    private DeleteRequestBuilder requestBuilder;
    @Mock
    private DeleteResponse deleteResponse;

    @Before
    public void setUp() {
        when(client.prepareDelete(anyString(), anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.get()).thenReturn(deleteResponse);
    }

    @Test
    public void verifyDeleteReturnsExpectedResponse() {
        assertEquals(deleteResponse, deletionComponent.delete(INDEX, TYPE, ID));
    }

    @Test
    public void verifyDeletePreparesRightDeleteRequest() {
        deletionComponent.delete(INDEX, TYPE, ID);
        verify(client).prepareDelete(INDEX, TYPE, ID);
    }
}