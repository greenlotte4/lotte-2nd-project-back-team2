package com.backend.repository.drive;


import com.backend.document.drive.Invitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface InvitationRepository extends MongoRepository<Invitation, String> {

    Optional<Invitation> findByEmail(String email);

}
