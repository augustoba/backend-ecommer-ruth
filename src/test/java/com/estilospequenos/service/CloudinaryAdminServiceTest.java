package com.estilospequenos.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** `extractPublicId` es la parte no trivial del borrado real en Cloudinary — se prueba aislada, sin red. */
class CloudinaryAdminServiceTest {

    @Test
    void extractsPublicIdFromUrlWithVersion() {
        String url = "https://res.cloudinary.com/jitutkbc/image/upload/v1699999999/estilos-pequenos/productos/abc123.jpg";
        assertEquals("estilos-pequenos/productos/abc123", CloudinaryAdminService.extractPublicId(url));
    }

    @Test
    void extractsPublicIdFromUrlWithoutVersion() {
        String url = "https://res.cloudinary.com/jitutkbc/image/upload/estilos-pequenos/logo/logo.png";
        assertEquals("estilos-pequenos/logo/logo", CloudinaryAdminService.extractPublicId(url));
    }

    @Test
    void returnsNullForNonCloudinaryUrl() {
        assertNull(CloudinaryAdminService.extractPublicId("https://otra-cosa.com/foto.jpg"));
        assertNull(CloudinaryAdminService.extractPublicId(null));
    }
}
