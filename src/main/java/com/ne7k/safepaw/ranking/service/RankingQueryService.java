package com.ne7k.safepaw.ranking.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.repository.DogRepository;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.ranking.domain.RankingCategory;
import com.ne7k.safepaw.ranking.dto.response.MyRankingResponse;
import com.ne7k.safepaw.ranking.dto.response.RankingBoardResponse;
import com.ne7k.safepaw.ranking.dto.response.RankingEntryResponse;
import com.ne7k.safepaw.ranking.repository.SeasonStatsRepository;
import com.ne7k.safepaw.ranking.repository.SeasonStatsRepository.DogRankRow;
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
public class RankingQueryService {

    private static final int MAX_SIZE = 50;

    private final SeasonStatsRepository seasonStatsRepository;
    private final SeasonService seasonService;
    private final SeasonRepository seasonRepository;
    private final DogRepository dogRepository;
    private final MarkerUrlResolver markerUrlResolver;

    public RankingBoardResponse board(String seasonParam, RankingCategory category, int page, int size) {
        validatePage(page, size);
        String seasonKey = resolveSeasonKey(seasonParam);
        int offset = page * size;

        List<DogRankRow> rows = switch (category) {
            case XP -> seasonStatsRepository.findXpBoard(seasonKey, size, offset);
            case TERRITORY -> seasonStatsRepository.findTerritoryBoard(seasonKey, size, offset);
            case DISTANCE -> seasonStatsRepository.findDistanceBoard(seasonKey, size, offset);
            case DURATION -> seasonStatsRepository.findDurationBoard(seasonKey, size, offset);
        };

        long total = switch (category) {
            case XP -> seasonStatsRepository.countXpParticipants(seasonKey);
            case TERRITORY -> seasonStatsRepository.countTerritoryParticipants(seasonKey);
            case DISTANCE -> seasonStatsRepository.countDistanceParticipants(seasonKey);
            case DURATION -> seasonStatsRepository.countDurationParticipants(seasonKey);
        };

        Map<Long, Dog> dogs = loadDogs(rows);
        String unit = category.unit();
        List<RankingEntryResponse> content = new ArrayList<>(rows.size());
        for (DogRankRow row : rows) {
            Dog dog = dogs.get(row.getDogId());
            MarkerUrlResolver.MarkerFields marker = dog != null
                    ? markerUrlResolver.resolveFields(dog.getMarkerImageKey())
                    : new MarkerUrlResolver.MarkerFields(null, null, null);
            content.add(new RankingEntryResponse(
                    row.getRank(),
                    row.getDogId(),
                    dog != null ? dog.getName() : "(unknown)",
                    marker.url(),
                    marker.type() != null ? marker.type().name() : null,
                    marker.value(),
                    dog != null && dog.getRank() != null ? dog.getRank().name() : null,
                    dog != null ? dog.getTerritoryColor() : null,
                    row.getValue() != null ? row.getValue() : 0.0,
                    unit
            ));
        }

        return new RankingBoardResponse(
                seasonKey,
                category.name(),
                page,
                size,
                total,
                content
        );
    }

    public MyRankingResponse me(Long userId, String seasonParam, Long dogId) {
        if (dogId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "dogId는 필수입니다.");
        }
        String seasonKey = resolveSeasonKey(seasonParam);

        Dog dog = dogRepository.findById(dogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_NOT_FOUND));
        if (!dog.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.DOG_NOT_OWNED);
        }

        MarkerUrlResolver.MarkerFields marker =
                markerUrlResolver.resolveFields(dog.getMarkerImageKey());

        DogRankRow xpRow = findRow(seasonStatsRepository.findAllXpRanks(seasonKey), dogId);
        DogRankRow territoryRow = findRow(seasonStatsRepository.findAllTerritoryRanks(seasonKey), dogId);
        DogRankRow distanceRow = findRow(seasonStatsRepository.findAllDistanceRanks(seasonKey), dogId);
        DogRankRow durationRow = findRow(seasonStatsRepository.findAllDurationRanks(seasonKey), dogId);

        return new MyRankingResponse(
                seasonKey,
                dog.getId(),
                dog.getName(),
                marker.url(),
                marker.type() != null ? marker.type().name() : null,
                marker.value(),
                dog.getTerritoryColor(),
                new MyRankingResponse.CategoryRanks(
                        toSlice(xpRow,
                                seasonStatsRepository.countXpParticipants(seasonKey),
                                xpRow == null ? 0L
                                        : seasonStatsRepository.countAboveXp(seasonKey, xpRow.getValue()),
                                RankingCategory.XP),
                        toSlice(territoryRow,
                                seasonStatsRepository.countTerritoryParticipants(seasonKey),
                                territoryRow == null ? 0L
                                        : seasonStatsRepository.countAboveTerritory(seasonKey, territoryRow.getValue()),
                                RankingCategory.TERRITORY),
                        toSlice(distanceRow,
                                seasonStatsRepository.countDistanceParticipants(seasonKey),
                                distanceRow == null ? 0L
                                        : seasonStatsRepository.countAboveDistance(seasonKey, distanceRow.getValue()),
                                RankingCategory.DISTANCE),
                        toSlice(durationRow,
                                seasonStatsRepository.countDurationParticipants(seasonKey),
                                durationRow == null ? 0L
                                        : seasonStatsRepository.countAboveDuration(seasonKey, durationRow.getValue()),
                                RankingCategory.DURATION)
                )
        );
    }

    private MyRankingResponse.RankSlice toSlice(DogRankRow row,
                                                long participants,
                                                long above,
                                                RankingCategory category) {
        if (row == null) {
            return new MyRankingResponse.RankSlice(null, 0.0, category.unit(), null);
        }
        Double percentile = null;
        if (participants > 0) {
            percentile = Math.round((above * 1000.0 / participants)) / 10.0;
        }
        return new MyRankingResponse.RankSlice(
                row.getRank(),
                row.getValue() != null ? row.getValue() : 0.0,
                category.unit(),
                percentile
        );
    }

    private static DogRankRow findRow(List<DogRankRow> rows, Long dogId) {
        for (DogRankRow row : rows) {
            if (dogId.equals(row.getDogId())) {
                return row;
            }
        }
        return null;
    }

    private Map<Long, Dog> loadDogs(List<DogRankRow> rows) {
        List<Long> ids = rows.stream().map(DogRankRow::getDogId).toList();
        return dogRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Dog::getId, Function.identity()));
    }

    private String resolveSeasonKey(String seasonParam) {
        if (seasonParam == null || seasonParam.isBlank()) {
            return seasonService.currentSeason().getKey();
        }
        return seasonRepository.findById(seasonParam)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND))
                .getKey();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST,
                    "page >= 0, 1 <= size <= " + MAX_SIZE);
        }
    }
}