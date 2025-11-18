package de.tum.moodtrip_backend.adapter_database.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.core.port.UserPort;
import reactor.test.StepVerifier;

@SpringBootTest(properties = {"spring.liquibase.enabled=false"})
class DatabaseUserAdapterTest {

    @Autowired
    private UserPort userPort;

    @Test
    void testCreateAndFindUser() {
        // Prepare test data (ID is null, let database generate it)
        String username = "testuser_" + System.currentTimeMillis();
        String email = "test_" + System.currentTimeMillis() + "@example.com";

        UserProfile user = new UserProfile(null, username, email, null);

        // Test save user
        StepVerifier.create(userPort.save(user))
                .assertNext(savedUser -> {
                    System.out.println("✅ User saved successfully: " + savedUser);
                    assert savedUser.id() != null;
                    assert savedUser.username().equals(username);
                    assert savedUser.email().equals(email);
                })
                .verifyComplete();

        // Test find by username
        StepVerifier.create(userPort.findByUsername(username))
                .assertNext(foundUser -> {
                    System.out.println("✅ Found user by username: " + foundUser);
                    assert foundUser.username().equals(username);
                })
                .verifyComplete();

        // Test username existence
        StepVerifier.create(userPort.existsByUsername(username))
                .assertNext(exists -> {
                    System.out.println("✅ Username exists: " + exists);
                    assert exists;
                })
                .verifyComplete();
    }

    @Test
    void testUserEmailUniqueness() {
        String email = "unique_" + System.currentTimeMillis() + "@example.com";

        // Test email existence
        StepVerifier.create(userPort.existsByEmail(email))
                .assertNext(exists -> {
                    System.out.println("✅ New email does not exist: " + !exists);
                    assert !exists;
                })
                .verifyComplete();
    }
}
