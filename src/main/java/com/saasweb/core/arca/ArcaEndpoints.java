package com.saasweb.core.arca;

/**
 * URLs de los web services SOAP de ARCA (ex AFIP) — homologación (testing,
 * ver PLAN_SAAS.md Fase 14) vs producción. No hay API REST/JSON: son
 * servicios SOAP viejos, con autenticación propia (WSAA) separada del
 * servicio de negocio (WSFEv1).
 */
final class ArcaEndpoints {

    private ArcaEndpoints() {
    }

    static String wsaaUrl(boolean modoPrueba) {
        return modoPrueba
                ? "https://wsaahomo.afip.gov.ar/ws/services/LoginCms"
                : "https://wsaa.afip.gov.ar/ws/services/LoginCms";
    }

    static String wsfeUrl(boolean modoPrueba) {
        return modoPrueba
                ? "https://wswhomo.afip.gov.ar/wsfev1/service.asmx"
                : "https://servicios1.afip.gov.ar/wsfev1/service.asmx";
    }
}
