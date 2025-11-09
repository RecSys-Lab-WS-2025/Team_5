package de.tum.moodtrip_backend.repository;

import de.tum.moodtrip_backend.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

}
