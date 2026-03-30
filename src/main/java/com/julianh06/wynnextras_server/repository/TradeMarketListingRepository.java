package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.TradeMarketListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TradeMarketListingRepository extends JpaRepository<TradeMarketListing, Long> {
    List<TradeMarketListing> findBySubmittedAtAfter(Instant since);
    List<TradeMarketListing> findByItemNameIgnoreCaseAndSubmittedAtAfter(String itemName, Instant since);
    List<TradeMarketListing> findByRarityAndSubmittedAtAfter(String rarity, Instant since);
    List<TradeMarketListing> findByItemNameIgnoreCaseAndRarityAndSubmittedAtAfter(String itemName, String rarity, Instant since);
    void deleteBySubmittedAtBefore(Instant cutoff);
    long countBySubmittedAtAfter(Instant since);
}
