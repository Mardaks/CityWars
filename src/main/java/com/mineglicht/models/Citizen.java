package com.mineglicht.models;

import java.util.UUID;

/**
 * Representa a un ciudadano dentro del sistema CityWars
 */
public class Citizen {
    private final UUID playerId;
    private UUID cityId;
    private long joinDate;
    private boolean isOfficer;
    private double taxPaid;
    private double totalTaxesPaid;
    private long lastTaxPayment;
    private int contributionPoints;

    /**
     * Constructor para crear un nuevo ciudadano
     *
     * @param playerId UUID del jugador
     * @param cityId UUID de la ciudad a la que pertenece
     */
    public Citizen(UUID playerId, UUID cityId) {
        this.playerId = playerId;
        this.cityId = cityId;
        this.joinDate = System.currentTimeMillis();
        this.isOfficer = false;
        this.taxPaid = 0;
        this.totalTaxesPaid = 0;
        this.lastTaxPayment = 0;
        this.contributionPoints = 0;
    }

    /**
     * Registra un pago de impuestos realizado por el ciudadano
     *
     * @param amount Cantidad de impuesto pagado
     */
    public void payTax(double amount) {
        this.taxPaid = amount;
        this.totalTaxesPaid += amount;
        this.lastTaxPayment = System.currentTimeMillis();
        this.contributionPoints += (int)(amount / 10); // Por cada 10 de impuesto, 1 punto de contribución
    }

    /**
     * Añade puntos de contribución (pueden otorgarse por participar en defensa, etc.)
     *
     * @param points Cantidad de puntos a añadir
     */
    public void addContributionPoints(int points) {
        this.contributionPoints += points;
    }

    /**
     * Verifica si el ciudadano ha pagado los impuestos en las últimas 24 horas
     *
     * @return true si ha pagado impuestos, false si no
     */
    public boolean hasPaidTaxesToday() {
        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000; // 24 horas en milisegundos

        return (currentTime - lastTaxPayment) < oneDayInMillis;
    }

    // Getters y setters

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getCityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public long getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(long joinDate) {
        this.joinDate = joinDate;
    }

    public boolean isOfficer() {
        return isOfficer;
    }

    public void setOfficer(boolean officer) {
        isOfficer = officer;
    }

    public double getTaxPaid() {
        return taxPaid;
    }

    public void setTaxPaid(double taxPaid) {
        this.taxPaid = taxPaid;
    }

    public double getTotalTaxesPaid() {
        return totalTaxesPaid;
    }

    public void setTotalTaxesPaid(double totalTaxesPaid) {
        this.totalTaxesPaid = totalTaxesPaid;
    }

    public long getLastTaxPayment() {
        return lastTaxPayment;
    }

    public void setLastTaxPayment(long lastTaxPayment) {
        this.lastTaxPayment = lastTaxPayment;
    }

    public int getContributionPoints() {
        return contributionPoints;
    }

    public void setContributionPoints(int contributionPoints) {
        this.contributionPoints = contributionPoints;
    }
}
