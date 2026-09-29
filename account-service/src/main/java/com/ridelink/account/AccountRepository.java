package com.ridelink.account;

import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface AccountRepository extends MongoRepository<Account,UUID> {
    Optional<Account> findByEmail(String email);
}
