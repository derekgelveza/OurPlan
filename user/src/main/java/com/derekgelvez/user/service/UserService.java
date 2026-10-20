package com.derekgelvez.user.service;


import com.derekgelvez.user.model.User;
import com.derekgelvez.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** General account queries. Not currently called by any controller or other service. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, EmailService emailService){
        this.userRepository = userRepository;
    }

    /** Every account as a full entity, including password hashes and codes; never return this to a client directly. */
    public List<User> allUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
}
