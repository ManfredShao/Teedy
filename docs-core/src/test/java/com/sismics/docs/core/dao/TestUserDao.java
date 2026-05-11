package com.sismics.docs.core.dao;

import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.dao.criteria.UserCriteria;
import com.sismics.docs.core.dao.dto.UserDto;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.util.TransactionUtil;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

/**
 * Tests for User DAO.
 */
public class TestUserDao extends BaseTransactionalTest {
    @Test
    public void testCreateAuthenticateUpdateAndDelete() throws Exception {
        UserDao userDao = new UserDao();
        User user = createUserWithEmail("userDaoAuth", "userDaoAuth@docs.com");
        TransactionUtil.commit();

        Assert.assertNotNull(userDao.authenticate("userDaoAuth", "12345678"));
        Assert.assertNull(userDao.authenticate("userDaoAuth", "bad-password"));

        User update = new User();
        update.setId(user.getId());
        update.setEmail("updated@docs.com");
        update.setStorageQuota(200_000L);
        update.setStorageCurrent(50L);
        update.setTotpKey("totp-key");
        update.setDisableDate(new Date());
        userDao.update(update, user.getId());
        TransactionUtil.commit();

        ThreadLocalContext.get().getEntityManager().clear();
        User updatedDb = userDao.getById(user.getId());
        Assert.assertEquals("updated@docs.com", updatedDb.getEmail());
        Assert.assertEquals(Long.valueOf(200_000L), updatedDb.getStorageQuota());
        Assert.assertEquals(Long.valueOf(50L), updatedDb.getStorageCurrent());
        Assert.assertEquals("totp-key", updatedDb.getTotpKey());
        Assert.assertNotNull(updatedDb.getDisableDate());
        Assert.assertNull(userDao.authenticate("userDaoAuth", "12345678"));

        Assert.assertNotNull(userDao.getActiveByUsername("userDaoAuth"));

        userDao.delete("userDaoAuth", user.getId());
        TransactionUtil.commit();

        ThreadLocalContext.get().getEntityManager().clear();
        Assert.assertNull(userDao.getActiveByUsername("userDaoAuth"));
    }

    @Test
    public void testPasswordQuotaCriteriaAndCounts() throws Exception {
        UserDao userDao = new UserDao();
        User user1 = createUserWithEmail("userDaoCriteria1", "userDaoCriteria1@docs.com");
        User user2 = createUserWithEmail("userDaoCriteria2", "userDaoCriteria2@docs.com");
        TransactionUtil.commit();

        User updatePassword = new User();
        updatePassword.setId(user2.getId());
        updatePassword.setPassword("new-password");
        userDao.updatePassword(updatePassword, user2.getId());
        TransactionUtil.commit();

        Assert.assertNotNull(userDao.authenticate("userDaoCriteria2", "new-password"));

        User updateHashed = new User();
        updateHashed.setId(user1.getId());
        updateHashed.setPassword("hashed-password");
        userDao.updateHashedPassword(updateHashed);

        User updateQuota = new User();
        updateQuota.setId(user1.getId());
        updateQuota.setStorageCurrent(123L);
        userDao.updateQuota(updateQuota);

        User updateOnboarding = new User();
        updateOnboarding.setId(user1.getId());
        updateOnboarding.setOnboarding(true);
        userDao.updateOnboarding(updateOnboarding);
        TransactionUtil.commit();

        ThreadLocalContext.get().getEntityManager().clear();
        User refreshedUser1 = userDao.getById(user1.getId());
        Assert.assertEquals(Long.valueOf(123L), refreshedUser1.getStorageCurrent());
        Assert.assertEquals("hashed-password", refreshedUser1.getPassword());
        Assert.assertTrue(refreshedUser1.isOnboarding());

        UserCriteria searchCriteria = new UserCriteria().setSearch("userDaoCriteria");
        List<UserDto> searchResults = userDao.findByCriteria(searchCriteria, new SortCriteria(1, true));
        Assert.assertTrue(containsUser(searchResults, user1.getUsername()));
        Assert.assertTrue(containsUser(searchResults, user2.getUsername()));

        UserCriteria idCriteria = new UserCriteria().setUserId(user2.getId());
        List<UserDto> idResults = userDao.findByCriteria(idCriteria, new SortCriteria(1, true));
        Assert.assertEquals(1, idResults.size());
        Assert.assertEquals(user2.getUsername(), idResults.get(0).getUsername());

        UserCriteria nameCriteria = new UserCriteria().setUserName(user1.getUsername());
        List<UserDto> nameResults = userDao.findByCriteria(nameCriteria, new SortCriteria(1, true));
        Assert.assertEquals(1, nameResults.size());
        Assert.assertEquals(user1.getUsername(), nameResults.get(0).getUsername());

        long globalStorage = userDao.getGlobalStorageCurrent();
        Assert.assertTrue(globalStorage >= 123L);

        long activeUsers = userDao.getActiveUserCount();
        Assert.assertTrue(activeUsers >= 2L);

        assertDuplicateUsername(userDao, "userDaoCriteria1");
    }

    private User createUserWithEmail(String username, String email) throws Exception {
        UserDao userDao = new UserDao();
        User user = new User();
        user.setUsername(username);
        user.setPassword("12345678");
        user.setEmail(email);
        user.setRoleId("admin");
        user.setStorageQuota(100_000L);
        userDao.create(user, username);
        return user;
    }

    private boolean containsUser(List<UserDto> users, String username) {
        for (UserDto user : users) {
            if (username.equals(user.getUsername())) {
                return true;
            }
        }
        return false;
    }

    private void assertDuplicateUsername(UserDao userDao, String username) throws Exception {
        User duplicate = new User();
        duplicate.setUsername(username);
        duplicate.setPassword("12345678");
        duplicate.setEmail("dup-" + username + "@docs.com");
        duplicate.setRoleId("admin");
        duplicate.setStorageQuota(100_000L);
        try {
            userDao.create(duplicate, username);
            Assert.fail("Expected duplicate username exception");
        } catch (Exception e) {
            Assert.assertEquals("AlreadyExistingUsername", e.getMessage());
        }
    }
}
