package com.backend.repository.drive;


import com.backend.document.drive.FileMogo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMogoRepository extends MongoRepository<FileMogo, String> {

    List<FileMogo> findByFolderIdAndStatusIsNot(String folderId,int status);


}
