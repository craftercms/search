/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.search.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by alfonsovasquez on 12/28/16.
 */
public class IndexingStatus {

    protected List<String> successfulUpdates;
    protected List<String> successfulDeletes;
    protected List<String> failedUpdates;
    protected List<String> failedDeletes;
    protected Lock listLock;

    public IndexingStatus() {
        successfulUpdates = new ArrayList<>();
        successfulDeletes = new ArrayList<>();
        failedUpdates = new ArrayList<>();
        failedDeletes = new ArrayList<>();
        listLock = new ReentrantLock();
    }

    public List<String> getSuccessfulUpdates() {
        return synchronizedCopy(successfulUpdates);
    }

    public List<String> getSuccessfulDeletes() {
        return synchronizedCopy(successfulDeletes);
    }

    public List<String> getFailedUpdates() {
        return synchronizedCopy(failedUpdates);
    }

    public List<String> getFailedDeletes() {
        return synchronizedCopy(failedDeletes);
    }

    public void addSuccessfulUpdate(String filename) {
        synchronizedAdd(successfulUpdates, filename);
    }

    public void addSuccessfulDelete(String filename) {
        synchronizedAdd(successfulDeletes, filename);
    }

    public void addFailedUpdate(String filename) {
        synchronizedAdd(failedUpdates, filename);
    }

    public void addFailedDelete(String filename) {
        synchronizedAdd(failedDeletes, filename);
    }

    public int getAttemptedUpdatesAndDeletes() {
        listLock.lock();
        try {
            return successfulUpdates.size() + successfulDeletes.size() + failedUpdates.size() + failedDeletes.size();
        } finally {
            listLock.unlock();
        }
    }

    protected List<String> synchronizedCopy(List<String> list) {
        listLock.lock();
        try {
            return new ArrayList<>(list);
        } finally {
            listLock.unlock();
        }
    }

    protected void synchronizedAdd(List<String> list, String filename) {
        listLock.lock();
        try {
            list.add(filename);
        } finally {
            listLock.unlock();
        }
    }

}
