package com.miniwallet.repository;

import com.miniwallet.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUserName(String userName);

  boolean existsByUserName(String userName);
}
