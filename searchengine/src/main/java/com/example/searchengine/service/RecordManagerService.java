package com.example.searchengine.service;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;

import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class RecordManagerService {
    private RecordManager recordManager;

    public RecordManagerService() throws IOException {
        initializeRecordManager();
    }

    private void initializeRecordManager() throws IOException {
        this.recordManager = RecordManagerFactory.createRecordManager("crawlerDb");
    }

    public RecordManager getRecordManager() {
        return recordManager;
    }

    public HTree getOrCreateHTree(String tableName) throws IOException {
        long recId = recordManager.getNamedObject(tableName);
        if (recId == 0) {
            HTree hTree = HTree.createInstance(recordManager);
            recordManager.setNamedObject(tableName, hTree.getRecid());
            return hTree;
        } else {
            return HTree.load(recordManager, recId);
        }
    }

    public void reinitializeRecordManager() throws IOException {
        if (recordManager != null) {
            recordManager.close();
        }
        initializeRecordManager();
    }

    public void commit() throws IOException {
        recordManager.commit();
    }

    public void close() throws IOException {
        recordManager.close();
    }
}