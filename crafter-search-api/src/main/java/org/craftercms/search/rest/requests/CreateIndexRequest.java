package org.craftercms.search.rest.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

import static org.craftercms.search.rest.AdminRestApiConstants.PATH_VAR_ID;

/**
 * Created by alfonsovasquez on 2/6/17.
 */
public class CreateIndexRequest {

    @NotNull
    private String id;

    public CreateIndexRequest() {
    }

    public CreateIndexRequest(String id) {
        this.id = id;
    }

    @JsonProperty(PATH_VAR_ID)
    public String getId() {
        return id;
    }

    @JsonProperty(PATH_VAR_ID)
    public void setId(String id) {
        this.id = id;
    }
    
}
