package com.fiscaladmin.mtca.cmbb.service;

/**
 * Seam to the Mayan EDMS binary store (P15). Tests use a simulator;
 * runtime uses {@link RestMayanClient} (REST v4, runtime token obtain).
 */
public interface MayanClient {

    /** Uploads a document; returns the Mayan document id. */
    String upload(String label, String fileName, byte[] content) throws Exception;
}
