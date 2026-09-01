package com.ne7k.safepaw.crew.service;

import com.ne7k.safepaw.crew.config.CrewProperties;
import com.ne7k.safepaw.crew.domain.Crew;
import com.ne7k.safepaw.crew.domain.CrewMember;
import com.ne7k.safepaw.crew.domain.CrewRole;
import com.ne7k.safepaw.crew.dto.request.CrewCreateRequest;
import com.ne7k.safepaw.crew.dto.request.CrewJoinRequest;
import com.ne7k.safepaw.crew.dto.request.CrewTransferRequest;
import com.ne7k.safepaw.crew.dto.request.CrewUpdateRequest;
import com.ne7k.safepaw.crew.dto.response.CrewMemberResponse;
import com.ne7k.safepaw.crew.dto.response.CrewResponse;
import com.ne7k.safepaw.crew.repository.CrewMemberRepository;
import com.ne7k.safepaw.crew.repository.CrewRepository;
import com.ne7k.safepaw.crew.repository.CrewStatsRepository;
import com.ne7k.safepaw.crew.repository.CrewStatsRepository.UserAreaRow;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.score.service.SeasonService;
import com.ne7k.safepaw.user.domain.User;
import com.ne7k.safepaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrewService {

    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewStatsRepository crewStatsRepository;
    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final MarkerUrlResolver markerUrlResolver;
    private final CrewProperties crewProperties;
    private final SeasonService seasonService;

    @Transactional
    public CrewResponse create(Long userId, CrewCreateRequest req) {
        assertNotInCrew(userId);
        String name = req.name().trim();
        if (crewRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.CREW_NAME_DUPLICATED);
        }
        assertOwnDraftKey(userId, req.imageKey());

        User leader = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            Crew crew = crewRepository.save(Crew.create(
                    leader, name, blankToNull(req.imageKey()), req.territoryColor(), newInviteCode()));
            crewMemberRepository.save(CrewMember.leaderOf(crew, leader));
            return toResponse(crew, userId, true);
        } catch (DataIntegrityViolationException e) {
            throw translateConstraint(e);
        }
    }

    @Transactional
    public CrewResponse join(Long userId, CrewJoinRequest req) {
        assertNotInCrew(userId);
        String code = req.inviteCode().trim().toUpperCase();
        Crew found = crewRepository.findByInviteCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_INVALID_INVITE));

        Crew locked = crewRepository.findByIdForUpdate(found.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));

        if (crewMemberRepository.countByCrew_Id(locked.getId()) >= crewProperties.maxMembers()) {
            throw new BusinessException(ErrorCode.CREW_MEMBER_LIMIT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        try {
            crewMemberRepository.save(CrewMember.memberOf(locked, user));
        } catch (DataIntegrityViolationException e) {
            throw translateConstraint(e);
        }
        return toResponse(locked, userId, true);
    }

    @Transactional
    public void leave(Long userId, Long crewId) {
        CrewMember me = requireMember(crewId, userId);
        if (me.getRole() == CrewRole.LEADER) {
            throw new BusinessException(ErrorCode.CREW_LEADER_CANNOT_LEAVE);
        }
        crewMemberRepository.deleteByCrewIdAndUserId(crewId, userId);
    }

    @Transactional
    public void kick(Long leaderUserId, Long crewId, Long targetUserId) {
        Crew crew = requireLeader(crewId, leaderUserId);
        if (leaderUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CREW_CANNOT_KICK_SELF);
        }
        requireMember(crewId, targetUserId);
        crewMemberRepository.deleteByCrewIdAndUserId(crewId, targetUserId);
        // 강퇴된 멤버가 기존 초대 코드로 재가입하지 못하도록 코드 자동 갱신
        crew.rotateInviteCode(newInviteCode());
    }

    @Transactional
    public void disband(Long leaderUserId, Long crewId) {
        Crew crew = requireLeader(crewId, leaderUserId);
        crewMemberRepository.deleteAllByCrewId(crewId);
        crewRepository.delete(crew);
    }

    @Transactional
    public CrewResponse transfer(Long leaderUserId, Long crewId, CrewTransferRequest req) {
        Crew crew = requireLeader(crewId, leaderUserId);
        if (leaderUserId.equals(req.targetUserId())) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "이미 리더입니다.");
        }
        CrewMember from = requireMember(crewId, leaderUserId);
        CrewMember to = crewMemberRepository.findByCrew_IdAndUser_Id(crewId, req.targetUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_TRANSFER_NOT_MEMBER));

        from.changeRole(CrewRole.MEMBER);
        crewMemberRepository.flush();
        to.changeRole(CrewRole.LEADER);
        crew.transferLeader(to.getUser());
        return toResponse(crew, leaderUserId, true);
    }

    @Transactional
    public CrewResponse rotateInviteCode(Long leaderUserId, Long crewId) {
        Crew crew = requireLeader(crewId, leaderUserId);
        crew.rotateInviteCode(newInviteCode());
        return toResponse(crew, leaderUserId, true);
    }

    @Transactional
    public CrewResponse update(Long leaderUserId, Long crewId, CrewUpdateRequest req) {
        Crew crew = requireLeader(crewId, leaderUserId);
        if (req.name() != null && !req.name().isBlank()) {
            String name = req.name().trim();
            if (crewRepository.existsByNameAndIdNot(name, crewId)) {
                throw new BusinessException(ErrorCode.CREW_NAME_DUPLICATED);
            }
        }
        assertOwnDraftKey(leaderUserId, req.imageKey());
        try {
            crew.updateProfile(req.name(), req.imageKey(), req.territoryColor());
            return toResponse(crew, leaderUserId, true);
        } catch (DataIntegrityViolationException e) {
            throw translateConstraint(e);
        }
    }

    @Transactional(readOnly = true)
    public CrewResponse getMine(Long userId) {
        CrewMember me = crewMemberRepository.findWithCrewByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_JOINED));
        return toResponse(me.getCrew(), userId, true);
    }

    @Transactional(readOnly = true)
    public CrewResponse getPublic(Long viewerUserId, Long crewId) {
        Crew crew = crewRepository.findWithLeaderById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        boolean member = crewMemberRepository.findByCrew_IdAndUser_Id(crewId, viewerUserId).isPresent();
        return toResponse(crew, viewerUserId, member);
    }

    @Transactional(readOnly = true)
    public List<CrewMemberResponse> listMembers(Long viewerUserId, Long crewId) {
        requireMember(crewId, viewerUserId);
        if (!crewRepository.existsById(crewId)) {
            throw new BusinessException(ErrorCode.CREW_NOT_FOUND);
        }
        String season = seasonService.currentSeason().getKey();
        Map<Long, Double> areas = crewStatsRepository.findMemberAreas(crewId, season).stream()
                .collect(Collectors.toMap(UserAreaRow::getUserId,
                        r -> r.getArea() != null ? r.getArea() : 0.0));

        return crewMemberRepository.findAllWithUserByCrewId(crewId).stream()
                .map(m -> new CrewMemberResponse(
                        m.getUser().getId(),
                        m.getUser().getNickname(),
                        m.getRole().name(),
                        m.getJoinedAt(),
                        dogRepository.countByOwner_Id(m.getUser().getId()),
                        areas.getOrDefault(m.getUser().getId(), 0.0)
                ))
                .toList();
    }

    public CrewResponse toResponse(Crew crew, Long viewerUserId, boolean includeInvite) {
        int memberCount = (int) crewMemberRepository.countByCrew_Id(crew.getId());
        String myRole = crewMemberRepository.findByCrew_IdAndUser_Id(crew.getId(), viewerUserId)
                .map(m -> m.getRole().name())
                .orElse(null);
        String imageUrl = markerUrlResolver.resolve(crew.getImageKey());
        return CrewResponse.of(
                crew,
                imageUrl,
                memberCount,
                crewProperties.maxMembers(),
                includeInvite ? crew.getInviteCode() : null,
                myRole
        );
    }

    private Crew requireLeader(Long crewId, Long userId) {
        Crew crew = crewRepository.findWithLeaderById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        if (!crew.isLedBy(userId)) {
            throw new BusinessException(ErrorCode.CREW_NOT_LEADER);
        }
        return crew;
    }

    private CrewMember requireMember(Long crewId, Long userId) {
        return crewMemberRepository.findByCrew_IdAndUser_Id(crewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_MEMBER));
    }

    private void assertNotInCrew(Long userId) {
        if (crewMemberRepository.existsByUser_Id(userId)) {
            throw new BusinessException(ErrorCode.CREW_ALREADY_JOINED);
        }
    }

    private void assertOwnDraftKey(Long userId, String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return;
        }
        String prefix = "crews/" + userId + "/draft/";
        if (!imageKey.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "이미지 키가 올바르지 않습니다.");
        }
    }

    private String newInviteCode() {
        int len = crewProperties.inviteCodeLength();
        for (int attempt = 0; attempt < 16; attempt++) {
            char[] buf = new char[len];
            for (int i = 0; i < len; i++) {
                buf[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
            }
            String code = new String(buf);
            if (!crewRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "초대 코드 발급에 실패했습니다.");
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private BusinessException translateConstraint(DataIntegrityViolationException e) {
        String msg = String.valueOf(e.getMostSpecificCause().getMessage());
        if (msg.contains("uq_crews_name")) {
            return new BusinessException(ErrorCode.CREW_NAME_DUPLICATED);
        }
        if (msg.contains("uq_crew_members_user")) {
            return new BusinessException(ErrorCode.CREW_ALREADY_JOINED);
        }
        return new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
    }
}
