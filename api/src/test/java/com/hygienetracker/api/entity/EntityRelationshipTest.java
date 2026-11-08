
package com.hygienetracker.api.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EntityRelationshipTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void entitiesCanBeSavedAndUserDeletionCascades() {
        // Create and save a user
        User user = new User();
        user.setDisplayName("Test User");
        user.setAvatar("test-avatar.png");
        user.setRole("USER");

        entityManager.persist(user);

        // Create and save an activity type
        ActivityType type = new ActivityType();
        type.setName("SHOWER");

        entityManager.persist(type);

        // Create an activity log
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setType(type);
        log.setLoggedAt(java.time.LocalDateTime.now());
        log.setMinutes(15);

        entityManager.persist(log);

        // Create shower details
        ShowerDetail detail = new ShowerDetail();
        detail.setActivityLog(log);
        detail.setShampoo(true);
        detail.setConditioner(true);
        detail.setBodyWash(false);
        detail.setExfoliant(false);

        entityManager.persist(detail);
        entityManager.flush();

        // Verify relationships were saved
        Long userId = user.getId();
        Long logId = log.getId();
        Long detailId = detail.getId();

        entityManager.clear();

        ActivityLog savedLog =
                entityManager.find(ActivityLog.class, logId);

        assertNotNull(savedLog);
        assertEquals(userId, savedLog.getUser().getId());
        assertEquals("SHOWER", savedLog.getType().getName());
        assertEquals(15, savedLog.getMinutes());
        assertTrue(savedLog.getShowerDetail().getShampoo());

        // Delete the user
        User savedUser = entityManager.find(User.class, userId);
        entityManager.remove(savedUser);
        entityManager.flush();
        entityManager.clear();

        // Verify associated records were deleted
        assertNull(entityManager.find(User.class, userId));
        assertNull(entityManager.find(ActivityLog.class, logId));
        assertNull(entityManager.find(ShowerDetail.class, detailId));

        // Activity types should remain
        assertNotNull(entityManager.find(ActivityType.class, type.getId()));
    }
}
