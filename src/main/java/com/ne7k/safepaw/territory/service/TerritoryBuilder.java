package com.ne7k.safepaw.territory.service;

import com.ne7k.safepaw.territory.config.TerritoryProperties;
import com.ne7k.safepaw.territory.dto.response.GeoJsonGeometry;
import com.ne7k.safepaw.territory.repository.TerritoryRepository;
import com.ne7k.safepaw.walk.domain.Geometries;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerritoryBuilder {

    private final TerritoryRepository territoryRepository;
    private final TerritoryProperties props;
    private final WKTReader wktReader = new WKTReader(Geometries.FACTORY);

    /** set11: 순서 경로 → BuildArea MULTIPOLYGON WKT. 실패 시 null */
    public String buildWalkAreaWkt(Long walkId) {
        String wkt = territoryRepository.buildWalkAreaWkt(
                walkId,
                props.pathSimplifyToleranceDegrees(),
                props.polygonSimplifyToleranceDegrees(),
                props.minPolygonPartAreaSquareMeters());
        if (wkt == null || wkt.isBlank()) {
            return null;
        }
        String trimmed = wkt.trim();
        if (!trimmed.startsWith("MULTIPOLYGON") && !trimmed.startsWith("POLYGON")) {
            return null;
        }
        return trimmed;
    }

    /** WKT → MultiPolygon (POLYGON 단일도 Multi로 정규화) */
    public MultiPolygon parseMultiPolygon(String wkt) {
        try {
            Geometry g = wktReader.read(wkt);
            g.setSRID(Geometries.SRID);
            return toMultiPolygon(g);
        } catch (Exception e) {
            throw new IllegalStateException("MultiPolygon WKT 파싱 실패 : " + wkt, e);
        }
    }

    public static MultiPolygon toMultiPolygon(Geometry g) {
        if (g instanceof MultiPolygon mp) {
            return (MultiPolygon) g;
        }
        if (g instanceof Polygon p) {
            return Geometries.FACTORY.createMultiPolygon(new Polygon[]{p});
        }
        if (g.getNumGeometries() > 0) {
            Polygon[] polys = new Polygon[g.getNumGeometries()];
            int n = 0;
            for (int i = 0; i < g.getNumGeometries(); i++) {
                Geometry part = g.getGeometryN(i);
                if (part instanceof Polygon poly && !poly.isEmpty()) {
                    polys[n++] = poly;
                }
            }
            if (n == 0) {
                throw new IllegalArgumentException("Polygon part 없음 : " + g.getGeometryType());
            }
            Polygon[] trimmed = new Polygon[n];
            System.arraycopy(polys, 0, trimmed, 0, n);
            return Geometries.FACTORY.createMultiPolygon(trimmed);
        }
        throw new IllegalArgumentException("MultiPolygon 변환 불가 : " + g.getGeometryType());
    }

    public GeoJsonGeometry toGeoJson(Geometry geom) {
        if (geom == null || geom.isEmpty()) {
            return null;
        }
        return GeoJsonGeometry.from(geom);
    }
}