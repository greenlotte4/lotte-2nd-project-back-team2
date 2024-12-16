package com.backend.repository.drive;

import com.backend.document.drive.Folder;
import com.backend.entity.page.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;


@Repository
public interface FolderMogoRepository extends MongoRepository<Folder, String>   {

    List<Folder> findByOwnerIdAndParentIdAndStatusIsNotOrderByOrder(String uid, String parentId ,int status);
    Optional<Folder> findByParentId(String parentId);
    Folder findByName(String name);


    List<Folder> findByOwnerIdAndParentIdAndStatusIsNot(String uid, String parentId, int status);
    List<Folder> findByOwnerIdAndIsPinnedAndStatus(String uid, int isPinned, int status);
    List<Folder> findByOwnerIdAndParentIdIsNotNullAndStatusIsNotOrderByUpdatedAtDesc(String uid, int status);

    List<Folder> findByOwnerIdAndStatus(String uid, int status);

    List<Folder> findAllByParentId(String parentId);

    Folder findFolderByNameAndParentIdAndStatusIsNot(String name, String parentId,int status);

    List<Folder> findBySharedUsersUidAndTarget(String userId,String target);
}
