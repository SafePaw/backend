package com.ne7k.safepaw.crew.service;

import com.ne7k.safepaw.crew.domain.Crew;
import com.ne7k.safepaw.crew.dto.response.CrewRankingBoardResponse;
import com.ne7k.safepaw.crew.dto.response.CrewRankingEntryResponse;
import com.ne7k.safepaw.crew.dto.response.MyCrewRankingResponse;
import com.ne7k.safepaw.crew.repository.CrewMemberRepository;
import com.ne7k.safepaw.crew.repository.CrewMemberRepository.CrewCountRow;
import com.ne7k.safepaw.crew.repository.CrewRepository;
import com.ne7k.safepaw.crew.repository.CrewStatsRepository;
import com.ne7k.safepaw.crew.repository.CrewStatsRepository.CrewRankRow;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.score.repository.SeasonRepository;
import com.ne7k.safepaw.score.service.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewRankingQueryService {

    private static final int MAX_SIZE = 50;
    private static final String UNIT = "m²";
    private static final String CATEGORY = "CREW_TERRITORY";

    private final CrewStatsRepository crewStatsRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final MarkerUrlResolver markerUrlResolver;
    private final SeasonService seasonService;
    private final SeasonRepository seasonRepository;

    public CrewRankingBoardResponse board(String seasonParam, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST,
                    "page >= 0, 1 <= size <= " + MAX_SIZE);
        }
        String season = resolveSeasonKey(seasonParam);
        int offset = page * size;
        List<CrewRankRow> rows = crewStatsRepository.findCrewTerritoryBoard(season, size, offset);
        long total = crewStatsRepository.countCrewTerritoryParticipants(season);

        List<Long> ids = rows.stream().map(CrewRankRow::getCrewId).toList();
        Map<Long, Crew> crews = ids.isEmpty()
                ? Map.of()
                : crewRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Crew::getId, Function.identity()));
        Map<Long, Integer> counts = ids.isEmpty()
                ? Map.of()
                : crewMemberRepository.countGroupedByCrewId(ids).stream()
                .collect(Collectors.toMap(CrewCountRow::getCrewId, r -> (int) r.getCnt()));

        List<CrewRankingEntryResponse> content = new ArrayList<>(rows.size());
        for (CrewRankRow row : rows) {
            Crew crew = crews.get(row.getCrewId());
            content.add(new CrewRankingEntryResponse(
                    row.getRank(),
                    row.getCrewId(),
                    crew != null ? crew.getName() : "(unknown)",
                    crew != null ? markerUrlResolver.resolve(crew.getImageKey()) : null,
                    crew != null ? crew.getTerritoryColor() : null,
                    counts.getOrDefault(row.getCrewId(), 0),
                    row.getValue() != null ? row.getValue() : 0.0,
                    UNIT
            ));
        }
        return new CrewRankingBoardResponse(season, CATEGORY, page, size, total, content);
    }

    public MyCrewRankingResponse me(Long userId, String seasonParam) {
        var membership = crewMemberRepository.findWithCrewByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_JOINED));
        Crew crew = membership.getCrew();
        String season = resolveSeasonKey(seasonParam);
        CrewRankRow row = crewStatsRepository.findCrewTerritoryRank(season, crew.getId()).orElse(null);
        long participants = crewStatsRepository.countCrewTerritoryParticipants(season);

        Integer rank = row != null ? row.getRank() : null;
        double value = row != null && row.getValue() != null ? row.getValue() : 0.0;
        Double percentile = null;
        if (row != null && participants > 0) {
            long above = crewStatsRepository.countAboveCrewTerritory(season, value);
            percentile = Math.round((above * 1000.0 / participants)) / 10.0;
        }
        return new MyCrewRankingResponse(
                season,
                crew.getId(),
                crew.getName(),
                crew.getTerritoryColor(),
                markerUrlResolver.resolve(crew.getImageKey()),
                rank,
                value,
                UNIT,
                percentile
        );
    }

    private String resolveSeasonKey(String seasonParam) {
        if (seasonParam == null || seasonParam.isBlank()) {
            return seasonService.currentSeason().getKey();
        }
        return seasonRepository.findById(seasonParam)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND))
                .getKey();
    }
}
