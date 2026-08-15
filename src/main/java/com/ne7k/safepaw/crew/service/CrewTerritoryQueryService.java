package com.ne7k.safepaw.crew.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ne7k.safepaw.crew.config.CrewProperties;
import com.ne7k.safepaw.crew.domain.Crew;
import com.ne7k.safepaw.crew.dto.response.CrewStatsResponse;
import com.ne7k.safepaw.crew.dto.response.CrewTerritoryResponse;
import com.ne7k.safepaw.crew.dto.response.CrewTerritoryUnionResponse;
import com.ne7k.safepaw.crew.repository.CrewMemberRepository;
import com.ne7k.safepaw.crew.repository.CrewRepository;
import com.ne7k.safepaw.crew.repository.CrewStatsRepository;
import com.ne7k.safepaw.crew.repository.CrewStatsRepository.CrewUnionRow;
import com.ne7k.safepaw.dog.service.MarkerUrlResolver;
import com.ne7k.safepaw.global.exception.BusinessException;
import com.ne7k.safepaw.global.exception.ErrorCode;
import com.ne7k.safepaw.score.repository.SeasonRepository;
import com.ne7k.safepaw.score.service.SeasonService;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewTerritoryQueryService {

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewStatsRepository crewStatsRepository;
    private final TerritoryRepository territoryRepository;
    private final MarkerUrlResolver markerUrlResolver;
    private final CrewProperties crewProperties;
    private final SeasonService seasonService;
    private final SeasonRepository seasonRepository;
    private final ObjectMapper objectMapper;

    public List<CrewTerritoryResponse> findMineInBbox(
            Long userId, double swLng, double swLat, double neLng, double neLat) {
        var me = crewMemberRepository.findWithCrewByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_JOINED));
        return findInBbox(me.getCrew(), userId, swLng, swLat, neLng, neLat);
    }

    public List<CrewTerritoryResponse> findInBbox(
            Long viewerUserId, Long crewId,
            double swLng, double swLat, double neLng, double neLat) {
        Crew crew = crewRepository.findWithLeaderById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        return findInBbox(crew, viewerUserId, swLng, swLat, neLng, neLat);
    }

    public CrewStatsResponse stats(Long crewId, String seasonParam) {
        Crew crew = crewRepository.findWithLeaderById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        String season = resolveSeasonKey(seasonParam);
        CrewUnionRow row = crewStatsRepository.findCrewUnion(crewId, season).orElse(null);
        double area = row != null && row.getArea() != null ? row.getArea() : 0.0;
        long cnt = row != null && row.getCnt() != null ? row.getCnt() : 0L;
        int members = (int) crewMemberRepository.countByCrew_Id(crewId);
        return new CrewStatsResponse(
                season,
                crew.getId(),
                crew.getName(),
                crew.getTerritoryColor(),
                markerUrlResolver.resolve(crew.getImageKey()),
                members,
                crewProperties.maxMembers(),
                cnt,
                area,
                "m²"
        );
    }

    public CrewTerritoryUnionResponse union(Long crewId, String seasonParam) {
        Crew crew = crewRepository.findWithLeaderById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        String season = resolveSeasonKey(seasonParam);
        CrewUnionRow row = crewStatsRepository.findCrewUnion(crewId, season).orElse(null);
        double area = row != null && row.getArea() != null ? row.getArea() : 0.0;
        JsonNode geometry = parseGeoJson(row != null ? row.getGeojson() : null);
        return new CrewTerritoryUnionResponse(
                season, crew.getId(), crew.getTerritoryColor(), area, geometry);
    }

    private List<CrewTerritoryResponse> findInBbox(
            Crew crew, Long viewerUserId,
            double swLng, double swLat, double neLng, double neLat) {
        List<Territory> list = territoryRepository.findActiveInBboxByCrewId(
                crew.getId(), swLng, swLat, neLng, neLat);
        String crewImageUrl = markerUrlResolver.resolve(crew.getImageKey());
        return list.stream()
                .map(t -> CrewTerritoryResponse.from(
                        t,
                        viewerUserId,
                        markerUrlResolver.resolveFields(t.getDog().getMarkerImageKey()),
                        crew,
                        crewImageUrl))
                .toList();
    }

    private String resolveSeasonKey(String seasonParam) {
        if (seasonParam == null || seasonParam.isBlank()) {
            return seasonService.currentSeason().getKey();
        }
        return seasonRepository.findById(seasonParam)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEASON_NOT_FOUND))
                .getKey();
    }

    private JsonNode parseGeoJson(String geojson) {
        if (geojson == null || geojson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(geojson);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "크루 영토 GeoJSON 파싱 실패");
        }
    }
}
