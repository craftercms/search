/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.util;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.RestClientSearchService;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Small utility command line program that receives a Crafter site path as an argument and updates the search engine's index with all
 * XML documents found in the path. The program also receives the site name and search server URL as command line arguments.
 *
 * @author Alfonso Vásquez
 */
public class SearchIndexUpdater {

    private static final Log logger = LogFactory.getLog(SearchIndexUpdater.class);

    private String siteName;
    private File siteRoot;
    private SearchService searchService;

    @Required
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Required
    public void setSiteRoot(File siteRoot) {
        this.siteRoot = siteRoot;
    }

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void updateIndex() {
        // Ignore site root folder name in the ID
        addDirDocsToIndex(siteName + ":", siteRoot);

        logger.info("Commiting changes...");

        searchService.commit();
    }

    private void addDirDocsToIndex(String currentIndexId, File dir) {
        File[] listing = dir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(".xml");
            }

        });

        for (File file : listing) {
            String fileIndexId = currentIndexId + "/" + file.getName();

            if (file.isFile()) {
                addDocToIndex(fileIndexId, file);
            } else {
                addDirDocsToIndex(fileIndexId, file);
            }
        }
    }

    private void addDocToIndex(String currentIndexId, File docFile) {
        try {
            logger.info("Adding " + currentIndexId + " to search index...");

            searchService.update(siteName, currentIndexId, FileUtils.readFileToString(docFile, "UTF-8"), true);
        } catch (IOException e) {
            logger.warn("Cannot read file [" + docFile.getAbsolutePath() + "]", e);
        } catch (SearchException e) {
            throw new SearchException("Error while attempting to add file [" + docFile.getAbsolutePath() + "] to index", e);
        }
    }

    public static void main(String... args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("h", "prints the help");
        parser.accepts("s", "the Crafter site name").withRequiredArg();
        parser.accepts("u", "the Crafter Search server URL").withRequiredArg();
        parser.accepts("p", "the Crafter site path").withRequiredArg();

        OptionSet options = parser.parse(args);

        if (options.has("h")) {
            parser.printHelpOn(System.out);
        } else {
            checkForRequiredOption(options, "s");
            checkForRequiredOption(options, "u");
            checkForRequiredOption(options, "p");

            RestClientSearchService searchService = new RestClientSearchService();
            searchService.setServerUrl(options.valueOf("u").toString());

            String sitePath = options.valueOf("p").toString();
            File siteRoot = new File(sitePath);

            if (!siteRoot.exists()) {
                throw new IllegalArgumentException("The specified document's path [" + sitePath + "] doesn't exist");
            }
            if (!siteRoot.isDirectory()) {
                throw new IllegalArgumentException("The specified document's path [" + sitePath + "] is not a directory");
            }

            SearchIndexUpdater searchIndexUpdater = new SearchIndexUpdater();
            searchIndexUpdater.setSiteName(options.valueOf("s").toString());
            searchIndexUpdater.setSiteRoot(siteRoot);
            searchIndexUpdater.setSearchService(searchService);

            searchIndexUpdater.updateIndex();
        }
    }

    private static void checkForRequiredOption(OptionSet options, String name) {
        if (!options.has(name)) {
            throw new RuntimeException("Missing required command line argument '" + name + "'");
        }
    }

}
