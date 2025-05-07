// Service pour la gestion des exécutions

// Importer le service d'agent pour récupérer les informations de l'agent
import { agentService } from './agent-service';
// Importer le service localStorage
import localStorageService from './local-storage-service';

const API_URL = 'http://localhost:8081/api';
const USE_LOCAL_STORAGE = true; // Mettre à true pour utiliser le localStorage au lieu de l'API

// Fonction utilitaire pour obtenir les en-têtes d'authentification
const getAuthHeaders = () => {
  const headers = {
    'Content-Type': 'application/json'
  };
  
  const token = localStorage.getItem('token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return headers;
};

// Fonction utilitaire pour obtenir l'ID de l'utilisateur actuel
const getCurrentUserId = () => {
  try {
    const userStr = localStorage.getItem('opti_agent_user');
    if (userStr) {
      const user = JSON.parse(userStr);
      return user.id;
    }
  } catch (error) {
    console.error('Erreur lors de la récupération de l\'ID utilisateur:', error);
  }
  return null;
};

// Service d'exécution
const executionService = {
  // Récupérer toutes les exécutions
  getAllExecutions: async () => {
    if (USE_LOCAL_STORAGE) {
      return localStorageService.getExecutions();
    }
    
    try {
      const response = await fetch(`${API_URL}/executions`, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      if (!response.ok) {
        throw new Error('Erreur lors de la récupération des exécutions');
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erreur:', error);
      return [];
    }
  },
  
  // Récupérer les exécutions d'un utilisateur
  getExecutionsByUserId: async (userId = getCurrentUserId()) => {
    if (!userId) return [];
    
    if (USE_LOCAL_STORAGE) {
      return localStorageService.getExecutionsByUserId(userId);
    }
    
    try {
      const response = await fetch(`${API_URL}/executions/user/${userId}`, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      if (!response.ok) {
        throw new Error('Erreur lors de la récupération des exécutions de l\'utilisateur');
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erreur:', error);
      return [];
    }
  },
  
  // Récupérer les exécutions d'un agent
  getExecutionsByAgentId: async (agentId) => {
    if (!agentId) {
      console.error('ID d\'agent non fourni');
      return [];
    }
    
    if (USE_LOCAL_STORAGE) {
      return localStorageService.getExecutionsByAgentId(agentId);
    }
    
    try {
      const response = await fetch(`${API_URL}/executions/agent/${agentId}`, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      if (!response.ok) {
        console.warn(`Erreur HTTP: ${response.status} ${response.statusText}`);
        // Utiliser le localStorage comme solution de secours
        return localStorageService.getExecutionsByAgentId(agentId);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erreur lors de la récupération des exécutions de l\'agent:', error);
      // Utiliser le localStorage comme solution de secours
      return localStorageService.getExecutionsByAgentId(agentId);
    }
  },
  
  // Récupérer une exécution par son ID
  getExecutionById: async (id) => {
    if (USE_LOCAL_STORAGE) {
      return localStorageService.getExecutionById(id);
    }
    
    try {
      const response = await fetch(`${API_URL}/executions/${id}`, {
        method: 'GET',
        headers: getAuthHeaders()
      });
      
      if (!response.ok) {
        console.warn(`Erreur HTTP: ${response.status} ${response.statusText}`);
        // Utiliser le localStorage comme solution de secours
        return localStorageService.getExecutionById(id);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erreur lors de la récupération de l\'exécution:', error);
      // Utiliser le localStorage comme solution de secours
      return localStorageService.getExecutionById(id);
    }
  },
  
  // Démarrer une nouvelle exécution
  startExecution: async (agentId, data = {}) => {
    if (!agentId) {
      console.error('ID d\'agent non fourni');
      return null;
    }
    
    // Récupérer l'agent pour avoir ses informations
    const agent = await agentService.getAgentById(agentId);
    if (!agent) {
      console.error('Agent non trouvé');
      return null;
    }
    
    // Enregistrer la facture si elle est fournie dans les données
    if (data.invoice && data.invoice.fileData) {
      try {
        const invoiceData = {
          invoiceNumber: data.invoice.invoiceNumber || `INV-${Date.now()}`,
          clientName: data.invoice.clientName || agent.name,
          amount: data.invoice.amount || 0,
          dueDate: data.invoice.dueDate || new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
          description: data.invoice.description || `Facture pour ${agent.name}`,
          fileName: data.invoice.fileName || `facture_${Date.now()}.pdf`,
          fileType: data.invoice.fileType || 'application/pdf',
          fileData: data.invoice.fileData
        };
        
        await agentService.addInvoiceToAgent(agentId, invoiceData);
        console.log('Facture enregistrée avec succès');
      } catch (error) {
        console.error('Erreur lors de l\'enregistrement de la facture:', error);
      }
    }
    
    // Enregistrer l'ordre de mission s'il est fourni dans les données
    if (data.missionOrder && data.missionOrder.fileData) {
      try {
        const missionOrderData = {
          missionName: data.missionOrder.missionName || `Mission-${Date.now()}`,
          clientName: data.missionOrder.clientName || agent.name,
          startDate: data.missionOrder.startDate || new Date().toISOString(),
          endDate: data.missionOrder.endDate || new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
          description: data.missionOrder.description || `Ordre de mission pour ${agent.name}`,
          fileName: data.missionOrder.fileName || `ordre_mission_${Date.now()}.pdf`,
          fileType: data.missionOrder.fileType || 'application/pdf',
          fileData: data.missionOrder.fileData
        };
        
        await agentService.addMissionOrderToAgent(agentId, missionOrderData);
        console.log('Ordre de mission enregistré avec succès');
      } catch (error) {
        console.error('Erreur lors de l\'enregistrement de l\'ordre de mission:', error);
      }
    }
    
    // Créer une nouvelle exécution
    const execution = {
      id: Date.now().toString(), // Générer un ID unique
      agentId,
      userId: getCurrentUserId(), // Ajouter l'ID de l'utilisateur actuel
      status: 'RUNNING',
      startTime: new Date().toISOString(),
      endTime: null,
      results: {},
      ...data
    };
    
    if (USE_LOCAL_STORAGE) {
      return localStorageService.saveExecution(execution);
    }
    
    try {
      const response = await fetch(`${API_URL}/executions`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(execution)
      });
      
      if (!response.ok) {
        console.warn(`Erreur HTTP: ${response.status} ${response.statusText}`);
        // Utiliser le localStorage comme solution de secours
        return localStorageService.saveExecution(execution);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erreur lors du démarrage de l\'exécution:', error);
      // Utiliser le localStorage comme solution de secours
      return localStorageService.saveExecution(execution);
    }
  },
  
  // Mettre à jour le statut d'une exécution
  updateExecutionStatus: async (id, status, results = {}) => {
    if (!id) {
      console.error('ID d\'exécution non fourni');
      return null;
    }
    
    if (USE_LOCAL_STORAGE) {
      const execution = localStorageService.getExecutionById(id);
      if (!execution) return null;
      
      execution.status = status;
      execution.endTime = new Date().toISOString();
      execution.results = { ...execution.results, ...results };
      
      return localStorageService.saveExecution(execution);
    }
    
    try {
      const execution = await executionService.getExecutionById(id);
      if (!execution) return null;
      
      execution.status = status;
      execution.endTime = new Date().toISOString();
      execution.results = { ...execution.results, ...results };
      
      const response = await fetch(`${API_URL}/executions/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(execution)
      });
      
      if (!response.ok) {
        console.warn(`Erreur HTTP: ${response.status} ${response.statusText}`);
        // Utiliser le localStorage comme solution de secours
        return localStorageService.saveExecution(execution);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erreur lors de la mise à jour de l\'exécution:', error);
      
      // Essayer de mettre à jour en localStorage comme solution de secours
      try {
        const execution = localStorageService.getExecutionById(id);
        if (!execution) return null;
        
        execution.status = status;
        execution.endTime = new Date().toISOString();
        execution.results = { ...execution.results, ...results };
        
        return localStorageService.saveExecution(execution);
      } catch (e) {
        console.error('Erreur lors de la mise à jour en localStorage:', e);
        return null;
      }
    }
  }
};

export { executionService };
