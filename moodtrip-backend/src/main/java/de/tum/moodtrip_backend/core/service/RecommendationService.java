package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Poi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {
    public List<Poi> recommendPois(List<Poi> pois) {
        // TODO: implement recommendation logic (e.g., ranking/filtering)
        return pois.subList(0,10);
    }
}
