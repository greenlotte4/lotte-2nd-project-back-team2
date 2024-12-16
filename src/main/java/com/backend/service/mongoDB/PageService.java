package com.backend.service.mongoDB;

import com.backend.document.page.Page;
import com.backend.dto.request.page.PageDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.folder.Permission;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.page.PageRepository;
import com.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;
    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Page save(PageDto page) {

        Page pages  = page.ToEntity();
        Page savedpage =  pageRepository.save(pages);
        log.info(savedpage);
        return savedpage;
    }

    public PageDto findById(String id) {
        Optional<Page> opt = pageRepository.findById(id);
        if(opt.isPresent()) {
            Page page = opt.get();
            PageDto pageDto = PageDto.builder()
                    .id(page.getId())
                    .ownerUid(page.getOwnerUid())
                    .title(page.getTitle())
                    .content(page.getContent())
                    .createAt(page.getCreateAt())
                    .updateAt(page.getUpdateAt())
                    .build();
            return pageDto;
        }
        return null;
    }

    public List<Page> pageList(String uid){
        List<Page> list = pageRepository.findByOwnerUidContaining(uid);
        return list;
    }

    public ResponseEntity<?> getPageContent(String pageId, Long userId) {
        Optional<Page> page = pageRepository.findById(pageId);
        if(page.isEmpty()){
            return ResponseEntity.badRequest().body("페이지가 없습니다...");
        }
        PageDto dto = PageDto.builder()
                .ownerUid(page.get().getOwnerUid())
                .title(page.get().getTitle())
                .content(page.get().getContent())
                .createAt(page.get().getCreateAt())
                .updateAt(page.get().getUpdateAt())
                .id(page.get().getId())
                .build();

        return ResponseEntity.ok(dto);
    }

    public ResponseEntity<?> putPageContent(PageDto dto, Long userId) {
        Optional<Page> page = pageRepository.findById(dto.getId());
        if(page.isEmpty()){
            return ResponseEntity.badRequest().body("페이지가 없습니다...");
        }
        page.get().putData(dto);
        pageRepository.save(page.get());
        return ResponseEntity.ok("수정 성공");
    }

    public ResponseEntity<?> getPageUsers(String pageId) {
        Optional<Page> page = pageRepository.findById(pageId);
        if(page.isEmpty()){
            return ResponseEntity.ok("dd");
        }
        String userIds = page.get().getOwnerUid();
        List<String> result = Arrays.asList(userIds.split(","));
        List<GetUsersAllDto> dtos = new ArrayList<>();
        for (String uid : result) {
            Optional<User> user = userRepository.findByUid(uid);
            if(user.isEmpty()){
                return ResponseEntity.badRequest().body("유저 정보가 일치하지 않습니다.");
            }
            GetUsersAllDto dto = user.get().toGetUsersAllDto();
            dtos.add(dto);
        }

        return ResponseEntity.ok(dtos);
    }

    public ResponseEntity<?> getPageTitle(String pageId) {
        Optional<Page> page = pageRepository.findById(pageId);
        if(page.isEmpty()){
            return ResponseEntity.badRequest().body("페이지 정보가 일치하지 않습니다.");
        }
        String title = page.get().getTitle();
        return ResponseEntity.ok(title);
    }

    public ResponseEntity<?> putPageTitle(String pageId, String title) {
        Optional<Page> page = pageRepository.findById(pageId);
        if(page.isEmpty()){
            return ResponseEntity.badRequest().body("페이지 정보가 일치하지 않습니다.");
        }
        page.get().putTitle(title);
        pageRepository.save(page.get());
        return ResponseEntity.ok("제목수정 성공!");
    }

    public ResponseEntity<?> putPageUsers(String pageId, List<GetUsersAllDto> users) {
        Optional<Page> page = pageRepository.findById(pageId);
        if(page.isEmpty()){
            return ResponseEntity.badRequest().body("페이지 정보가 일치하지 않습니다.");
        }
        String pageIds = page.get().getOwnerUid();
        List<String> ids = users.stream().map(v->v.getUid()).toList();
        List<String> originUids = Arrays.asList(pageIds.split(","));

        Set<String> setIds = new HashSet<>(ids);
        Set<String> setOriginUids = new HashSet<>(originUids);

        Set<String> added = new HashSet<>(setOriginUids);
        added.removeAll(setIds);

        // 빠진 항목 (ids에는 있지만 originUids에는 없는 항목)
        Set<String> missing = new HashSet<>(setIds);
        missing.removeAll(setOriginUids);
        String addedString;
        String missingString;
        if(!added.isEmpty()){
            addedString = added.iterator().next();
            String message = "{\"message\":\"" + "added" + "\"}";
            messagingTemplate.convertAndSend("/topic/page/user/"+addedString, message);
        }
        if(!missing.isEmpty()){
            missingString = missing.iterator().next();
            String message = "{\"message\":\"" + "delete" + "\"}";
            messagingTemplate.convertAndSend("/topic/page/user/"+missingString, message);
        }
        String result = String.join(",", ids);
        page.get().putUsers(result);
        pageRepository.save(page.get());
        return ResponseEntity.ok("수정완료!");
    }

    public ResponseEntity<?> postNewPage(String uid) {
        Page page = Page.builder()
                .ownerUid(uid)
                .content(null)
                .title("제목없음")
                .createAt(LocalDateTime.now())
                .leader(uid)
                .build();
        pageRepository.save(page);

        return ResponseEntity.ok(page.getId());
    }

    public ResponseEntity<?> deletePage(String pageId) {
        Optional<Page> page = pageRepository.findById(pageId);
        if(page.isEmpty()){
            return ResponseEntity.badRequest().body("페이지 정보가 일치하지 않습니다.");
        }
        pageRepository.delete(page.get());
        String userIds = page.get().getOwnerUid();
        List<String> ids = Arrays.asList(userIds.split(","));
        for (String id : ids) {
            String message = "{\"message\":\"" + "delete" + "\"}";
            messagingTemplate.convertAndSend("/topic/page/user/"+id,message);
        }
        return ResponseEntity.ok("삭제성공!");
    }
}
