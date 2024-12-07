package com.backend.repository.drive;

import com.backend.document.drive.Folder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface FolderMogoRepository extends MongoRepository<Folder, String>   {

    List<Folder> findByOwnerIdAndParentIdAndStatusIsNotOrderByOrder(String uid, String parentId ,int status);
    Optional<Folder> findByParentId(String parentId);
    Folder findByName(String name);


    List<Folder> findByOwnerIdAndParentIdAndStatus(String uid, String parentId, int status);
    List<Folder> findByOwnerIdAndIsPinnedAndStatus(String uid, int isPinned, int status);


}
