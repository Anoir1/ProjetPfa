package com.optiagent.backend.repository;

import com.optiagent.backend.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByClientNameContainingIgnoreCase(String clientName);
    List<Invoice> findByAgentId(String agentId);
}
