package com.sismics.docs.core.dao;

import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.constant.PermType;
import com.sismics.docs.core.dao.dto.DocumentDto;
import com.sismics.docs.core.model.jpa.Document;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.util.TransactionUtil;
import com.sismics.util.context.ThreadLocalContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Tests the Document DAO.
 */
public class TestDocumentDao extends BaseTransactionalTest {
    @Test
    public void testCreateFindUpdateAndCount() throws Exception {
        User user = createUser("testDocumentDao");
        DocumentDao documentDao = new DocumentDao();

        Document document = createDocument(user, "Document title", null);
        TransactionUtil.commit();

        Document fetched = documentDao.getById(document.getId());
        Assert.assertNotNull(fetched);
        Assert.assertEquals("Document title", fetched.getTitle());

        List<Document> byUser = documentDao.findByUserId(user.getId());
        Assert.assertTrue(containsDocument(byUser, document.getId()));

        List<Document> all = documentDao.findAll(0, 10);
        Assert.assertTrue(containsDocument(all, document.getId()));

        long count = documentDao.getDocumentCount();
        Assert.assertTrue(count >= 1);

        DocumentDto dto = documentDao.getDocument(document.getId(), PermType.READ, Arrays.asList("admin"));
        Assert.assertNotNull(dto);
        Assert.assertEquals(document.getId(), dto.getId());
        Assert.assertEquals(user.getUsername(), dto.getCreator());
        Assert.assertEquals(Boolean.FALSE, dto.getShared());
        Assert.assertEquals(Integer.valueOf(0), dto.getFileCount());

        Document updated = new Document();
        updated.setId(document.getId());
        updated.setUserId(user.getId());
        updated.setTitle("Updated title");
        updated.setDescription("Updated description");
        updated.setSubject("Updated subject");
        updated.setIdentifier("Updated identifier");
        updated.setPublisher("Updated publisher");
        updated.setFormat("Updated format");
        updated.setSource("Updated source");
        updated.setType("Updated type");
        updated.setCoverage("Updated coverage");
        updated.setRights("Updated rights");
        updated.setCreateDate(new Date());
        updated.setLanguage("fra");
        updated.setFileId(createFile(user, 123L).getId());
        documentDao.update(updated, user.getId());
        TransactionUtil.commit();

        ThreadLocalContext.get().getEntityManager().clear();
        Document updatedDb = documentDao.getById(document.getId());
        Assert.assertEquals("Updated title", updatedDb.getTitle());
        Assert.assertEquals("Updated description", updatedDb.getDescription());
        Assert.assertEquals("fra", updatedDb.getLanguage());
        Assert.assertEquals(updated.getFileId(), updatedDb.getFileId());
    }

    @Test
    public void testUpdateFileIdAndMissingDocument() throws Exception {
        User user = createUser("testDocumentDaoFileId");
        DocumentDao documentDao = new DocumentDao();

        Document document = createDocument(user, "Document file update", null);
        TransactionUtil.commit();

        document.setFileId(createFile(user, 321L).getId());
        documentDao.updateFileId(document);
        TransactionUtil.commit();

        ThreadLocalContext.get().getEntityManager().clear();
        Document updatedDb = documentDao.getById(document.getId());
        Assert.assertEquals(document.getFileId(), updatedDb.getFileId());

        Document missing = documentDao.getById("missing-document-id");
        Assert.assertNull(missing);
    }

    private Document createDocument(User user, String title, String fileId) {
        Document document = new Document();
        document.setUserId(user.getId());
        document.setLanguage("eng");
        document.setTitle(title);
        document.setDescription("Document description");
        document.setSubject("Document subject");
        document.setIdentifier("Document identifier");
        document.setPublisher("Document publisher");
        document.setFormat("Document format");
        document.setSource("Document source");
        document.setType("Document type");
        document.setCoverage("Document coverage");
        document.setRights("Document rights");
        document.setCreateDate(new Date());
        document.setUpdateDate(new Date());
        document.setFileId(fileId);

        DocumentDao documentDao = new DocumentDao();
        documentDao.create(document, user.getId());
        return document;
    }

    private boolean containsDocument(List<Document> documents, String documentId) {
        for (Document document : documents) {
            if (documentId.equals(document.getId())) {
                return true;
            }
        }
        return false;
    }
}
