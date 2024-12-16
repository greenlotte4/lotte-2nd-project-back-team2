package com.backend.repository.drive;


import com.backend.document.drive.Invitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitationRepository extends MongoRepository<Invitation, String> {

}
