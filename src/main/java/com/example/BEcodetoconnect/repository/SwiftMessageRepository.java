package com.example.BEcodetoconnect.repository;

import com.example.BEcodetoconnect.model.SwiftMessage;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface SwiftMessageRepository extends MongoRepository<SwiftMessage, String> {
}
