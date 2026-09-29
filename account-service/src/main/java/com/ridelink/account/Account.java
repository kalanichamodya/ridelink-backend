package com.ridelink.account;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.*;
import java.util.UUID;
@Document(collection="accounts")
public class Account {
    @Id public UUID id;
    @Indexed(unique=true) public String email;
    public String passwordHash;
    public String name;
    public Role role;
    public boolean active = true;
    @Version public Long version;
    public enum Role { PASSENGER, DRIVER, ADMIN }
}
