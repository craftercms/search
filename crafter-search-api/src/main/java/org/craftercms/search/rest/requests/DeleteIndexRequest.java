package org.craftercms.search.rest.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.craftercms.search.service.AdminService;

import static org.craftercms.search.rest.AdminRestApiConstants.*;

/**
 * Created by alfonsovasquez on 2/6/17.
 */
public class DeleteIndexRequest {

    private AdminService.IndexDeleteMode deleteMode;

    @JsonProperty(PARAM_DELETE_MODE)
    public AdminService.IndexDeleteMode getDeleteMode() {
        return deleteMode;
    }

    @JsonProperty(PARAM_DELETE_MODE)
    public void setDeleteMode(AdminService.IndexDeleteMode deleteMode) {
        this.deleteMode = deleteMode;
    }

}
