package org.craftercms.search.service.impl.elasticsearch;

import org.craftercms.search.service.Query;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.craftercms.search.service.impl.elasticsearch.ElasticsearchSearchService.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchSearchServiceTest {
    private static final String FOUND_INDEX = "index1";
    private static final String SITE = "site";
    private static final String ID = "1";

    @Mock
    private QueryComponent queryComponent;
    @Mock
    private Query query;
    @Mock
    private DeletionComponent deletionComponent;
    @Mock
    private UpdateComponent updateComponent;
    @Mock
    private DeleteResponse deleteResponse;
    @Mock
    private UpdateResponse updateResponse;
    @InjectMocks
    private ElasticsearchSearchService service;
    private Map<String, Object> expectedResult;

    @Before
    public void setUpQuery() {
        Map<String, Object> queryComponentResult = new HashMap<>();
        queryComponentResult.put("test", 1);
        queryComponentResult.put("test2", "Hello, world!");
        expectedResult = Collections.unmodifiableMap(new HashMap<>(queryComponentResult));
        when(queryComponent.search(query)).thenReturn(expectedResult);
        when(queryComponent.search(FOUND_INDEX, query)).thenReturn(expectedResult);
    }

    @Before
    public void setUpDeletion() {
        when(deleteResponse.toString()).thenReturn("DeleteResponse");
        when(deletionComponent.delete(FOUND_INDEX, ELASTICSEARCH_TYPE, SITE +  ID_DELIMITER + ID)).thenReturn(deleteResponse);
        when(deletionComponent.delete(DEFAULT_INDEX, ELASTICSEARCH_TYPE, SITE + ID_DELIMITER + ID)).thenReturn(deleteResponse);
    }

    @Test
    public void verifySearchQueryReturnsExpectedResult() {
        assertEquals(expectedResult, service.search(query));
        verify(queryComponent).search(query);
    }

    @Test
    public void verifySearchWithIndexAndQueryReturnsExpectedResult() {
        assertEquals(expectedResult, service.search(FOUND_INDEX, query));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void verifyCommitUnsupported() {
        service.commit();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void verifyCommitWithIndexIdUnsupported() {
        service.commit(FOUND_INDEX);
    }

    @Test
    public void verifyDeleteWithIndex() {
        assertEquals("[index1] Delete for site:1 successful: DeleteResponse", service.delete(FOUND_INDEX, SITE, ID));
    }

    @Test
    public void verifyDeleteWithoutIndex() {
        assertEquals("[default_index] Delete for site:1 successful: DeleteResponse", service.delete(SITE, ID));
    }
}