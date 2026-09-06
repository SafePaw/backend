package com.ne7k.safepaw.score.service;

import com.ne7k.safepaw.dog.domain.Dog;
import com.ne7k.safepaw.dog.domain.DogRank;
import com.ne7k.safepaw.score.domain.Season;
import com.ne7k.safepaw.score.domain.XpLedger;
import com.ne7k.safepaw.score.domain.XpSource;
import com.ne7k.safepaw.score.repository.XpLedgerRepository;
import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.walk.domain.WalkSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class XpService {

    // set4 고정 보상값 (추후 정책화)
    public static final int XP_WALK_COMPLETED = 50;
    public static final int XP_TERRITORY_CLAIMED = 100;
    public static final int XP_FIRST_CLAIM_BONUS = 200;

    private final XpLedgerRepository xpLedgerRepository;

    /**
     * 같은 트랜잭션에서 호출. dog.gainXp + 원장 기록 + 랭크 전이. 부여 내역 반환.
     * WALK_COMPLETED 는 3분 이상 + 산책 면적 50㎡ 이상일 때만.
     */
    public List<Grant> award(Dog dog, Season season, WalkSession walk, Territory territory,
                             boolean walkCompleted, boolean territoryClaimed, boolean firstClaim) {
        List<Grant> grants = new ArrayList<>();

        if (walkCompleted) {
            grant(dog, season, XpSource.WALK_COMPLETED, XP_WALK_COMPLETED, walk, null, grants);
        }
        if (territoryClaimed) {
            grant(dog, season, XpSource.TERRITORY_CLAIMED, XP_TERRITORY_CLAIMED, walk, territory, grants);
            if (firstClaim) {
                grant(dog, season, XpSource.FIRST_CLAIM_BONUS, XP_FIRST_CLAIM_BONUS, walk, territory, grants);
            }
        }
        // 랭크 전이 (DogRank.ofTotalXp 는 set3 domain)
        dog.promote(DogRank.ofTotalXp(dog.getTotalXp()));
        return grants;
    }

    private void grant(Dog dog, Season season, XpSource source, int amount,
                       WalkSession walk, Territory territory, List<Grant> out) {
        dog.gainXp(amount);
        xpLedgerRepository.save(XpLedger.of(dog, season, source, amount, walk, territory));
        out.add(new Grant(source, amount));
    }

    public record Grant(XpSource source, int amount) {}
}