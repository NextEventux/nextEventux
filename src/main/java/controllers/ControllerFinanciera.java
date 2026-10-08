package main.java.controllers;

import java.util.Map;
import main.java.services.ServiceFinanciera;

public class ControllerFinanciera {
    
    private ServiceFinanciera serviceFinanciera;

    public ControllerFinanciera(ServiceFinanciera serviceFinanciera) {
        this.serviceFinanciera = serviceFinanciera;
    }

    public Map<String, Object> consultarEstadoFinanciero(int idEvento) {
        return serviceFinanciera.consultarEstadoFinanciero(idEvento);
    }
}