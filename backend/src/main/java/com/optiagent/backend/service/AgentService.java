package com.optiagent.backend.service;

import com.optiagent.backend.model.Agent;
import com.optiagent.backend.model.Invoice;
import com.optiagent.backend.model.InvoiceFile;
import com.optiagent.backend.model.MissionOrder;
import com.optiagent.backend.model.dto.AgentRequest;
import com.optiagent.backend.model.dto.InvoiceRequest;
import com.optiagent.backend.model.dto.MissionOrderRequest;
import com.optiagent.backend.repository.AgentRepository;
import com.optiagent.backend.repository.InvoiceFileRepository;
import com.optiagent.backend.repository.InvoiceRepository;
import com.optiagent.backend.repository.MissionOrderRepository;
import com.optiagent.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceFileRepository invoiceFileRepository;
    private final MissionOrderRepository missionOrderRepository;
    private final UserRepository userRepository;

    public AgentService(AgentRepository agentRepository, 
                       InvoiceRepository invoiceRepository,
                       InvoiceFileRepository invoiceFileRepository,
                       MissionOrderRepository missionOrderRepository,
                       UserRepository userRepository) {
        this.agentRepository = agentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceFileRepository = invoiceFileRepository;
        this.missionOrderRepository = missionOrderRepository;
        this.userRepository = userRepository;
    }

    // Agent operations
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    public List<Agent> getAgentsByUserId(String userId) {
        return agentRepository.findByUserId(userId);
    }

    public Optional<Agent> getAgentById(String id) {
        return agentRepository.findById(id);
    }

    public Agent createAgent(AgentRequest agentRequest) {
        // Récupérer l'ID de l'utilisateur depuis la requête
        String userId = agentRequest.getUserId();

        // Créer un nouvel agent avec l'ID de l'utilisateur
        Agent agent = new Agent(agentRequest.getName(), agentRequest.getRole(), userId);

        // Mettre à jour les statistiques de l'utilisateur si l'ID est fourni
        if (userId != null && !userId.isEmpty()) {
            userRepository.findById(userId).ifPresent(user -> {
                // Incrémenter le nombre total d'agents de l'utilisateur
                user.incrementTotalAgents();
                userRepository.save(user);
            });
        }

        // Sauvegarder et retourner l'agent créé
        return agentRepository.save(agent);
    }

    public Optional<Agent> updateAgent(String id, AgentRequest agentRequest) {
        return agentRepository.findById(id)
                .map(agent -> {
                    agent.setName(agentRequest.getName());
                    agent.setRole(agentRequest.getRole());
                    return agentRepository.save(agent);
                });
    }

    public boolean deleteAgent(String id) {
        return agentRepository.findById(id).map(agent -> {
            // Mettre à jour les statistiques de l'utilisateur si l'agent a un propriétaire
            String userId = agent.getUserId();
            if (userId != null && !userId.isEmpty()) {
                userRepository.findById(userId).ifPresent(user -> {
                    user.decrementTotalAgents();
                    userRepository.save(user);
                });
            }

            agentRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    // Supprimer tous les agents d'un utilisateur
    public void deleteAgentsByUserId(String userId) {
        List<Agent> userAgents = agentRepository.findByUserId(userId);
        
        // Pour chaque agent, supprimer d'abord ses factures
        for (Agent agent : userAgents) {
            List<Invoice> invoices = invoiceRepository.findByAgentId(agent.getId());
            invoiceRepository.deleteAll(invoices);
        }
        
        // Puis supprimer tous les agents
        agentRepository.deleteAll(userAgents);
    }

    // Invoice operations
    public Optional<Agent> addInvoiceToAgent(String agentId, InvoiceRequest invoiceRequest) {
        return agentRepository.findById(agentId)
                .map(agent -> {
                    // Créer une nouvelle facture
                    Invoice invoice = new Invoice();
                    invoice.setInvoiceNumber(invoiceRequest.getInvoiceNumber());
                    invoice.setClientName(invoiceRequest.getClientName());
                    invoice.setAmount(invoiceRequest.getAmount());
                    invoice.setIssueDate(LocalDateTime.now());
                    invoice.setDueDate(invoiceRequest.getDueDate());
                    invoice.setStatus("PENDING");
                    invoice.setDescription(invoiceRequest.getDescription());
                    invoice.setAgentId(agentId); // Définir l'ID de l'agent pour la facture
                    
                    // Sauvegarder la facture dans la collection invoices
                    Invoice savedInvoice = invoiceRepository.save(invoice);
                    
                    // Créer un fichier de facture dans la collection invoiceFiles si des données de fichier sont fournies
                    if (invoiceRequest.getFileData() != null && invoiceRequest.getFileData().length > 0) {
                        InvoiceFile invoiceFile = new InvoiceFile(
                            invoiceRequest.getFileName() != null ? invoiceRequest.getFileName() : "invoice_" + savedInvoice.getId() + ".pdf",
                            invoiceRequest.getFileType() != null ? invoiceRequest.getFileType() : "application/pdf",
                            invoiceRequest.getFileData(),
                            agentId
                        );
                        
                        // Sauvegarder le fichier de facture
                        invoiceFileRepository.save(invoiceFile);
                    }
                    
                    // Ajouter la facture à l'agent
                    if (agent.getInvoices() == null) {
                        agent.setInvoices(new ArrayList<>());
                    }
                    agent.getInvoices().add(savedInvoice);
                    
                    // Sauvegarder l'agent mis à jour
                    return agentRepository.save(agent);
                });
    }

    public List<Invoice> getInvoicesByAgentId(String agentId) {
        return agentRepository.findById(agentId)
                .map(Agent::getInvoices)
                .orElse(List.of());
    }

    public Optional<Invoice> getInvoiceById(String invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    public boolean deleteInvoice(String agentId, String invoiceId) {
        return agentRepository.findById(agentId)
                .map(agent -> {
                    boolean removed = agent.getInvoices().removeIf(invoice -> invoice.getId().equals(invoiceId));
                    if (removed) {
                        agentRepository.save(agent);
                        invoiceRepository.deleteById(invoiceId);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    // Mission Order operations
    public Optional<Agent> addMissionOrderToAgent(String agentId, MissionOrderRequest missionOrderRequest) {
        return agentRepository.findById(agentId)
                .map(agent -> {
                    // Créer un ordre de mission dans la collection missionOrders
                    if (missionOrderRequest.getFileData() != null && missionOrderRequest.getFileData().length > 0) {
                        MissionOrder missionOrder = new MissionOrder(
                            missionOrderRequest.getFileName() != null ? missionOrderRequest.getFileName() : "mission_order_" + agent.getId() + ".pdf",
                            missionOrderRequest.getFileType() != null ? missionOrderRequest.getFileType() : "application/pdf",
                            missionOrderRequest.getFileData(),
                            agentId
                        );
                        
                        // Définir les autres propriétés de l'ordre de mission
                        missionOrder.setMissionName(missionOrderRequest.getMissionName());
                        missionOrder.setClientName(missionOrderRequest.getClientName());
                        missionOrder.setStartDate(missionOrderRequest.getStartDate());
                        missionOrder.setEndDate(missionOrderRequest.getEndDate());
                        missionOrder.setDescription(missionOrderRequest.getDescription());
                        
                        // Sauvegarder l'ordre de mission
                        MissionOrder savedMissionOrder = missionOrderRepository.save(missionOrder);
                        
                        // Associer l'ordre de mission à l'agent
                        agent.setMissionOrder(savedMissionOrder);
                    }
                    
                    // Sauvegarder l'agent mis à jour
                    return agentRepository.save(agent);
                });
    }

    public Optional<MissionOrder> getMissionOrderByAgentId(String agentId) {
        return missionOrderRepository.findByAgentId(agentId);
    }

    public boolean deleteMissionOrder(String agentId) {
        Optional<MissionOrder> missionOrderOpt = missionOrderRepository.findByAgentId(agentId);
        if (missionOrderOpt.isPresent()) {
            missionOrderRepository.delete(missionOrderOpt.get());
            return true;
        }
        return false;
    }
}
