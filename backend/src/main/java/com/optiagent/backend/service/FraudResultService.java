package com.optiagent.backend.service;

import com.optiagent.backend.model.FraudResult;
import com.optiagent.backend.repository.FraudResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FraudResultService {
    
    private final FraudResultRepository fraudResultRepository;

    public FraudResultService(FraudResultRepository fraudResultRepository) {
        this.fraudResultRepository = fraudResultRepository;
    }


    public FraudResult saveFraudResult(FraudResult fraudResult) {
        return fraudResultRepository.save(fraudResult);
    }

    public List<FraudResult> getFraudResultsByAgentId(String agentId) {
        return fraudResultRepository.findByAgentId(agentId);
    }


    public List<FraudResult> getFraudResultsByAgentIdOrderedByDate(String agentId) {
        return fraudResultRepository.findByAgentIdOrderByDateFactureDesc(agentId);
    }


    public List<FraudResult> getFraudulentResultsByAgentId(String agentId) {
        return fraudResultRepository.findByAgentIdAndFraude(agentId, "Oui");
    }


    public List<FraudResult> getFraudResultsByAgentIdAndStore(String agentId, String storeName) {
        return fraudResultRepository.findByAgentIdAndNomDuCommerce(agentId, storeName);
    }


    public Optional<FraudResult> getFraudResultById(String id) {
        return fraudResultRepository.findById(id);
    }


    public List<FraudResult> getAllFraudResults() {
        return fraudResultRepository.findAll();
    }
}
